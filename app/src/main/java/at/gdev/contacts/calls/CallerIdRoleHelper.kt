package at.gdev.contacts.calls

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallerIdRoleHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isRoleHeld(): Boolean = roleManager()?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    fun isRoleAvailable(): Boolean = roleManager()?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun createRequestIntent(): Intent? = roleManager()
        ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
        ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

    private fun roleManager(): RoleManager? = context.getSystemService(RoleManager::class.java)
}
