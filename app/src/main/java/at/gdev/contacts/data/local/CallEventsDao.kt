package at.gdev.contacts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CallEventsDao {

    @Insert
    suspend fun insert(event: CallEventEntity): Long

    /** Not-yet-logged calls for a contact, most recent first. */
    @Query(
        """
        SELECT * FROM call_events
        WHERE contact_ulid = :contactUlid AND logged = 0
        ORDER BY occurred_at DESC
        """,
    )
    suspend fun unloggedFor(contactUlid: String): List<CallEventEntity>

    @Query("UPDATE call_events SET logged = 1 WHERE id = :id")
    suspend fun markLogged(id: Long)

    /** Drops events older than [cutoff] (epoch millis) so the table stays small. */
    @Query("DELETE FROM call_events WHERE occurred_at < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)

    @Query("DELETE FROM call_events")
    suspend fun clear()
}
