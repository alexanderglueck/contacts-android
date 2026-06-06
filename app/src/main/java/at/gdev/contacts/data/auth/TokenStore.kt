package at.gdev.contacts.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.gdev.contacts.domain.model.AuthSession
import at.gdev.contacts.domain.model.AuthUser
import at.gdev.contacts.domain.model.TeamSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }

    val session: Flow<AuthSession?> = context.dataStore.data.map { prefs -> prefs.toSession() }

    suspend fun save(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = session.accessToken
            prefs[Keys.ULID] = session.user.ulid
            prefs[Keys.NAME] = session.user.name
            prefs[Keys.EMAIL] = session.user.email
            val team = session.user.currentTeam
            if (team != null) {
                prefs[Keys.TEAM_UUID] = team.uuid
                prefs[Keys.TEAM_NAME] = team.name
            } else {
                prefs.remove(Keys.TEAM_UUID)
                prefs.remove(Keys.TEAM_NAME)
            }
        }
    }

    suspend fun updateCurrentTeam(team: TeamSummary?) {
        context.dataStore.edit { prefs ->
            if (team != null) {
                prefs[Keys.TEAM_UUID] = team.uuid
                prefs[Keys.TEAM_NAME] = team.name
            } else {
                prefs.remove(Keys.TEAM_UUID)
                prefs.remove(Keys.TEAM_NAME)
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    /** Latest FCM token known to the app (may differ from what the backend has). */
    suspend fun currentFcmToken(): String? =
        context.dataStore.data.first()[Keys.FCM_TOKEN]

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { it[Keys.FCM_TOKEN] = token }
    }

    /** The token last successfully registered with the backend; used to de-dupe registrations. */
    suspend fun registeredFcmToken(): String? =
        context.dataStore.data.first()[Keys.REGISTERED_FCM_TOKEN]

    suspend fun setRegisteredFcmToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token != null) prefs[Keys.REGISTERED_FCM_TOKEN] = token
            else prefs.remove(Keys.REGISTERED_FCM_TOKEN)
        }
    }

    /** ULID of the device row created by the backend; needed to de-register on logout. */
    suspend fun registeredDeviceId(): String? =
        context.dataStore.data.first()[Keys.REGISTERED_DEVICE_ID]

    suspend fun setRegisteredDeviceId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[Keys.REGISTERED_DEVICE_ID] = id
            else prefs.remove(Keys.REGISTERED_DEVICE_ID)
        }
    }

    private fun Preferences.toSession(): AuthSession? {
        val token = this[Keys.TOKEN] ?: return null
        val ulid = this[Keys.ULID] ?: return null
        val name = this[Keys.NAME] ?: return null
        val email = this[Keys.EMAIL] ?: return null
        val teamUuid = this[Keys.TEAM_UUID]
        val teamName = this[Keys.TEAM_NAME]
        val team = if (teamUuid != null && teamName != null) TeamSummary(teamUuid, teamName) else null
        return AuthSession(token, AuthUser(ulid, name, email, team))
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("access_token")
        val ULID = stringPreferencesKey("user_ulid")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val TEAM_UUID = stringPreferencesKey("team_uuid")
        val TEAM_NAME = stringPreferencesKey("team_name")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val REGISTERED_FCM_TOKEN = stringPreferencesKey("registered_fcm_token")
        val REGISTERED_DEVICE_ID = stringPreferencesKey("registered_device_id")
    }
}
