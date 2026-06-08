@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package at.gdev.contacts.ui.contacts

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import at.gdev.contacts.ui.common.PhotoSource
import at.gdev.contacts.ui.common.captureUri
import at.gdev.contacts.ui.common.newCaptureFile
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Context
import android.content.Intent
import at.gdev.contacts.ui.common.ContactAvatar
import at.gdev.contacts.ui.common.MarkdownText
import at.gdev.contacts.ui.common.initialsOf
import at.gdev.contacts.domain.model.Contact
import at.gdev.contacts.domain.model.ContactAddress
import at.gdev.contacts.domain.model.ContactCall
import at.gdev.contacts.data.util.DateTimes
import at.gdev.contacts.domain.model.ContactComment
import at.gdev.contacts.domain.model.ContactRelation
import at.gdev.contacts.domain.model.RecordedCall
import at.gdev.contacts.domain.model.ContactDate
import at.gdev.contacts.domain.model.ContactEmail
import at.gdev.contacts.domain.model.ContactGiftIdea
import at.gdev.contacts.domain.model.ContactNote
import at.gdev.contacts.domain.model.ContactNumber
import at.gdev.contacts.domain.model.ContactUrl
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.ui.contacts.edit.AddressSheet
import at.gdev.contacts.ui.contacts.edit.CallPickerSheet
import at.gdev.contacts.ui.contacts.edit.CallSheet
import at.gdev.contacts.ui.contacts.edit.CommentSheet
import at.gdev.contacts.ui.contacts.edit.DateSheet
import at.gdev.contacts.ui.contacts.edit.RelationSheet
import at.gdev.contacts.ui.contacts.edit.EmailSheet
import at.gdev.contacts.ui.contacts.edit.GiftIdeaSheet
import at.gdev.contacts.ui.contacts.edit.NoteSheet
import at.gdev.contacts.ui.contacts.edit.NumberSheet
import at.gdev.contacts.ui.contacts.edit.UrlSheet
import at.gdev.contacts.ui.util.formatDate
import at.gdev.contacts.ui.util.formatDateTime
import at.gdev.contacts.ui.util.formatMonthDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onEditBase: (contactId: String) -> Unit,
    onContactClick: (contactId: String) -> Unit = {},
    viewModel: ContactDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is ContactDetailViewModel.DetailEvent.Deleted) onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.contact?.displayName ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.contact != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit contact") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    state.contact?.let { onEditBase(it.id) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete contact") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
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

            state.contact == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text(state.error ?: "Contact not found") }

            else -> ContactDetailContent(
                contact = state.contact!!,
                comments = state.comments,
                recordedCalls = state.recordedCalls,
                onContactClick = onContactClick,
                viewModel = viewModel,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete contact?") },
            text = { Text("This permanently removes ${state.contact?.displayName ?: "this contact"} and all attached data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteContact()
                    },
                    enabled = !state.deleting,
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Sheets(state = state, viewModel = viewModel)

    if (state.imageViewerOpen && state.contact != null) {
        ImageViewerDialog(
            contact = state.contact!!,
            submitting = state.imageSubmitting,
            error = state.imageError,
            oversize = state.oversizeImage,
            onDismiss = viewModel::closeImageViewer,
            onTakePhoto = viewModel::uploadImage,
            onRemove = viewModel::removeImage,
            onDownsize = viewModel::downsizeAndUpload,
            onCancelOversize = viewModel::cancelOversize,
        )
    }
}


@Composable
private fun ImageViewerDialog(
    contact: Contact,
    submitting: Boolean,
    error: String?,
    oversize: OversizeImage?,
    onDismiss: () -> Unit,
    onTakePhoto: (ByteArray, String) -> Unit,
    onRemove: () -> Unit,
    onDownsize: () -> Unit,
    onCancelOversize: () -> Unit,
) {
    val context = LocalContext.current
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var lastSource by remember { mutableStateOf<PhotoSource?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingFile
        if (ok && file != null && file.length() > 0) {
            val bytes = runCatching { file.readBytes() }.getOrNull()
            file.delete()
            pendingFile = null
            if (bytes != null) onTakePhoto(bytes, "image/jpeg")
        } else {
            file?.delete()
            pendingFile = null
        }
    }
    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val mime = resolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) onTakePhoto(bytes, mime)
        }
    }

    fun launchCamera() {
        lastSource = PhotoSource.Camera
        val file = newCaptureFile(context)
        pendingFile = file
        takePicture.launch(captureUri(context, file))
    }

    fun launchGallery() {
        lastSource = PhotoSource.Gallery
        pickFromGallery.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    BackHandler(onBack = onDismiss)
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                    if (!contact.imageUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = { confirmRemove = true },
                            enabled = !submitting,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (contact.imageUrl.isNullOrBlank()) {
                        ContactAvatar(
                            imageUrl = null,
                            initials = initialsOf(contact.firstName, contact.lastName, contact.fullName),
                            size = 240.dp,
                            textStyle = MaterialTheme.typography.displayMedium,
                        )
                    } else {
                        AsyncImage(
                            model = contact.imageUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Button(
                        onClick = { launchCamera() },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Take photo")
                        }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { launchGallery() },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose from gallery") }
                }
            }
        }

    if (confirmRemove) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove photo?") },
            text = { Text("Replaces the contact's avatar with their initials. The image can't be recovered.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemove()
                    },
                    enabled = !submitting,
                ) { Text("Remove") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmRemove = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (oversize != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCancelOversize,
            title = { Text("Image too large") },
            text = {
                Text(
                    "This image is ${"%.1f".format(oversize.sizeMb)} MB; the server limit is 8 MB. " +
                        "Downsize it now, or pick another?"
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onDownsize) { Text("Downsize") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onCancelOversize()
                    when (lastSource) {
                        PhotoSource.Camera -> launchCamera()
                        PhotoSource.Gallery -> launchGallery()
                        null -> Unit
                    }
                }) { Text("Choose another") }
            },
        )
    }
}


