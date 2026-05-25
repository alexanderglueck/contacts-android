@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package at.gdev.contacts.ui.contacts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import at.gdev.contacts.ui.common.ContactAvatar
import at.gdev.contacts.ui.common.PhotoSource
import at.gdev.contacts.ui.common.captureUri
import at.gdev.contacts.ui.common.initialsOf
import at.gdev.contacts.ui.common.newCaptureFile
import at.gdev.contacts.ui.contacts.edit.DateField
import at.gdev.contacts.ui.contacts.edit.NamedRefDropdown
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    onBack: () -> Unit,
    onSaved: (contactId: String) -> Unit,
    viewModel: EditContactViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.savedContactId) {
        state.savedContactId?.let(onSaved)
    }

    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val mime = resolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) viewModel.selectImage(bytes, mime)
        }
    }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var lastSource by remember { mutableStateOf<PhotoSource?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingFile
        if (ok && file != null && file.length() > 0) {
            val bytes = runCatching { file.readBytes() }.getOrNull()
            file.delete()
            pendingFile = null
            if (bytes != null) viewModel.selectImage(bytes, "image/jpeg")
        } else {
            file?.delete()
            pendingFile = null
        }
    }
    val launchGallery = {
        lastSource = PhotoSource.Gallery
        pickFromGallery.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val launchCamera = {
        lastSource = PhotoSource.Camera
        val file = newCaptureFile(context)
        pendingFile = file
        takePicture.launch(captureUri(context, file))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New contact" else "Edit contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isNew) {
                AvatarPicker(
                    pendingBytes = state.pendingImageBytes,
                    initials = initialsOf(state.firstName, state.lastName),
                    onTakePhoto = { launchCamera() },
                    onPickGallery = { launchGallery() },
                    onClear = viewModel::clearPendingImage,
                )
                state.imageError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                SectionDivider()
            }

            SectionHeader("Name")
            FieldRow {
                OutlinedTextField(
                    value = state.salutation,
                    onValueChange = viewModel::setSalutation,
                    label = { Text("Salutation *") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("salutation"),
                    supportingText = state.fieldErrors["salutation"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FieldRow {
                OutlinedTextField(
                    value = state.titleBefore,
                    onValueChange = viewModel::setTitleBefore,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.titleAfter,
                    onValueChange = viewModel::setTitleAfter,
                    label = { Text("Title (after)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            FieldRow {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::setFirstName,
                    label = { Text("First name *") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("firstname"),
                    supportingText = state.fieldErrors["firstname"]?.let { { Text(it) } },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::setLastName,
                    label = { Text("Last name *") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("lastname"),
                    supportingText = state.fieldErrors["lastname"]?.let { { Text(it) } },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::setNickname,
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()
            SectionHeader("Demographics")
            NamedRefDropdown(
                label = "Gender",
                options = state.genders,
                selectedId = state.genderId,
                onSelect = { viewModel.setGender(it?.id) },
                nullable = true,
            )
            NamedRefDropdown(
                label = "Nationality",
                options = state.nationalities,
                selectedId = state.nationalityId,
                onSelect = { viewModel.setNationality(it?.id) },
                nullable = true,
            )
            DateField(
                label = "Date of birth",
                value = state.dateOfBirth,
                onChange = viewModel::setDateOfBirth,
            )
            OutlinedTextField(
                value = state.firstMet,
                onValueChange = viewModel::setFirstMet,
                label = { Text("First met") },
                placeholder = { Text("e.g. at the OSM conference 2018") },
                minLines = 1,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                label = "Died",
                value = state.diedAt,
                onChange = viewModel::setDiedAt,
            )
            OutlinedTextField(
                value = state.diedFrom,
                onValueChange = viewModel::setDiedFrom,
                label = { Text("Cause of death") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()
            SectionHeader("Professional")
            OutlinedTextField(
                value = state.company,
                onValueChange = viewModel::setCompany,
                label = { Text("Company") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FieldRow {
                OutlinedTextField(
                    value = state.job,
                    onValueChange = viewModel::setJob,
                    label = { Text("Job") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.department,
                    onValueChange = viewModel::setDepartment,
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.customId,
                onValueChange = viewModel::setCustomId,
                label = { Text("Custom ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.iban,
                onValueChange = viewModel::setIban,
                label = { Text("IBAN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.vatin,
                onValueChange = viewModel::setVatin,
                label = { Text("VAT ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionDivider()
            SectionHeader("Groups")
            if (state.contactGroups.isEmpty()) {
                Text(
                    "No groups available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.contactGroups.forEach { group ->
                        FilterChip(
                            selected = group.id in state.selectedGroupIds,
                            onClick = { viewModel.toggleGroup(group.id) },
                            label = { Text(group.name) },
                        )
                    }
                }
            }

            SectionDivider()
            SectionHeader("Other")
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Active", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.active, onCheckedChange = viewModel::setActive)
            }

            if (state.error != null && state.fieldErrors.isEmpty()) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        }
    }

    state.oversizeImage?.let { oversize ->
        AlertDialog(
            onDismissRequest = viewModel::cancelOversize,
            title = { Text("Image too large") },
            text = {
                Text(
                    "This image is ${"%.1f".format(oversize.sizeMb)} MB; the server limit is 8 MB. " +
                        "Downsize it now, or pick another?"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::downsizeAndSelect) { Text("Downsize") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cancelOversize()
                    when (lastSource) {
                        PhotoSource.Camera -> launchCamera()
                        PhotoSource.Gallery -> launchGallery()
                        null -> launchGallery()
                    }
                }) { Text("Choose another") }
            },
        )
    }
}


@Composable
private fun AvatarPicker(
    pendingBytes: ByteArray?,
    initials: String,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pendingBytes != null) {
            AsyncImage(
                model = pendingBytes,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
            )
        } else {
            ContactAvatar(imageUrl = null, initials = initials, size = 96.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onTakePhoto, modifier = Modifier.weight(1f)) { Text("Take photo") }
            OutlinedButton(onClick = onPickGallery, modifier = Modifier.weight(1f)) { Text("Gallery") }
        }
        if (pendingBytes != null) {
            TextButton(onClick = onClear) { Text("Remove photo") }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun FieldRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}
