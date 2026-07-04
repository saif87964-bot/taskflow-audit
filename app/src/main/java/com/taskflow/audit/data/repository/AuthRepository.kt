package com.taskflow.audit.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.security.EncryptedPrefs
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val isAdmin: Boolean) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signIn(shortId: String, pin: String): AuthResult {
        return try {
            val email = buildEmail(shortId)
            val result = auth.signInWithEmailAndPassword(email, pin.padEnd(6, '0')).await()
            val user = result.user ?: return AuthResult.Failure("Authentication returned no user")

            EncryptedPrefs.putString(EncryptedPrefs.KEY_USER_ID, user.uid)
            EncryptedPrefs.putString(EncryptedPrefs.KEY_STAFF_SHORT_ID, shortId)

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

    suspend fun refreshToken(): Boolean {
        return try {
            auth.currentUser?.getIdToken(true)?.await() != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates a new Firebase Auth user for a staff member using a secondary FirebaseApp
     * instance so that the currently signed-in admin session is NOT disturbed.
     * Returns the new user's UID.
     */
    suspend fun createStaff(
        shortId: String,
        fullName: String,
        role: String,
        colorHex: String,
        isAdmin: Boolean
    ): String {
        val email = buildEmail(shortId)
        val tempAppName = "tf_create_${System.currentTimeMillis()}"
        val opts = FirebaseApp.getInstance().options
        val tempApp = FirebaseApp.initializeApp(AppRepositories.appContext, opts, tempAppName)
            ?: throw Exception("Could not init secondary Firebase app")
        val tempAuth = FirebaseAuth.getInstance(tempApp)
        return try {
            val result = tempAuth.createUserWithEmailAndPassword(email, "123400").await()
            result.user?.uid ?: throw Exception("No UID returned")
        } finally {
            tempApp.delete()
        }
    }

    /**
     * Updates the current user's password (used during forced PIN reset flow).
     */
    suspend fun updateCurrentUserPin(newPin: String) {
        val user = auth.currentUser ?: throw Exception("Session expired. Please log in again.")
        user.updatePassword(newPin.padEnd(6, '0')).await()
    }

    fun enableBiometric() = EncryptedPrefs.putBoolean(EncryptedPrefs.KEY_BIOMETRIC_ENABLED, true)

    fun isBiometricEnabled() = EncryptedPrefs.getBoolean(EncryptedPrefs.KEY_BIOMETRIC_ENABLED)

    fun getSavedShortId() = EncryptedPrefs.getString(EncryptedPrefs.KEY_STAFF_SHORT_ID)

    private fun buildEmail(shortId: String) = "${shortId.lowercase()}@taskflow.audit"

    private fun mapFirebaseError(message: String?): String {
        val m = message?.lowercase() ?: return "Authentication failed"
        return when {
            m.contains("password") || m.contains("credential") || m.contains("invalid login") ->
                "Incorrect PIN. Please try again."
            m.contains("user") && m.contains("not found") ->
                "Staff member not found. Contact your administrator."
            m.contains("network") || m.contains("timeout") || m.contains("failed to connect") || m.contains("unable to resolve") || m.contains("googleapis") ->
                "No connection. Check your internet and try again."
            m.contains("too many") || m.contains("blocked") ->
                "Too many failed attempts. Please wait before trying again."
            else -> "Error: ${message?.take(120)}"
        }
    }
}
