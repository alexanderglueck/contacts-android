package at.gdev.contacts.calls

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Directory
import at.gdev.contacts.data.local.ContactsDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

/**
 * Exposes our synced contacts as a system contact directory. When an unknown
 * number rings, cooperative dialers (AOSP / Google Phone) query registered
 * directories — we answer with the matching contact name, and the system
 * caller-ID UI shows it natively (no notification path needed).
 *
 * Reads from the same Room cache the [CallerIdScreeningService] uses, so the
 * data is whatever the last [at.gdev.contacts.data.sync.ContactSyncWorker]
 * pull populated.
 */
class ContactsDirectoryProvider : ContentProvider() {

    private lateinit var dao: ContactsDao

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "directories", DIRECTORIES)
        addURI(AUTHORITY, "contacts/filter/*", CONTACT_FILTER)
        addURI(AUTHORITY, "contacts/lookup/*/entities", CONTACT_LOOKUP_ENTITIES)
        addURI(AUTHORITY, "contacts/lookup/*", CONTACT_LOOKUP)
        addURI(AUTHORITY, "data/phones/filter/*", PHONE_FILTER)
        addURI(AUTHORITY, "data/emails/filter/*", EMAIL_FILTER)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun contactsDao(): ContactsDao
    }

    override fun onCreate(): Boolean {
        val ctx: Context = context ?: return false
        val deps = EntryPointAccessors.fromApplication(ctx.applicationContext, Deps::class.java)
        dao = deps.contactsDao()
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = when (uriMatcher.match(uri)) {
        DIRECTORIES -> directoriesCursor(projection)
        PHONE_FILTER -> phoneFilterCursor(uri.lastPathSegment.orEmpty(), projection)
        CONTACT_FILTER -> contactFilterCursor(uri.lastPathSegment.orEmpty(), projection)
        CONTACT_LOOKUP -> contactLookupCursor(uri.pathSegments.getOrNull(2).orEmpty(), projection)
        CONTACT_LOOKUP_ENTITIES -> contactLookupCursor(uri.pathSegments.getOrNull(2).orEmpty(), projection)
        EMAIL_FILTER -> emptyCursor(projection ?: DEFAULT_EMAIL_FILTER_COLS)
        else -> null
    }

    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        DIRECTORIES -> Directory.CONTENT_TYPE
        PHONE_FILTER -> Phone.CONTENT_TYPE
        CONTACT_FILTER, CONTACT_LOOKUP, CONTACT_LOOKUP_ENTITIES -> Contacts.CONTENT_TYPE
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    // ---- Directory metadata ----

    private fun directoriesCursor(projection: Array<String>?): Cursor {
        val cols = projection ?: DEFAULT_DIRECTORY_COLS
        val cursor = MatrixCursor(cols)
        val row: Array<Any?> = cols.map { col ->
            when (col) {
                Directory.ACCOUNT_NAME -> ACCOUNT_NAME
                Directory.ACCOUNT_TYPE -> ACCOUNT_TYPE
                Directory.DISPLAY_NAME -> DISPLAY_NAME
                Directory.TYPE_RESOURCE_ID -> 0
                Directory.EXPORT_SUPPORT -> Directory.EXPORT_SUPPORT_NONE
                Directory.SHORTCUT_SUPPORT -> Directory.SHORTCUT_SUPPORT_NONE
                Directory.PHOTO_SUPPORT -> Directory.PHOTO_SUPPORT_NONE
                else -> null
            }
        }.toTypedArray()
        cursor.addRow(row)
        return cursor
    }

    // ---- The reason this provider exists: reverse-number lookup ----

    private fun phoneFilterCursor(number: String, projection: Array<String>?): Cursor {
        val cols = projection ?: DEFAULT_PHONE_FILTER_COLS
        val digits = number.filter { it.isDigit() }
        if (digits.length < SUFFIX_MIN) return MatrixCursor(cols)

        val suffix = digits.takeLast(SUFFIX_LEN)
        val matches = runBlocking { dao.lookupByDigitSuffix(suffix) }
        if (matches.isEmpty()) return MatrixCursor(cols)

        val cursor = MatrixCursor(cols)
        matches.forEachIndexed { index, match ->
            val rowId = stableId(match.ulid, index)
            val contactId = stableId(match.contactUlid, 0)
            val row: Array<Any?> = cols.map { col ->
                when (col) {
                    BaseColumns._ID -> rowId
                    Phone.CONTACT_ID -> contactId
                    Phone.LOOKUP_KEY -> match.contactUlid
                    Phone.DISPLAY_NAME, Phone.DISPLAY_NAME_PRIMARY -> match.contactFullName
                    Phone.DISPLAY_NAME_ALTERNATIVE -> match.contactFullName
                    Phone.PHOTO_ID -> 0
                    Phone.PHOTO_URI, Phone.PHOTO_THUMBNAIL_URI -> null
                    Phone.NUMBER -> match.number
                    Phone.NORMALIZED_NUMBER -> match.digits
                    Phone.TYPE -> Phone.TYPE_OTHER
                    Phone.LABEL -> match.name
                    else -> null
                }
            }.toTypedArray()
            cursor.addRow(row)
        }
        return cursor
    }

    // ---- Name search (optional but standard for directory providers) ----

    private fun contactFilterCursor(query: String, projection: Array<String>?): Cursor {
        // Not implemented: name search isn't required for caller-ID and our Room
        // table only carries names + numbers. Return empty so the system moves on.
        return MatrixCursor(projection ?: DEFAULT_CONTACT_COLS)
    }

    private fun contactLookupCursor(lookupKey: String, projection: Array<String>?): Cursor {
        // Lookup by key — empty for now; caller-ID display only needs phoneFilter.
        return MatrixCursor(projection ?: DEFAULT_CONTACT_COLS)
    }

    private fun emptyCursor(cols: Array<String>) = MatrixCursor(cols)

    private fun stableId(seed: String, salt: Int): Long {
        // Long out of the ulid hash, ORed with the salt so multiple rows from
        // one ulid don't collide.
        val base = seed.hashCode().toLong() and 0x7FFF_FFFFL
        return (base shl 16) or (salt.toLong() and 0xFFFF)
    }

    companion object {
        const val AUTHORITY = "at.gdev.contacts.directory"
        private const val ACCOUNT_NAME = "Contacts"
        private const val ACCOUNT_TYPE = "at.gdev.contacts"
        private const val DISPLAY_NAME = "Contacts"

        private const val DIRECTORIES = 1
        private const val CONTACT_FILTER = 2
        private const val CONTACT_LOOKUP = 3
        private const val CONTACT_LOOKUP_ENTITIES = 4
        private const val PHONE_FILTER = 5
        private const val EMAIL_FILTER = 6

        private const val SUFFIX_LEN = 9
        private const val SUFFIX_MIN = 7

        private val DEFAULT_DIRECTORY_COLS = arrayOf(
            Directory.ACCOUNT_NAME,
            Directory.ACCOUNT_TYPE,
            Directory.DISPLAY_NAME,
            Directory.TYPE_RESOURCE_ID,
            Directory.EXPORT_SUPPORT,
            Directory.SHORTCUT_SUPPORT,
            Directory.PHOTO_SUPPORT,
        )

        private val DEFAULT_PHONE_FILTER_COLS = arrayOf(
            BaseColumns._ID,
            Phone.CONTACT_ID,
            Phone.LOOKUP_KEY,
            Phone.DISPLAY_NAME,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.PHOTO_ID,
            Phone.PHOTO_URI,
            Phone.PHOTO_THUMBNAIL_URI,
            Phone.NUMBER,
            Phone.NORMALIZED_NUMBER,
            Phone.TYPE,
            Phone.LABEL,
        )

        private val DEFAULT_CONTACT_COLS = arrayOf(
            BaseColumns._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.PHOTO_ID,
            Contacts.PHOTO_URI,
            Contacts.PHOTO_THUMBNAIL_URI,
        )

        private val DEFAULT_EMAIL_FILTER_COLS = arrayOf(
            BaseColumns._ID,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
        )
    }
}