@Composable
private fun ContactDetailContent(
    contact: Contact,
    comments: List<ContactComment>,
    recordedCalls: List<RecordedCall>,
    onContactClick: (String) -> Unit,
    viewModel: ContactDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Header(contact, onAvatarClick = viewModel::openImageViewer)

        if (hasAbout(contact)) {
            Section(title = "About", onAdd = null) { About(contact) }
        }

        if (hasIdentifiers(contact)) {
            Section(title = "Identifiers", onAdd = null) { Identifiers(contact) }
        }

        Section(title = "Phone numbers", onAdd = viewModel::openAddNumber) {
            EmptyOr(contact.numbers) {
                contact.numbers.forEach { item ->
                    LabeledLine(
                        label = item.name,
                        value = item.number,
                        onClick = { viewModel.openEditNumber(item) },
                        trailing = { ActionIcon(Icons.Filled.Call, "Call ${item.number}") { dial(context, item.number) } },
                    )
                }
            }
        }

        Section(title = "Email addresses", onAdd = viewModel::openAddEmail) {
            EmptyOr(contact.emails) {
                contact.emails.forEach { item ->
                    LabeledLine(
                        label = item.name,
                        value = item.email,
                        onClick = { viewModel.openEditEmail(item) },
                        trailing = { ActionIcon(Icons.Filled.Email, "Email ${item.email}") { sendMail(context, item.email) } },
                    )
                }
            }
        }

        Section(title = "Links", onAdd = viewModel::openAddUrl) {
            EmptyOr(contact.urls) {
                contact.urls.forEach { item ->
                    LabeledLine(
                        label = item.name,
                        value = item.url,
                        onClick = { viewModel.openEditUrl(item) },
                        trailing = { ActionIcon(Icons.AutoMirrored.Filled.OpenInNew, "Open ${item.url}") { openUrl(context, item.url) } },
                    )
                }
            }
        }

        Section(title = "Addresses", onAdd = viewModel::openAddAddress) {
            EmptyOr(contact.addresses) {
                contact.addresses.forEach { item ->
                    AddressRow(
                        item = item,
                        onClick = { viewModel.openEditAddress(item) },
                        onOpenMap = { openMap(context, item) },
                    )
                }
            }
        }

        Section(title = "Important dates", onAdd = viewModel::openAddDate) {
            EmptyOr(contact.dates) {
                contact.dates.forEach { item ->
                    DateRow(item) { viewModel.openEditDate(item) }
                }
            }
        }

        Section(title = "Notes", onAdd = viewModel::openAddNote) {
            EmptyOr(contact.notes) {
                contact.notes.forEach { item ->
                    NoteRow(item) { viewModel.openEditNote(item) }
                }
            }
        }

        Section(title = "Call history", onAdd = viewModel::openAddCall) {
            if (recordedCalls.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::openCallPicker,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("From recent calls (${recordedCalls.size})")
                }
            }
            EmptyOr(contact.calls) {
                contact.calls.forEach { item ->
                    CallRow(item) { viewModel.openEditCall(item) }
                }
            }
        }

        Section(title = "Relationships", onAdd = viewModel::openAddRelation) {
            EmptyOr(contact.relations) {
                contact.relations.forEach { item ->
                    RelationRow(
                        item = item,
                        onOpen = { onContactClick(item.relatedContactId) },
                        onEdit = { viewModel.openEditRelation(item) },
                    )
                }
            }
        }

        Section(title = "Gift ideas", onAdd = viewModel::openAddGiftIdea) {
            EmptyOr(contact.giftIdeas) {
                contact.giftIdeas.forEach { item ->
                    GiftRow(item) { viewModel.openEditGiftIdea(item) }
                }
            }
        }

        Section(title = "Comments", onAdd = viewModel::openAddComment) {
            CommentsBlock(
                comments = comments,
                onEdit = viewModel::openEditComment,
                onReply = viewModel::openReplyComment,
            )
        }
    }
}

