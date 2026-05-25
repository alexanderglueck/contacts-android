package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.GenericErrorBody
import at.gdev.contacts.data.network.dto.ValidationErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Validation errors keyed by field, surfaced from Laravel-style 422 responses. */
class ValidationException(
    message: String,
    val errors: Map<String, List<String>>,
) : RuntimeException(message) {
    fun firstError(): String? = errors.values.firstOrNull()?.firstOrNull()
}

class UnauthenticatedException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)

fun Throwable.toDomainError(json: Json): Throwable {
    if (this !is HttpException) return this
    val raw = response()?.errorBody()?.string().orEmpty()
    return when (code()) {
        422 -> runCatching { json.decodeFromString<ValidationErrorBody>(raw) }
            .map { ValidationException(it.message, it.errors) }
            .getOrElse { ValidationException("Validation failed", emptyMap()) }

        401 -> UnauthenticatedException(parseMessage(raw, json) ?: "Unauthenticated")
        403 -> ForbiddenException(parseMessage(raw, json) ?: "Forbidden")
        404 -> NotFoundException(parseMessage(raw, json) ?: "Not found")
        else -> this
    }
}

private fun parseMessage(raw: String, json: Json): String? =
    runCatching { json.decodeFromString<GenericErrorBody>(raw).message }.getOrNull()
