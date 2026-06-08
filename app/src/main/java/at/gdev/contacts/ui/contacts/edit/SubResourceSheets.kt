package at.gdev.contacts.ui.contacts.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.gdev.contacts.data.util.DateTimes
import at.gdev.contacts.domain.model.ContactAddress
import at.gdev.contacts.domain.model.ContactCall
import at.gdev.contacts.domain.model.ContactComment
import at.gdev.contacts.domain.model.ContactDate
import at.gdev.contacts.domain.model.ContactEmail
import at.gdev.contacts.domain.model.ContactGiftIdea
import at.gdev.contacts.domain.model.ContactNote
import at.gdev.contacts.domain.model.ContactNumber
import at.gdev.contacts.domain.model.ContactRelation
import at.gdev.contacts.domain.model.ContactSummary
import at.gdev.contacts.domain.model.ContactUrl
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.domain.model.RecordedCall
import at.gdev.contacts.ui.util.formatDateTime
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun NumberSheet(
    existing: ContactNumber?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var number by rememberSaveable(existing) { mutableStateOf(existing?.number.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Add phone number" else "Edit phone number",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && number.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), number.trim()) },
        onDelete = onDelete,
        deleteSubject = "phone number",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label (e.g. Mobile)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EmailSheet(
    existing: ContactEmail?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var email by rememberSaveable(existing) { mutableStateOf(existing?.email.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Add email" else "Edit email",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && email.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), email.trim()) },
        onDelete = onDelete,
        deleteSubject = "email",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label (e.g. Work)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun UrlSheet(
    existing: ContactUrl?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var url by rememberSaveable(existing) { mutableStateOf(existing?.url.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Add link" else "Edit link",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && url.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), url.trim()) },
        onDelete = onDelete,
        deleteSubject = "link",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun NoteSheet(
    existing: ContactNote?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, text: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var text by rememberSaveable(existing) { mutableStateOf(existing?.text.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Add note" else "Edit note",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && text.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), text.trim()) },
        onDelete = onDelete,
        deleteSubject = "note",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Note") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DateSheet(
    existing: ContactDate?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, date: LocalDate, skipYear: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var date by remember(existing) { mutableStateOf<LocalDate?>(existing?.date) }
    var skipYear by rememberSaveable(existing) { mutableStateOf(existing?.skipYear == true) }

    EditSheetScaffold(
        title = if (existing == null) "Add date" else "Edit date",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && date != null,
        onDismiss = onDismiss,
        onSave = { date?.let { onSave(name.trim(), it, skipYear) } },
        onDelete = onDelete,
        deleteSubject = "date",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("What (e.g. Birthday)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        DateField(label = "Date", value = date, onChange = { date = it }, required = true)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = skipYear, onCheckedChange = { skipYear = it })
            Text("Recurring (ignore year)", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddressSheet(
    existing: ContactAddress?,
    countries: List<NamedRef>,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, street: String, zip: String, city: String, state: String?, countryId: Int) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var street by rememberSaveable(existing) { mutableStateOf(existing?.street.orEmpty()) }
    var zip by rememberSaveable(existing) { mutableStateOf(existing?.zip.orEmpty()) }
    var city by rememberSaveable(existing) { mutableStateOf(existing?.city.orEmpty()) }
    var state by rememberSaveable(existing) { mutableStateOf(existing?.state.orEmpty()) }
    var countryId by remember(existing, countries) {
        mutableStateOf(countries.firstOrNull { it.name == existing?.country }?.id)
    }

    EditSheetScaffold(
        title = if (existing == null) "Add address" else "Edit address",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank() && street.isNotBlank() && zip.isNotBlank() && city.isNotBlank() && countryId != null,
        onDismiss = onDismiss,
        onSave = {
            countryId?.let { id ->
                onSave(
                    name.trim(),
                    street.trim(),
                    zip.trim(),
                    city.trim(),
                    state.trim().takeIf { it.isNotBlank() },
                    id,
                )
            }
        },
        onDelete = onDelete,
        deleteSubject = "address",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label (e.g. Home)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = street,
            onValueChange = { street = it },
            label = { Text("Street") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = zip,
                onValueChange = { zip = it },
                label = { Text("ZIP") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.weight(2f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state,
            onValueChange = { state = it },
            label = { Text("State / region (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        NamedRefDropdown(
            label = "Country",
            options = countries,
            selectedId = countryId,
            onSelect = { countryId = it?.id },
        )
    }
}

@Composable
fun CallSheet(
    existing: ContactCall?,
    recorded: RecordedCall? = null,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (calledAt: LocalDate, time: LocalTime, note: String?) -> Unit,
    onDelete: () -> Unit,
) {
    // called_at is a UTC datetime; we edit it as a date + time pair in local time.
    // A call picked from recent history (already local) or an existing entry
    // (UTC → local) pre-fills both fields.
    val existingLocal = DateTimes.instantToLocal(existing?.calledAt)
    val initialDate = recorded?.occurredAt?.toLocalDate() ?: existingLocal?.toLocalDate()
    val initialTime = recorded?.occurredAt?.toLocalTime() ?: existingLocal?.toLocalTime()
    var date by remember(existing, recorded) { mutableStateOf<LocalDate?>(initialDate ?: LocalDate.now()) }
    var time by remember(existing, recorded) { mutableStateOf(initialTime ?: LocalTime.now().withSecond(0).withNano(0)) }
    var note by rememberSaveable(existing, recorded) { mutableStateOf(existing?.note.orEmpty()) }

    EditSheetScaffold(
        title = when {
            recorded != null -> "Log call"
            existing == null -> "Log a call"
            else -> "Edit call"
        },
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = date != null,
        onDismiss = onDismiss,
        onSave = { date?.let { onSave(it, time, note.trim().takeIf { s -> s.isNotBlank() }) } },
        onDelete = onDelete,
        deleteSubject = "call entry",
    ) {
        DateField(label = "Date", value = date, onChange = { date = it }, required = true)
        Spacer(Modifier.height(12.dp))
        TimeField(label = "Time", value = time, onChange = { time = it })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            minLines = 2,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Lets the user pick one of the incoming calls observed by the screening service
 * and turn it into a call-log entry. Selecting a call hands off to [CallSheet]
 * (via [onPick]) with the date/time pre-filled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallPickerSheet(
    recordedCalls: List<RecordedCall>,
    onPick: (RecordedCall) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Recent calls", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick a call to log — the date and time are filled in for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(12.dp))
            recordedCalls.forEach { call ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(call) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(context.formatDateTime(call.occurredAt), style = MaterialTheme.typography.bodyLarge)
                    if (!call.matchedLabel.isNullOrBlank()) {
                        Text(
                            call.matchedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun RelationSheet(
    existing: ContactRelation?,
    currentContactName: String,
    candidates: List<ContactSummary>,
    candidatesLoading: Boolean,
    candidatesLoadingMore: Boolean,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (relatedContactId: String, forwardLabel: String, inverseLabel: String?) -> Unit,
    onDelete: () -> Unit,
) {
    val thisName = currentContactName.ifBlank { "This contact" }
    val listState = rememberLazyListState()
    // For a new relation the user first picks the other contact; for an existing
    // one the contact is fixed (only the labels are editable).
    var selectedId by rememberSaveable(existing) { mutableStateOf(existing?.relatedContactId) }
    var selectedName by rememberSaveable(existing) { mutableStateOf(existing?.relatedContactName.orEmpty()) }
    var query by rememberSaveable(existing) { mutableStateOf("") }
    var forward by rememberSaveable(existing) { mutableStateOf(existing?.label.orEmpty()) }
    var inverse by rememberSaveable(existing) { mutableStateOf(existing?.inverse.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Add relationship" else "Edit relationship",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = selectedId != null && forward.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { selectedId?.let { onSave(it, forward.trim(), inverse.trim().takeIf { s -> s.isNotBlank() }) } },
        onDelete = onDelete,
        deleteSubject = "relationship",
        // Fully expand so dragging the candidate list scrolls it instead of closing the sheet.
        skipPartiallyExpanded = true,
    ) {
        if (selectedId == null) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQueryChange(it) },
                label = { Text("Search contacts") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            when {
                candidatesLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                candidates.isEmpty() -> Text(
                    "No contacts found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                // Pages of 20 are fetched as you scroll toward the bottom.
                else -> {
                    LaunchedEffect(listState, candidates.size) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                            .collect { lastVisible ->
                                if (lastVisible >= candidates.size - 3) onLoadMore()
                            }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(candidates, key = { it.id }) { c ->
                            Text(
                                c.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedId = c.id; selectedName = c.displayName }
                                    .padding(vertical = 10.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            HorizontalDivider()
                        }
                        if (candidatesLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Related contact",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (existing == null) {
                    TextButton(onClick = { selectedId = null }) { Text("Change") }
                }
            }
            val otherName = selectedName.ifBlank { "they" }
            // forward_label = what the OTHER contact is to this one (what shows on
            // this page); inverse_label = the reverse (optional, mirrors if blank).
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = forward,
                onValueChange = { forward = it },
                label = { Text("$otherName is the…") },
                placeholder = { Text("e.g. Mother") },
                supportingText = { Text("…of $thisName  (this is what shows on this page)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = inverse,
                onValueChange = { inverse = it },
                label = { Text("$thisName is the… (optional)") },
                placeholder = { Text("e.g. Son") },
                supportingText = { Text("…of $otherName — leave blank if it's the same both ways") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun GiftIdeaSheet(
    existing: ContactGiftIdea?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, url: String?, dueAt: LocalDate?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var description by rememberSaveable(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var url by rememberSaveable(existing) { mutableStateOf(existing?.url.orEmpty()) }
    var dueAt by remember(existing) { mutableStateOf<LocalDate?>(existing?.dueAt) }

    EditSheetScaffold(
        title = if (existing == null) "Add gift idea" else "Edit gift idea",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                name.trim(),
                description.trim().takeIf { it.isNotBlank() },
                url.trim().takeIf { it.isNotBlank() },
                dueAt,
            )
        },
        onDelete = onDelete,
        deleteSubject = "gift idea",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Idea") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Notes (optional)") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Link (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        DateField(label = "Due date (optional)", value = dueAt, onChange = { dueAt = it })
    }
}

@Composable
fun CommentSheet(
    existing: ContactComment?,
    replyToParentId: String?,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (text: String) -> Unit,
    onDelete: () -> Unit,
) {
    var text by rememberSaveable(existing) { mutableStateOf(existing?.text.orEmpty()) }
    val title = when {
        existing != null -> "Edit comment"
        replyToParentId != null -> "Reply"
        else -> "Add comment"
    }
    EditSheetScaffold(
        title = title,
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = text.isNotBlank(),
        onDismiss = onDismiss,
        onSave = { onSave(text.trim()) },
        onDelete = onDelete,
        deleteSubject = "comment",
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Comment (markdown supported)") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