@Composable
private fun Header(contact: Contact, onAvatarClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ContactAvatar(
            imageUrl = contact.imageUrl,
            initials = initialsOf(contact.firstName, contact.lastName, contact.fullName),
            size = 96.dp,
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )
    }
    if (!contact.active) {
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AssistChip(onClick = {}, enabled = false, label = { Text("Inactive") })
        }
    }
    Spacer(Modifier.height(8.dp))
    contact.nickname?.takeIf { it.isNotBlank() }?.let { LabeledLine("Nickname", it) }
    contact.salutation?.takeIf { it.isNotBlank() }?.let { LabeledLine("Salutation", it) }
    contact.company?.takeIf { it.isNotBlank() }?.let { LabeledLine("Company", it) }
    contact.job?.takeIf { it.isNotBlank() }?.let { LabeledLine("Job", it) }
    contact.department?.takeIf { it.isNotBlank() }?.let { LabeledLine("Department", it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun About(contact: Contact) {
    val context = LocalContext.current
    contact.note?.takeIf { it.isNotBlank() }?.let {
        MarkdownText(it)
        Spacer(Modifier.height(8.dp))
    }
    contact.dateOfBirth?.let {
        // Year 1900 is the sentinel for "birth year unknown" — show day/month only.
        val formatted = if (it.year == UNKNOWN_BIRTH_YEAR) context.formatMonthDay(it) else context.formatDate(it)
        LabeledLine("Date of birth", formatted)
    }
    contact.firstMet?.takeIf { it.isNotBlank() }?.let {
        Spacer(Modifier.height(4.dp))
        Text("First met", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        MarkdownText(it)
        Spacer(Modifier.height(4.dp))
    }
    contact.diedAt?.let {
        val cause = contact.diedFrom?.takeIf { c -> c.isNotBlank() }?.let { c -> " ($c)" }.orEmpty()
        LabeledLine("Died", context.formatDate(it) + cause)
    }
    contact.gender?.let { LabeledLine("Gender", it.name.replaceFirstChar(Char::uppercaseChar)) }
    contact.nationality?.let { LabeledLine("Nationality", it.name) }
    if (contact.contactGroups.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Groups",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            contact.contactGroups.forEach { group ->
                SuggestionChip(onClick = {}, label = { Text(group.name) })
            }
        }
    }
}

@Composable
private fun Identifiers(contact: Contact) {
    var any = false
    contact.customId?.takeIf { it.isNotBlank() }?.let { LabeledLine("Custom ID", it); any = true }
    contact.iban?.takeIf { it.isNotBlank() }?.let { LabeledLine("IBAN", it); any = true }
    contact.vatin?.takeIf { it.isNotBlank() }?.let { LabeledLine("VAT ID", it); any = true }
    if (!any) EmptyRow()
}

@Composable
private fun Section(
    title: String,
    onAdd: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        if (onAdd != null) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add to $title")
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    content()
}

@Composable
private fun EmptyOr(items: List<Any>, content: @Composable () -> Unit) {
    if (items.isEmpty()) EmptyRow() else content()
}

@Composable
private fun EmptyRow() {
    Text(
        "—",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun LabeledLine(
    label: String?,
    value: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(vertical = 6.dp)
    Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.bodyMedium)
            label?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun AddressRow(item: ContactAddress, onClick: () -> Unit, onOpenMap: () -> Unit) {
    val lines = listOf(
        item.street.trim(),
        listOf(item.zip, item.city).filter { it.isNotBlank() }.joinToString(" "),
        listOf(item.state, item.country).filter { it.isNotBlank() }.joinToString(", "),
    ).filter { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (item.name.isNotBlank()) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        ActionIcon(Icons.Filled.Map, "Open in maps", onOpenMap)
    }
}

@Composable
private fun DateRow(item: ContactDate, onClick: () -> Unit) {
    val context = LocalContext.current
    val formatted = item.date?.let {
        if (item.skipYear) context.formatMonthDay(it) else context.formatDate(it)
    } ?: "—"
    LabeledLine(label = item.name, value = formatted, onClick = onClick)
}

@Composable
private fun NoteRow(item: ContactNote, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        if (item.name.isNotBlank()) {
            Text(item.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        MarkdownText(item.text)
    }
}

@Composable
private fun CallRow(item: ContactCall, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        val when_ = context.formatDateTime(DateTimes.instantToLocal(item.calledAt))
        Text(when_, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        if (!item.note.isNullOrBlank()) {
            MarkdownText(item.note)
        }
    }
}

@Composable
private fun RelationRow(item: ContactRelation, onOpen: () -> Unit, onEdit: () -> Unit) {
    // `label` is what the related contact is to the viewed one (e.g. "Mother"),
    // so it labels the related person's name. Tap the row to jump to that contact;
    // the edit icon adjusts the labels.
    LabeledLine(
        label = item.label,
        value = item.relatedContactName,
        onClick = onOpen,
        trailing = { ActionIcon(Icons.Filled.Edit, "Edit relationship", onEdit) },
    )
}

@Composable
private fun GiftRow(item: ContactGiftIdea, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        item.description?.takeIf { it.isNotBlank() }?.let {
            MarkdownText(it)
        }
        item.url?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        item.dueAt?.let {
            Text(
                "Due ${context.formatDate(it)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun Sheets(state: ContactDetailUiState, viewModel: ContactDetailViewModel) {
    when (val sheet = state.activeSheet) {
        ActiveSheet.None -> Unit

        is ActiveSheet.Number -> NumberSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveNumber,
            onDelete = viewModel::deleteNumber,
        )

        is ActiveSheet.Email -> EmailSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveEmail,
            onDelete = viewModel::deleteEmail,
        )

        is ActiveSheet.Url -> UrlSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveUrl,
            onDelete = viewModel::deleteUrl,
        )

        is ActiveSheet.Note -> NoteSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveNote,
            onDelete = viewModel::deleteNote,
        )

        is ActiveSheet.DateItem -> DateSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveDate,
            onDelete = viewModel::deleteDate,
        )

        is ActiveSheet.Address -> AddressSheet(
            existing = sheet.existing,
            countries = state.countries,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveAddress,
            onDelete = viewModel::deleteAddress,
        )

        is ActiveSheet.Call -> CallSheet(
            existing = sheet.existing,
            recorded = sheet.recorded,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveCall,
            onDelete = viewModel::deleteCall,
        )

        ActiveSheet.CallPicker -> CallPickerSheet(
            recordedCalls = state.recordedCalls,
            onPick = viewModel::pickRecordedCall,
            onDismiss = viewModel::closeSheet,
        )

        is ActiveSheet.GiftIdeaItem -> GiftIdeaSheet(
            existing = sheet.existing,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveGiftIdea,
            onDelete = viewModel::deleteGiftIdea,
        )

        is ActiveSheet.Relation -> RelationSheet(
            existing = sheet.existing,
            currentContactName = state.contact?.displayName.orEmpty(),
            candidates = state.relationCandidates,
            candidatesLoading = state.relationCandidatesLoading,
            candidatesLoadingMore = state.relationCandidatesLoadingMore,
            onQueryChange = viewModel::searchRelationCandidates,
            onLoadMore = viewModel::loadMoreRelationCandidates,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveRelation,
            onDelete = viewModel::deleteRelation,
        )

        is ActiveSheet.Comment -> CommentSheet(
            existing = sheet.existing,
            replyToParentId = sheet.replyToParentId,
            submitting = state.submitting,
            error = state.sheetError,
            onDismiss = viewModel::closeSheet,
            onSave = viewModel::saveComment,
            onDelete = viewModel::deleteComment,
        )
    }
}

@Composable
private fun CommentsBlock(
    comments: List<ContactComment>,
    onEdit: (ContactComment) -> Unit,
    onReply: (parentId: String) -> Unit,
) {
    if (comments.isEmpty()) {
        EmptyRow()
        return
    }
    // Stable thread order: top-level chronologically, then each thread's replies chronologically.
    val sorted = comments.sortedBy { it.createdAt }
    val topLevel = sorted.filter { it.parentId == null }
    val repliesByParent = sorted.filter { it.parentId != null }.groupBy { it.parentId!! }

    topLevel.forEach { parent ->
        CommentRow(
            comment = parent,
            indent = false,
            onEdit = { onEdit(parent) },
            onReply = { onReply(parent.id) },
        )
        repliesByParent[parent.id].orEmpty().forEach { reply ->
            CommentRow(
                comment = reply,
                indent = true,
                onEdit = { onEdit(reply) },
                onReply = { onReply(parent.id) },
            )
        }
    }

    // Orphan replies whose parent isn't in the list (e.g. parent tombstoned + cleaned up): show flat.
    val parentIds = topLevel.map { it.id }.toSet()
    val orphans = repliesByParent.filterKeys { it !in parentIds }.values.flatten()
    orphans.forEach { item ->
        CommentRow(
            comment = item,
            indent = false,
            onEdit = { onEdit(item) },
            onReply = { onReply(item.id) },
        )
    }
}

@Composable
private fun CommentRow(
    comment: ContactComment,
    indent: Boolean,
    onEdit: () -> Unit,
    onReply: () -> Unit,
) {
    val context = LocalContext.current
    val startPadding = if (indent) 24.dp else 0.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .clickable(enabled = !comment.tombstoned, onClick = onEdit)
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val whoWhen = listOfNotNull(
                    comment.owner?.name,
                    DateTimes.instantToLocal(comment.createdAt)?.let { context.formatDateTime(it) },
                ).joinToString(" · ")
                Text(
                    whoWhen,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!comment.tombstoned) {
                ActionIcon(Icons.AutoMirrored.Filled.Reply, "Reply", onReply)
            }
        }
        if (comment.tombstoned) {
            Text(
                "[deleted]",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            MarkdownText(comment.text)
        }
    }
}

