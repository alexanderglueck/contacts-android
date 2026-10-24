package at.gdev.contacts.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import at.gdev.contacts.data.util.DateTimes
import at.gdev.contacts.domain.model.ContactActivity
import at.gdev.contacts.domain.model.ContactActivityChange
import at.gdev.contacts.ui.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactHistoryScreen(
    onBack: () -> Unit,
    viewModel: ContactHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::pullToRefresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                HistoryList(state = state, onLoadMore = viewModel::loadMore)
            }
        }
    }
}

@Composable
private fun HistoryList(state: ContactHistoryUiState, onLoadMore: () -> Unit) {
    val listState = rememberLazyListState()
    // Ask for the next page once the last loaded entry scrolls into view.
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(nearEnd, state.hasMore, state.items.size) {
        if (nearEnd && state.hasMore) onLoadMore()
    }

    // LazyColumn even for the empty and error states, so pull to refresh works there too.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        state.error?.let { error ->
            item {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            }
        }
        if (state.items.isEmpty() && state.error == null) {
            item {
                Text(
                    "No history yet",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            }
        }
        items(state.items, key = { it.id }) { activity ->
            ActivityRow(activity)
            HorizontalDivider()
        }
        if (state.loadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: ContactActivity) {
    val context = LocalContext.current
    val event = activity.summary ?: when (activity.event) {
        "created" -> "Created"
        "updated" -> "Updated"
        "deleted" -> "Deleted"
        else -> activity.event.replaceFirstChar(Char::uppercaseChar)
    }
    val date = DateTimes.instantToLocal(activity.createdAt)?.let { context.formatDateTime(it) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(event) }
                activity.userName?.let { append(" by $it") }
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        date?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        activity.changes.forEach { ChangeLine(it) }
    }
}

@Composable
private fun ChangeLine(change: ContactActivityChange) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("${change.label}: ") }
            withStyle(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough)) {
                append(change.old ?: "—")
            }
            append("  →  ")
            append(change.new ?: "—")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}
