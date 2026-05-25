package at.gdev.contacts.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val ulid: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    val company: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

@Entity(
    tableName = "contact_numbers",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["ulid"],
            childColumns = ["contact_ulid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("contact_ulid"),
        Index("digits"),
    ],
)
data class ContactNumberEntity(
    @PrimaryKey val ulid: String,
    @ColumnInfo(name = "contact_ulid") val contactUlid: String,
    val name: String,
    val number: String,
    /** All non-digit chars stripped, used for suffix-matching incoming numbers. */
    val digits: String,
)
