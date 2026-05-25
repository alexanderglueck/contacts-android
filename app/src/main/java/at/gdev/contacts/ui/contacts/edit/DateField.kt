package at.gdev.contacts.ui.contacts.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
) {
    var showPicker by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showPicker = true
        }
    }

    OutlinedTextField(
        value = value?.format(DISPLAY) ?: "",
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("Tap to select") },
        readOnly = true,
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val initialMillis = (value ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    onChange(millis?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) })
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                if (!required) {
                    TextButton(onClick = {
                        onChange(null)
                        showPicker = false
                    }) { Text("Clear") }
                } else {
                    TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private val DISPLAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
