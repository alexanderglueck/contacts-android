package at.gdev.contacts.ui.contacts.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.gdev.contacts.domain.model.NamedRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamedRefDropdown(
    label: String,
    options: List<NamedRef>,
    selectedId: Int?,
    onSelect: (NamedRef?) -> Unit,
    modifier: Modifier = Modifier,
    nullable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text(if (options.isEmpty()) "Loading…" else "Select") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (nullable) {
                DropdownMenuItem(
                    text = { Text("— none —") },
                    onClick = { onSelect(null); expanded = false },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
