package at.gdev.contacts.ui.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Which picker the user invoked most recently — drives "Choose another" after an oversize error. */
enum class PhotoSource { Camera, Gallery }

/** Creates an empty cache-dir file that the system camera can write a JPEG into. */
fun newCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    return File(dir, "avatar-${System.currentTimeMillis()}.jpg")
}

/**
 * Returns a content:// URI for [file] backed by the app's FileProvider authority.
 * The system camera receives this URI via EXTRA_OUTPUT and writes the captured image
 * directly into the underlying cache file.
 */
fun captureUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
