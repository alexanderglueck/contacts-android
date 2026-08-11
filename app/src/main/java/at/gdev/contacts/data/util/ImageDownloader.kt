package at.gdev.contacts.data.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves a remote image into the shared Pictures collection.
 *
 * Uses the app's authenticated OkHttp client, so an image URL behind the API's auth works the
 * same as a public one. No storage permission is involved: writing our own entry through
 * MediaStore has never needed one on the API levels this app supports.
 */
@Singleton
class ImageDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    suspend fun saveToPictures(url: String, displayName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                response.use {
                    if (!it.isSuccessful) error("Server returned ${it.code}")

                    val resolver = context.contentResolver
                    val pending = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/$ALBUM",
                        )
                        // Hides the entry from galleries until the bytes are actually there,
                        // so a failed download can't leave a broken thumbnail behind.
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, pending)
                        ?: error("Couldn't create an entry in Pictures")

                    runCatching {
                        val sink = resolver.openOutputStream(uri)
                            ?: error("Couldn't open Pictures for writing")
                        sink.use { out -> it.body.byteStream().use { source -> source.copyTo(out) } }
                    }.onFailure {
                        // Otherwise the placeholder lingers as an invisible empty row forever.
                        resolver.delete(uri, null, null)
                    }.getOrThrow()

                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                Unit
            }
        }

    private companion object {
        const val ALBUM = "Contacts"
    }
}