private fun hasAbout(contact: Contact): Boolean =
    !contact.note.isNullOrBlank() ||
            contact.dateOfBirth != null ||
            !contact.firstMet.isNullOrBlank() ||
            contact.diedAt != null ||
            contact.gender != null ||
            contact.nationality != null ||
            contact.contactGroups.isNotEmpty()

private fun hasIdentifiers(contact: Contact): Boolean =
    !contact.customId.isNullOrBlank() ||
            !contact.iban.isNullOrBlank() ||
            !contact.vatin.isNullOrBlank()

/** Sentinel birth year meaning "exact year unknown" — the date of birth shows day/month only. */
private const val UNKNOWN_BIRTH_YEAR = 1900

private fun dial(context: Context, number: String) {
    fire(context, Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
}

private fun sendMail(context: Context, email: String) {
    fire(context, Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri()))
}

private fun openUrl(context: Context, url: String) {
    val normalized = if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) url
    else "https://$url"
    fire(context, Intent(Intent.ACTION_VIEW, normalized.toUri()))
}

private fun openMap(context: Context, address: ContactAddress) {
    val query = listOfNotNull(
        address.street.trim().takeIf { it.isNotBlank() },
        listOf(address.zip, address.city).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
        address.state.takeIf { it.isNotBlank() },
        address.country.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    val uri = if (query.isBlank()) "geo:0,0".toUri()
    else "geo:0,0?q=${android.net.Uri.encode(query)}".toUri()
    fire(context, Intent(Intent.ACTION_VIEW, uri))
}

private fun fire(context: Context, intent: Intent) {
    runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
