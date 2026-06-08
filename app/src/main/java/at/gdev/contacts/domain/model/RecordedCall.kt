package at.gdev.contacts.domain.model

import java.time.LocalDateTime

/**
 * A locally-observed incoming call from a contact that hasn't yet been turned
 * into a saved call-log entry. Surfaced in the "From recent calls" picker.
 */
data class RecordedCall(
    val id: Long,
    val occurredAt: LocalDateTime,
    val matchedLabel: String?,
)
