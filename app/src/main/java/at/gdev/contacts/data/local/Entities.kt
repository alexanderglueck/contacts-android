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

/**
 * A locally-observed incoming call from a known contact, recorded by the call
 * screening service. Purely device-local (never synced) and pruned after a
 * month — it exists only to pre-fill the call-log "called at" timestamp so the
 * user need only add a note. No foreign key to [ContactEntity]: the matched
 * contact may have come from the API rather than the cached summaries.
 */
@Entity(
    tableName = "call_events",
    indices = [
        Index("contact_ulid"),
        Index("occurred_at"),
    ],
)
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "contact_ulid") val contactUlid: String,
    @ColumnInfo(name = "raw_number") val rawNumber: String,
    @ColumnInfo(name = "matched_label") val matchedLabel: String?,
    /** When the call came in, epoch millis. */
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    /** Set once the user has turned this event into a saved call-log entry. */
    val logged: Boolean = false,
)
