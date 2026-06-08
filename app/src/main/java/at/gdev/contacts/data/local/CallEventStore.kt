package at.gdev.contacts.data.local

import at.gdev.contacts.domain.model.RecordedCall
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only store of incoming calls observed by the call screening service.
 * Entries are kept for [RETENTION_MILLIS] (a month) so the call-log picker
 * stays short, then pruned. Never synced to the backend.
 */
@Singleton
class CallEventStore @Inject constructor(
    private val dao: CallEventsDao,
) {

    /** Record an observed incoming call and opportunistically prune stale ones. */
    suspend fun recordIncoming(
        contactId: String,
        rawNumber: String,
        matchedLabel: String?,
        occurredAtMillis: Long,
    ) {
        dao.pruneOlderThan(occurredAtMillis - RETENTION_MILLIS)
        dao.insert(
            CallEventEntity(
                contactUlid = contactId,
                rawNumber = rawNumber,
                matchedLabel = matchedLabel,
                occurredAt = occurredAtMillis,
            ),
        )
    }

    /** Not-yet-logged calls for a contact, most recent first. */
    suspend fun recordedFor(contactId: String, nowMillis: Long): List<RecordedCall> {
        dao.pruneOlderThan(nowMillis - RETENTION_MILLIS)
        return dao.unloggedFor(contactId).map { it.toDomain() }
    }

    suspend fun markLogged(id: Long) = dao.markLogged(id)

    suspend fun clear() = dao.clear()

    private fun CallEventEntity.toDomain() = RecordedCall(
        id = id,
        occurredAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAt), ZoneId.systemDefault()),
        matchedLabel = matchedLabel,
    )

    private companion object {
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
