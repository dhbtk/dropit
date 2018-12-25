package dropit.mobile.infrastructure.preferences

import android.content.Context
import android.provider.Settings
import java.util.*

private const val PHONE_ID = "phoneId"
private const val PHONE_NAME = "phoneName"
private const val CURRENT_COMPUTER_ID = "currentComputerId"

class PreferencesHelper(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("dropit.mobile.Preferences", Context.MODE_PRIVATE)

    val phoneId: String
        get() = sharedPreferences.getString(PHONE_ID, "")

    val phoneName: String
        get() = sharedPreferences.getString(PHONE_NAME, "")

    var currentComputerId: String?
        get() = sharedPreferences.getString(CURRENT_COMPUTER_ID, null)
        set(value) {
            sharedPreferences.edit().putString(CURRENT_COMPUTER_ID, value).apply()
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