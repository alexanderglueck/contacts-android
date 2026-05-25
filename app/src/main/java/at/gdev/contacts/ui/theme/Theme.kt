package at.gdev.contacts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5AA8),
    onPrimary = Color.White,
    secondary = Color(0xFF5A6481),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF052978),
    secondary = Color(0xFFC2CCED),
    background = Color(0xFF1B1B1F),
    surface = Color(0xFF1B1B1F),
)

@Composable
fun ContactsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
