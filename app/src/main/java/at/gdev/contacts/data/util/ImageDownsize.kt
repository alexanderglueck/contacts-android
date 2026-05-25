package at.gdev.contacts.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TARGET_MAX_DIMENSION = 1600

/**
 * Decode → optionally rotate per EXIF → JPEG re-encode, dropping quality stepwise
 * (90 → 80 → 70 → 60) until under [cap]. Returns null if the image can't be decoded
 * or stays oversize even at minimum quality.
 *
 * Safe to call from any background dispatcher; allocates one or two Bitmaps which
 * are explicitly recycled before return.
 */
fun downsizeJpeg(bytes: ByteArray, cap: Int): ByteArray? = runCatching {
    val measure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, measure)
    if (measure.outWidth <= 0 || measure.outHeight <= 0) return@runCatching null

    var sample = 1
    val longest = maxOf(measure.outWidth, measure.outHeight)
    while (longest / (sample * 2) >= TARGET_MAX_DIMENSION) sample *= 2

    val decoded = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return@runCatching null

    val rotation = runCatching {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)

    val oriented = if (rotation == 0f) decoded else {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it != decoded) decoded.recycle() }
    }

    var quality = 90
    val out = ByteArrayOutputStream()
    while (true) {
        out.reset()
        oriented.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (out.size() <= cap || quality <= 50) break
        quality -= 10
    }
    oriented.recycle()
    out.toByteArray().takeIf { it.size <= cap }
}.getOrNull()
