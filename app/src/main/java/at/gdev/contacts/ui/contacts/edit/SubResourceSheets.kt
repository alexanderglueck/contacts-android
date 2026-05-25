package at.gdev.contacts.ui.contacts.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.gdev.contacts.domain.model.ContactAddress
import at.gdev.contacts.domain.model.ContactCall
import at.gdev.contacts.domain.model.ContactComment
import at.gdev.contacts.domain.model.ContactDate
import at.gdev.contacts.domain.model.ContactEmail
import at.gdev.contacts.domain.model.ContactGiftIdea
import at.gdev.contacts.domain.model.ContactNote
import at.gdev.contacts.domain.model.ContactNumber
import at.gdev.contacts.domain.model.ContactUrl
import at.gdev.contacts.domain.model.NamedRef
import java.time.LocalDate

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
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (calledAt: LocalDate, note: String?) -> Unit,
    onDelete: () -> Unit,
) {
    // We model called_at as a date-only picker for simplicity; the server accepts ISO datetimes.
    val initialDate = existing?.calledAt?.let {
        runCatching { LocalDate.parse(it.substringBefore('T').substringBefore(' ').take(10)) }.getOrNull()
    }
    var date by remember(existing) { mutableStateOf<LocalDate?>(initialDate ?: LocalDate.now()) }
    var note by rememberSaveable(existing) { mutableStateOf(existing?.note.orEmpty()) }

    EditSheetScaffold(
        title = if (existing == null) "Log a call" else "Edit call",
        isNew = existing == null,
        submitting = submitting,
        error = error,
        canSave = date != null,
        onDismiss = onDismiss,
        onSave = { date?.let { onSave(it, note.trim().takeIf { s -> s.isNotBlank() }) } },
        onDelete = onDelete,
        deleteSubject = "call entry",
    ) {
        DateField(label = "Called at", value = date, onChange = { date = it }, required = true)
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
