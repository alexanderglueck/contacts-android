package at.gdev.contacts.domain.model

/**
 * Builds the client-side display string: `title_before firstname lastname title_after (nickname)`.
 * The server still ships `fullname` for compatibility, but it bundles the company name we render
 * separately below. [fallback] (typically the server-composed fullname) is returned only if every
 * structured part is blank.
 */
fun composeDisplayName(
    titleBefore: String?,
    firstName: String,
    lastName: String,
    titleAfter: String?,
    nickname: String?,
    fallback: String = "",
): String {
    val core = listOfNotNull(
        titleBefore?.trim()?.takeIf { it.isNotEmpty() },
        firstName.trim().takeIf { it.isNotEmpty() },
        lastName.trim().takeIf { it.isNotEmpty() },
        titleAfter?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" ")
    val nick = nickname?.trim()?.takeIf { it.isNotEmpty() }
    val composed = when {
        nick == null -> core
        core.isEmpty() -> "($nick)"
        else -> "$core ($nick)"
    }
    return composed.ifEmpty { fallback }
}
