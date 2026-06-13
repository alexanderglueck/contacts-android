package at.gdev.contacts.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationPrefs by preferencesDataStore(name = "notification_prefs")

/** Tracks whether we've already shown the one-time notification-permission prompt. */
@Singleton
class NotificationPreferencesStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val permissionPromptShown: Flow<Boolean> =
        context.notificationPrefs.data.map { it[Keys.PERMISSION_PROMPT_SHOWN] ?: false }

    suspend fun setPermissionPromptShown(shown: Boolean) {
        context.notificationPrefs.edit { it[Keys.PERMISSION_PROMPT_SHOWN] = shown }
    }

    private object Keys {
        val PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("permission_prompt_shown")
    }
}
