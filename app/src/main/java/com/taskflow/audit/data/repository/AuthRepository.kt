package com.taskflow.audit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.taskflow.audit.security.EncryptedPrefs
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val isAdmin: Boolean) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Handles Firebase Authentication.
 *
 * Staff are pre-provisioned with email: {shortId}@taskflow.audit (dummy domain)
 * and their PIN as password. Firebase hashes/salts passwords server-side — the
 * app never sees the plaintext PIN after the initial sign-in call.
 *
 * Token lifecycle:
 *   - Firebase ID tokens expire after 1 hour and are auto-refreshed by the SDK
 *   - The SDK persists tokens in its own encrypted store
 *   - EncryptedPrefs stores only the staffId and biometric flag
 */
class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * Signs in with email constructed from shortId and the entered PIN.
     * Returns the Firebase user on success so callers can fetch the staff document.
     */
    suspend fun signIn(shortId: String, pin: String): AuthResult {
        return try {
            val email = buildEmail(shortId)
            val result = auth.signInWithEmailAndPassword(email, pin).await()
            val user = result.user ?: return AuthResult.Failure("Authentication returned no user")

            EncryptedPrefs.putString(EncryptedPrefs.KEY_USER_ID, user.uid)
            EncryptedPrefs.putString(EncryptedPrefs.KEY_STAFF_SHORT_ID, shortId)

            // Fetch the staff document to determine admin status
            val staffRepo = StaffRepository()
            val staffDoc = staffRepo.getStaffByUid(user.uid)
            val isAdmin = staffDoc?.isAdmin ?: false

            AuthResult.Success(user, isAdmin)
        } catch (e: Exception) {
            AuthResult.Failure(mapFirebaseError(e.message))
        }
    }

    suspend fun signOut() {
        auth.signOut()
        EncryptedPrefs.putString(EncryptedPrefs.KEY_USER_ID, null)
        EncryptedPrefs.putBoolean(EncryptedPrefs.KEY_BIOMETRIC_ENABLED, false)
        EncryptedPrefs.putLong(EncryptedPrefs.KEY_LAST_AUTH_TIMESTAMP, 0L)
    }

    /** Called after successful biometric auth to validate the existing Firebase session. */
    suspend fun refreshToken(): Boolean {
        return try {
            auth.currentUser?.getIdToken(true)?.await() != null
        } catch (_: Exception) {
            false
        }
    }

    fun enableBiometric() = EncryptedPrefs.putBoolean(EncryptedPrefs.KEY_BIOMETRIC_ENABLED, true)

    fun isBiometricEnabled() = EncryptedPrefs.getBoolean(EncryptedPrefs.KEY_BIOMETRIC_ENABLED)

    fun getSavedShortId() = EncryptedPrefs.getString(EncryptedPrefs.KEY_STAFF_SHORT_ID)

    private fun buildEmail(shortId: String) = "${shortId.lowercase()}@taskflow.audit"

    private fun mapFirebaseError(message: String?): String = when {
        message == null -> "Authentication failed"
        message.contains("password") || message.contains("credential") ->
            "Incorrect PIN. Please try again."
        message.contains("user") && message.contains("not found") ->
            "Staff member not found. Contact your administrator."
        message.contains("network") || message.contains("timeout") ->
            "No connection. Check your internet and try again."
        message.contains("too many") || message.contains("blocked") ->
            "Too many failed attempts. Please wait before trying again."
        else -> "Authentication failed. Please try again."
    }
}
