package at.gdev.contacts.ui.calls

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Bottom-sheet onboarding for the caller-ID feature. Steps the user through
 * the two grants we need (notifications + the call-screening role) and shows
 * a green checkmark for each as it lands. Reads the live grant state on every
 * resume so the user can leave for the system settings screen and come back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallerIdSetupSheet(
    visible: Boolean,
    roleAvailable: Boolean,
    roleHeld: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    createRoleRequestIntent: () -> Intent?,
) {
    if (!visible) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var notificationsGranted by remember {
        mutableStateOf(checkNotificationsGranted(context))
    }

    // Re-check on resume so we pick up grants made via the system settings flow.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = checkNotificationsGranted(context)
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { onRefresh() }

    val complete = notificationsGranted && roleHeld

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            HeaderIcon()
            Spacer(Modifier.height(16.dp))
            Text(
                "See who's calling",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Show contact names from your address book on incoming calls — " +
                        "even for numbers you haven't saved on this phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            StepRow(
                index = 1,
                title = "Notification access",
                description = "Lets the app show a heads-up with the caller's name.",
                done = notificationsGranted,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationsGranted = true
                    }
                },
                actionLabel = "Grant",
            )
            Spacer(Modifier.height(12.dp))
            StepRow(
                index = 2,
                title = "Call screening",
                description = if (roleAvailable) {
                    "Lets Android invoke the lookup before the call rings."
                } else {
                    "Not supported on this device."
                },
                done = roleHeld,
                onAction = { createRoleRequestIntent()?.let(roleLauncher::launch) },
                actionLabel = "Set",
                enabled = roleAvailable,
            )

            Spacer(Modifier.height(24.dp))
            if (complete) {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now")
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PhoneInTalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    title: String,
    description: String,
    done: Boolean,
    onAction: () -> Unit,
    actionLabel: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepBadge(index = index, done = done)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (!done) {
            TextButton(onClick = onAction, enabled = enabled) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StepBadge(index: Int, done: Boolean) {
    val bg = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = bg, shape = CircleShape, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = fg)
            } else {
                Text(index.toString(), color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun checkNotificationsGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
