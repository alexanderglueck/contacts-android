package at.gdev.contacts.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

/**
 * Circular avatar that prefers the remote image, falling back to initials on
 * missing URL, load failure, or while loading. Initials sit on a colored
 * background that matches the rest of the app's chrome.
 */
@Composable
fun ContactAvatar(
    imageUrl: String?,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val fallback: @Composable () -> Unit = {
            Text(
                text = initials,
                style = textStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (imageUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { fallback() },
                error = { fallback() },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

fun initialsOf(firstName: String, lastName: String, fullName: String = ""): String {
    val combined = (firstName.firstOrNull()?.uppercaseChar()?.toString().orEmpty() +
            lastName.firstOrNull()?.uppercaseChar()?.toString().orEmpty())
    return combined.ifBlank { fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
}
