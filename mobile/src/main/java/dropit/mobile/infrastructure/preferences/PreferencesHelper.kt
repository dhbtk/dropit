package dropit.mobile.infrastructure.preferences

import android.content.Context
import android.provider.Settings
import dropit.application.dto.TokenRequest
import java.util.*
import javax.inject.Inject

private const val PHONE_ID = "phoneId"
private const val PHONE_NAME = "phoneName"
private const val CURRENT_COMPUTER_ID = "currentComputerId"

class PreferencesHelper @Inject constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("dropit.mobile.Preferences", Context.MODE_PRIVATE)

    val phoneId
        get() = sharedPreferences.getString(PHONE_ID, "").let { UUID.fromString(it) }!!

    val phoneName
        get() = sharedPreferences.getString(PHONE_NAME, "")!!

    val tokenRequest
        get() = TokenRequest(phoneId, phoneName)

    var currentComputerId: UUID?
        get() = sharedPreferences.getString(CURRENT_COMPUTER_ID, null)?.let { UUID.fromString(it) }
        set(value) {
            sharedPreferences.edit().putString(CURRENT_COMPUTER_ID, value?.toString()).apply()
        }

    init {
        initDefaultValues(context)
    }

    private fun initDefaultValues(context: Context) {
        if (!sharedPreferences.contains(PHONE_ID)) {
            sharedPreferences
                .edit()
                .putString(PHONE_ID, UUID.randomUUID().toString())
                .apply()
        }
        if (!sharedPreferences.contains(PHONE_NAME)) {
            sharedPreferences
                .edit()
                .putString(PHONE_NAME, Settings.Secure.getString(context.contentResolver, "bluetooth_name"))
                .apply()
        }
    }
}
