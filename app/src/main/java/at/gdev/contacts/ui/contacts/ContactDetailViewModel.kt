package at.gdev.contacts.ui.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import at.gdev.contacts.data.network.ValidationException
import at.gdev.contacts.domain.model.Contact
import at.gdev.contacts.domain.model.ContactAddress
import at.gdev.contacts.domain.model.ContactCall
import at.gdev.contacts.domain.model.ContactComment
import at.gdev.contacts.domain.model.ContactDate
import at.gdev.contacts.domain.model.ContactEmail
import at.gdev.contacts.domain.model.ContactGiftIdea
import at.gdev.contacts.domain.model.ContactNote
import at.gdev.contacts.domain.model.ContactNumber
import at.gdev.contacts.domain.model.ContactUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.domain.repository.ReferenceRepository
import at.gdev.contacts.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class OversizeImage(val bytes: ByteArray, val mimeType: String) {
    val sizeMb: Double get() = bytes.size / (1024.0 * 1024.0)

    override fun equals(other: Any?): Boolean =
        other is OversizeImage && other.bytes.contentEquals(bytes) && other.mimeType == mimeType

    override fun hashCode(): Int = bytes.contentHashCode() * 31 + mimeType.hashCode()
}

data class ContactDetailUiState(
    val loading: Boolean = true,
    val contact: Contact? = null,
    val comments: List<ContactComment> = emptyList(),
    val error: String? = null,
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val submitting: Boolean = false,
    val sheetError: String? = null,
    val countries: List<NamedRef> = emptyList(),
    val deleting: Boolean = false,
    val imageViewerOpen: Boolean = false,
    val imageSubmitting: Boolean = false,
    val imageError: String? = null,
    val oversizeImage: OversizeImage? = null,
)

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data class Number(val existing: ContactNumber?) : ActiveSheet
    data class Email(val existing: ContactEmail?) : ActiveSheet
    data class Url(val existing: ContactUrl?) : ActiveSheet
    data class Note(val existing: ContactNote?) : ActiveSheet
    data class DateItem(val existing: ContactDate?) : ActiveSheet
    data class Address(val existing: ContactAddress?) : ActiveSheet
    data class Call(val existing: ContactCall?) : ActiveSheet
    data class GiftIdeaItem(val existing: ContactGiftIdea?) : ActiveSheet
    data class Comment(
        val existing: ContactComment? = null,
        val replyToParentId: String? = null,
    ) : ActiveSheet
}

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ContactsRepository,
    private val referenceRepository: ReferenceRepository,
) : ViewModel() {

    private val contactId: String = checkNotNull(savedStateHandle[Routes.ARG_CONTACT_ID])

    private val _state = MutableStateFlow(ContactDetailUiState())
    val state: StateFlow<ContactDetailUiState> = _state.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events get() = _events.receiveAsFlow()

    sealed interface DetailEvent {
        data object Deleted : DetailEvent
    }

    init {
        load()
    }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.contact == null) }
            // Contact detail and comments live on separate endpoints; fetch them in parallel.
            val contactDeferred = async { repository.getContact(contactId) }
            val commentsDeferred = async {
                repository.listComments(contactId).getOrDefault(emptyList())
            }
            val contact = contactDeferred.await()
            val comments = commentsDeferred.await()
            _state.update {
                it.copy(
                    loading = false,
                    contact = contact,
                    comments = comments,
                    error = if (contact == null) "Contact not found" else null,
                )
            }
        }
    }

    // ----- Sheet open / close -----

    fun openAddNumber() = setSheet(ActiveSheet.Number(null))
    fun openEditNumber(item: ContactNumber) = setSheet(ActiveSheet.Number(item))
    fun openAddEmail() = setSheet(ActiveSheet.Email(null))
    fun openEditEmail(item: ContactEmail) = setSheet(ActiveSheet.Email(item))
    fun openAddUrl() = setSheet(ActiveSheet.Url(null))
    fun openEditUrl(item: ContactUrl) = setSheet(ActiveSheet.Url(item))
    fun openAddNote() = setSheet(ActiveSheet.Note(null))
    fun openEditNote(item: ContactNote) = setSheet(ActiveSheet.Note(item))
    fun openAddDate() = setSheet(ActiveSheet.DateItem(null))
    fun openEditDate(item: ContactDate) = setSheet(ActiveSheet.DateItem(item))
    fun openAddAddress() {
        ensureCountriesLoaded()
        setSheet(ActiveSheet.Address(null))
    }
    fun openEditAddress(item: ContactAddress) {
        ensureCountriesLoaded()
        setSheet(ActiveSheet.Address(item))
    }
    fun openAddCall() = setSheet(ActiveSheet.Call(null))
    fun openEditCall(item: ContactCall) = setSheet(ActiveSheet.Call(item))
    fun openAddGiftIdea() = setSheet(ActiveSheet.GiftIdeaItem(null))
    fun openEditGiftIdea(item: ContactGiftIdea) = setSheet(ActiveSheet.GiftIdeaItem(item))

    fun openAddComment() = setSheet(ActiveSheet.Comment(existing = null, replyToParentId = null))
    fun openReplyComment(parentId: String) = setSheet(ActiveSheet.Comment(existing = null, replyToParentId = parentId))
    fun openEditComment(item: ContactComment) = setSheet(ActiveSheet.Comment(existing = item))

    fun closeSheet() = setSheet(ActiveSheet.None)

    private fun setSheet(target: ActiveSheet) {
        _state.update { it.copy(activeSheet = target, sheetError = null) }
    }

    private fun ensureCountriesLoaded() {
        if (_state.value.countries.isNotEmpty()) return
        viewModelScope.launch {
            val loaded = runCatching { referenceRepository.countries() }.getOrDefault(emptyList())
            _state.update { it.copy(countries = loaded) }
        }
    }

    // ----- Image viewer + upload -----

    fun openImageViewer() = _state.update { it.copy(imageViewerOpen = true, imageError = null) }
    fun closeImageViewer() = _state.update { it.copy(imageViewerOpen = false, imageError = null) }

    fun uploadImage(bytes: ByteArray, mimeType: String) {
        if (_state.value.imageSubmitting) return
        if (bytes.size > MAX_IMAGE_BYTES) {
            _state.update { it.copy(oversizeImage = OversizeImage(bytes, mimeType), imageError = null) }
            return
        }
        doUpload(bytes, mimeType)
    }

    fun cancelOversize() = _state.update { it.copy(oversizeImage = null, imageError = null) }

    fun downsizeAndUpload() {
        val pending = _state.value.oversizeImage ?: return
        _state.update { it.copy(oversizeImage = null, imageSubmitting = true, imageError = null) }
        viewModelScope.launch {
            val downsized = withContext(Dispatchers.Default) { downsizeJpeg(pending.bytes, MAX_IMAGE_BYTES) }
            if (downsized == null) {
                _state.update {
                    it.copy(imageSubmitting = false, imageError = "Couldn't downsize this image. Pick another.")
                }
                return@launch
            }
            _state.update { it.copy(imageSubmitting = false) }
            doUpload(downsized, "image/jpeg")
        }
    }

    private fun doUpload(bytes: ByteArray, mimeType: String) {
        _state.update { it.copy(imageSubmitting = true, imageError = null) }
        viewModelScope.launch {
            val result = repository.uploadContactImage(contactId, bytes, mimeType)
            _state.update { it.copy(imageSubmitting = false) }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(imageViewerOpen = false) }
                    reload()
                },
                onFailure = { err ->
                    _state.update { it.copy(imageError = err.message ?: "Failed to upload image") }
                },
            )
        }
    }

    fun removeImage() {
        if (_state.value.imageSubmitting) return
        _state.update { it.copy(imageSubmitting = true, imageError = null) }
        viewModelScope.launch {
            val result = repository.removeContactImage(contactId)
            _state.update { it.copy(imageSubmitting = false) }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(imageViewerOpen = false) }
                    reload()
                },
                onFailure = { err ->
                    _state.update { it.copy(imageError = err.message ?: "Failed to remove image") }
                },
            )
        }
    }

    // ----- Contact-level actions -----

    fun deleteContact() {
        if (_state.value.deleting) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true) }
            val result = repository.deleteContact(contactId)
            _state.update { it.copy(deleting = false) }
            result.onSuccess { _events.send(DetailEvent.Deleted) }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message ?: "Failed to delete contact") }
                }
        }
    }

    // ----- Sub-resource mutations -----

    fun saveNumber(name: String, number: String) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Number ?: return
        submit {
            if (sheet.existing == null) repository.addNumber(contactId, name, number)
            else repository.updateNumber(contactId, sheet.existing.id, name, number)
        }
    }
    fun deleteNumber() = (_state.value.activeSheet as? ActiveSheet.Number)?.existing?.let { item ->
        submit { repository.deleteNumber(contactId, item.id) }
    }

    fun saveEmail(name: String, email: String) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Email ?: return
        submit {
            if (sheet.existing == null) repository.addEmail(contactId, name, email)
            else repository.updateEmail(contactId, sheet.existing.id, name, email)
        }
    }
    fun deleteEmail() = (_state.value.activeSheet as? ActiveSheet.Email)?.existing?.let { item ->
        submit { repository.deleteEmail(contactId, item.id) }
    }

    fun saveUrl(name: String, url: String) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Url ?: return
        submit {
            if (sheet.existing == null) repository.addUrl(contactId, name, url)
            else repository.updateUrl(contactId, sheet.existing.id, name, url)
        }
    }
    fun deleteUrl() = (_state.value.activeSheet as? ActiveSheet.Url)?.existing?.let { item ->
        submit { repository.deleteUrl(contactId, item.id) }
    }

    fun saveNote(name: String, text: String) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Note ?: return
        submit {
            if (sheet.existing == null) repository.addNote(contactId, name, text)
            else repository.updateNote(contactId, sheet.existing.id, name, text)
        }
    }
    fun deleteNote() = (_state.value.activeSheet as? ActiveSheet.Note)?.existing?.let { item ->
        submit { repository.deleteNote(contactId, item.id) }
    }

    fun saveDate(name: String, date: LocalDate, skipYear: Boolean) {
        val sheet = _state.value.activeSheet as? ActiveSheet.DateItem ?: return
        submit {
            if (sheet.existing == null) repository.addDate(contactId, name, date, skipYear)
            else repository.updateDate(contactId, sheet.existing.id, name, date, skipYear)
        }
    }
    fun deleteDate() = (_state.value.activeSheet as? ActiveSheet.DateItem)?.existing?.let { item ->
        submit { repository.deleteDate(contactId, item.id) }
    }

    fun saveAddress(
        name: String, street: String, zip: String, city: String, state: String?, countryId: Int,
    ) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Address ?: return
        submit {
            if (sheet.existing == null) {
                repository.addAddress(contactId, name, street, zip, city, state, countryId)
            } else {
                repository.updateAddress(contactId, sheet.existing.id, name, street, zip, city, state, countryId)
            }
        }
    }
    fun deleteAddress() = (_state.value.activeSheet as? ActiveSheet.Address)?.existing?.let { item ->
        submit { repository.deleteAddress(contactId, item.id) }
    }

    fun saveCall(calledAt: LocalDate, note: String?) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Call ?: return
        val isoDateTime = calledAt.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00"
        submit {
            if (sheet.existing == null) repository.addCall(contactId, isoDateTime, note)
            else repository.updateCall(contactId, sheet.existing.id, isoDateTime, note)
        }
    }
    fun deleteCall() = (_state.value.activeSheet as? ActiveSheet.Call)?.existing?.let { item ->
        submit { repository.deleteCall(contactId, item.id) }
    }

    fun saveGiftIdea(name: String, description: String?, url: String?, dueAt: LocalDate?) {
        val sheet = _state.value.activeSheet as? ActiveSheet.GiftIdeaItem ?: return
        submit {
            if (sheet.existing == null) repository.addGiftIdea(contactId, name, description, url, dueAt)
            else repository.updateGiftIdea(contactId, sheet.existing.id, name, description, url, dueAt)
        }
    }
    fun deleteGiftIdea() = (_state.value.activeSheet as? ActiveSheet.GiftIdeaItem)?.existing?.let { item ->
        submit { repository.deleteGiftIdea(contactId, item.id) }
    }

    fun saveComment(text: String) {
        val sheet = _state.value.activeSheet as? ActiveSheet.Comment ?: return
        submit {
            if (sheet.existing != null) {
                repository.updateComment(contactId, sheet.existing.id, text)
            } else {
                repository.addComment(contactId, text, sheet.replyToParentId)
            }
        }
    }
    fun deleteComment() = (_state.value.activeSheet as? ActiveSheet.Comment)?.existing?.let { item ->
        submit { repository.deleteComment(contactId, item.id) }
    }

    /** Common submission helper: shows spinner, surfaces errors, refetches detail on success. */
    private fun submit(block: suspend () -> Result<Unit>) {
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, sheetError = null) }
            val result = block()
            result.onSuccess {
                _state.update { it.copy(submitting = false, activeSheet = ActiveSheet.None, sheetError = null) }
                load()
            }.onFailure { err ->
                val message = (err as? ValidationException)?.firstError() ?: err.message ?: "Failed"
                _state.update { it.copy(submitting = false, sheetError = message) }
            }
        }
    }
}

private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
private const val DOWNSIZE_TARGET_MAX_DIMENSION = 1600

/**
 * Decode → optionally rotate per EXIF → JPEG re-encode, dropping quality stepwise until under [cap].
 * Returns null if the image can't be decoded or stays oversize even at minimum quality.
 */
private fun downsizeJpeg(bytes: ByteArray, cap: Int): ByteArray? = runCatching {
    val measure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, measure)
    if (measure.outWidth <= 0 || measure.outHeight <= 0) return@runCatching null

    var sample = 1
    val longest = maxOf(measure.outWidth, measure.outHeight)
    while (longest / (sample * 2) >= DOWNSIZE_TARGET_MAX_DIMENSION) sample *= 2

    val decoded = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return@runCatching null

    val rotation = runCatching {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)

    val oriented = if (rotation == 0f) decoded else {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it != decoded) decoded.recycle() }
    }

    var quality = 90
    val out = ByteArrayOutputStream()
    while (true) {
        out.reset()
        oriented.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (out.size() <= cap || quality <= 50) break
        quality -= 10
    }
    oriented.recycle()
    out.toByteArray().takeIf { it.size <= cap }
}.getOrNull()
