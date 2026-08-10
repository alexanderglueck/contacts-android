package at.gdev.contacts.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TARGET_MAX_DIMENSION = 1600
private const val MIN_QUALITY = 50

/** Extra decode attempts at half resolution each, used only to recover from OOM. */
private const val DECODE_ATTEMPTS = 3

/**
 * Decode → optionally rotate per EXIF → JPEG re-encode, dropping quality stepwise
 * (90 → 80 → 70 → 60 → 50) until under [cap]. Returns null if the image can't be
 * decoded or stays oversize even at minimum quality.
 *
 * Safe to call from any background dispatcher; the bitmaps it allocates are
 * recycled before return, including on the failure paths.
 */
fun downsizeJpeg(bytes: ByteArray, cap: Int): ByteArray? {
    val measure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, measure) }
    if (measure.outWidth <= 0 || measure.outHeight <= 0) return null

    var sample = 1
    val longest = maxOf(measure.outWidth, measure.outHeight)
    while (longest / (sample * 2) >= TARGET_MAX_DIMENSION) sample *= 2

    // A capture big enough to exhaust the heap is exactly the one that most needs
    // downsizing, so halve the decode and try again rather than reporting failure.
    // Everything else is terminal: a smaller decode won't fix a corrupt image, and
    // it can't rescue an encode that is somehow still over cap at MIN_QUALITY.
    repeat(DECODE_ATTEMPTS) { attempt ->
        try {
            return encodeUnderCap(bytes, sample shl attempt, cap)
        } catch (_: OutOfMemoryError) {
            // Fall through to a coarser sample size.
        } catch (_: Exception) {
            return null
        }
    }
    return null
}

/**
 * One decode/encode attempt at [sample]. Returns null when the image can't be
 * decoded, the encoder fails, or the result won't fit under [cap]. Propagates
 * [OutOfMemoryError] so the caller can retry smaller.
 */
private fun encodeUnderCap(bytes: ByteArray, sample: Int, cap: Int): ByteArray? {
    val decoded = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return null

    var working = decoded
    try {
        val rotation = exifRotation(bytes)
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap
                .createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) {
                working = rotated
                decoded.recycle()
            }
        }

        // An Ultra HDR capture decodes with its gain map attached and the JPEG
        // encoder writes it straight back out, which is dead weight on a path
        // whose whole job is getting under a byte cap -- and the server re-encodes
        // to a 400x400 SDR JPEG regardless.
        if (working.hasGainmap()) working.gainmap = null

        val out = ByteArrayOutputStream()
        var quality = 90
        while (true) {
            out.reset()
            // A failed compress leaves the stream empty; without this the empty
            // buffer sails through the cap check and gets uploaded as a 0-byte file.
            if (!working.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
            if (out.size() <= cap || quality <= MIN_QUALITY) break
            quality -= 10
        }
        return out.toByteArray().takeIf { it.size in 1..cap }
    } finally {
        working.recycle()
    }
}

private fun exifRotation(bytes: ByteArray): Float = runCatching {
    val exif = ExifInterface(ByteArrayInputStream(bytes))
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
}.getOrDefault(0f)
