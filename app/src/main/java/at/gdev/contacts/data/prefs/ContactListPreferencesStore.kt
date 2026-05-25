package at.gdev.contacts.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.gdev.contacts.domain.model.ContactSort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.contactListPrefs by preferencesDataStore(name = "contact_list_prefs")

@Singleton
class ContactListPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val sort: Flow<ContactSort> = context.contactListPrefs.data.map { prefs ->
        when (prefs[Keys.SORT]) {
            "first_name" -> ContactSort.FirstName
            else -> ContactSort.LastName
        }
    }

    suspend fun setSort(sort: ContactSort) {
        context.contactListPrefs.edit { prefs ->
            prefs[Keys.SORT] = when (sort) {
                ContactSort.FirstName -> "first_name"
                ContactSort.LastName -> "last_name"
            }
        }
    }

    private object Keys {
        val SORT = stringPreferencesKey("sort")
    }
}
