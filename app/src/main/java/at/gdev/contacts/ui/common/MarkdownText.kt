package at.gdev.contacts.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/** Thin wrapper around [Markdown] so call sites stay tidy and we have one place to tune styling. */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Markdown(content = text, modifier = modifier)
}
