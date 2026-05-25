package at.gdev.contacts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class NumberWithContact(
    val ulid: String,
    val name: String,
    val number: String,
    val digits: String,
    @androidx.room.ColumnInfo(name = "contact_ulid") val contactUlid: String,
    @androidx.room.ColumnInfo(name = "contact_full_name") val contactFullName: String,
    @androidx.room.ColumnInfo(name = "contact_image_url") val contactImageUrl: String?,
)

@Dao
interface ContactsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNumbers(numbers: List<ContactNumberEntity>)

    @Query("DELETE FROM contact_numbers WHERE contact_ulid = :contactUlid")
    suspend fun deleteNumbersFor(contactUlid: String)

    @Query("DELETE FROM contacts WHERE ulid NOT IN (:keepUlids)")
    suspend fun pruneContactsNotIn(keepUlids: List<String>)

    @Transaction
    suspend fun replaceContactWithNumbers(contact: ContactEntity, numbers: List<ContactNumberEntity>) {
        upsertContacts(listOf(contact))
        deleteNumbersFor(contact.ulid)
        if (numbers.isNotEmpty()) upsertNumbers(numbers)
    }

    @Query(
        """
        SELECT n.ulid AS ulid,
               n.name AS name,
               n.number AS number,
               n.digits AS digits,
               n.contact_ulid AS contact_ulid,
               c.full_name AS contact_full_name,
               c.image_url AS contact_image_url
        FROM contact_numbers n
        INNER JOIN contacts c ON c.ulid = n.contact_ulid
        WHERE n.digits = :digits OR n.digits LIKE '%' || :digits
        ORDER BY length(n.digits) DESC
        LIMIT 10
        """,
    )
    suspend fun lookupByDigitSuffix(digits: String): List<NumberWithContact>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun contactCount(): Int

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("DELETE FROM contact_numbers")
    suspend fun clearNumbers()

    @Transaction
    suspend fun clearAll() {
        clearNumbers()
        clearContacts()
    }
}
