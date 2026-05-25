package at.gdev.contacts.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONTACTS_LIST = "contacts"
    const val CONTACT_DETAIL = "contacts/{contactId}"
    const val CONTACT_EDIT = "contacts/{contactId}/edit"
    const val SETTINGS = "settings"
    const val CALENDAR = "calendar"

    fun contactDetail(id: String): String = "contacts/$id"
    fun contactEdit(id: String): String = "contacts/$id/edit"

    const val ARG_CONTACT_ID = "contactId"
}
