package com.taskflow.audit.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class BiometricResult {
    object Success : BiometricResult()
    object UserCancelled : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}

object BiometricHelper {

    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        activity: FragmentActivity,
        title: String = "Verify Identity",
        subtitle: String = "Use your fingerprint or face to unlock TaskFlow Audit",
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                EncryptedPrefs.putLong(
                    EncryptedPrefs.KEY_LAST_AUTH_TIMESTAMP,
                    System.currentTimeMillis()
                )
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onResult(BiometricResult.UserCancelled)
                } else {
                    onResult(BiometricResult.Error(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                // Individual attempt failed — BiometricPrompt handles retries automatically
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN instead")
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    /**
     * Returns true if the last biometric auth was within the past 8 hours.
     * After 8 hours the user must enter their PIN again.
     */
    fun isSessionValid(): Boolean {
        val lastAuth = EncryptedPrefs.getLong(EncryptedPrefs.KEY_LAST_AUTH_TIMESTAMP)
        val eightHoursMs = 8 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastAuth) < eightHoursMs
    }
}
