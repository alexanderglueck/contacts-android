package at.gdev.contacts.calls

import android.telecom.Call
import android.telecom.CallScreeningService
import at.gdev.contacts.data.local.CallEventStore
import at.gdev.contacts.domain.repository.ContactsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallerIdScreeningService : CallScreeningService() {

    @Inject lateinit var repository: ContactsRepository
    @Inject lateinit var notifier: CallerIdNotifier
    @Inject lateinit var callEventStore: CallEventStore

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        // ALWAYS respond first so we stay inside the screening budget (~5s).
        // We never want to silence or reject — just observe and annotate.
        respondToCall(callDetails, CallResponse.Builder().build())

        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return
        val handle = callDetails.handle ?: return
        if (handle.scheme != "tel") return
        val rawNumber = handle.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return

        // The call is happening right now, so wall-clock time is the call time.
        val occurredAt = System.currentTimeMillis()
        scope.launch {
            val matches = runCatching { repository.lookupByNumber(rawNumber) }.getOrDefault(emptyList())
            val match = matches.firstOrNull() ?: return@launch
            // Persist it so the contact's call-log picker can offer it later.
            runCatching {
                callEventStore.recordIncoming(match.contactId, rawNumber, match.matchedLabel, occurredAt)
            }
            notifier.showMatch(rawNumber, match)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
