package com.taskflow.audit.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AES-256-GCM encrypted SharedPreferences backed by the Android Keystore.
 * Keys and values are both encrypted at rest. Used to store the Firebase
 * refresh token and biometric-unlock flag.
 */
object EncryptedPrefs {

    private const val PREFS_FILE = "taskflow_secure"

    // Keys
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_STAFF_SHORT_ID = "staff_short_id"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_LAST_AUTH_TIMESTAMP = "last_auth_ts"
    const val KEY_STAFF_DIRECTORY = "staff_directory"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    fun putString(key: String, value: String?) =
        prefs.edit().putString(key, value).apply()

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun putBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()

    fun getLong(key: String, default: Long = 0L): Long =
        prefs.getLong(key, default)

    fun putLong(key: String, value: Long) =
        prefs.edit().putLong(key, value).apply()

    fun clear() = prefs.edit().clear().apply()
}
