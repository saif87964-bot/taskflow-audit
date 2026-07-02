package com.taskflow.audit.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.StaffDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StaffRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** Real-time stream of all staff — used by Admin Dashboard. */
    fun getAllStaffFlow(): Flow<List<StaffDocument>> = callbackFlow {
        val listener = db.collection(Collections.STAFF)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val staff = snapshot.documents.mapNotNull {
                    it.toObject(StaffDocument::class.java)
                }
                trySend(staff)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getStaffByUid(uid: String): StaffDocument? =
        db.collection(Collections.STAFF).document(uid).get().await()
            .toObject(StaffDocument::class.java)

    suspend fun getStaffByShortId(shortId: String): StaffDocument? =
        db.collection(Collections.STAFF)
            .whereEqualTo("shortId", shortId)
            .limit(1)
            .get().await()
            .documents.firstOrNull()
            ?.toObject(StaffDocument::class.java)

    /**
     * Creates the Firestore staff document for a newly provisioned Firebase Auth user.
     */
    suspend fun createStaffDoc(
        uid: String,
        shortId: String,
        fullName: String,
        role: String,
        colorHex: String,
        isAdmin: Boolean
    ) {
        val doc = StaffDocument(
            uid = uid,
            shortId = shortId.lowercase(),
            fullName = fullName,
            role = role,
            initials = shortId.uppercase().take(2),
            colorHex = colorHex,
            isAdmin = isAdmin,
            email = "${shortId.lowercase()}@taskflow.audit",
            pendingPinReset = false
        )
        db.collection(Collections.STAFF).document(uid).set(doc).await()
    }

    /** Flags the staff member as needing to reset their PIN on next login. */
    suspend fun setPendingPinReset(uid: String) {
        db.collection(Collections.STAFF).document(uid)
            .update("pendingPinReset", true).await()
    }

    /** Clears the pending PIN reset flag after the user has set a new PIN. */
    suspend fun clearPendingPinReset(uid: String) {
        db.collection(Collections.STAFF).document(uid)
            .update("pendingPinReset", false).await()
    }

    suspend fun updateStaff(uid: String, fullName: String, role: String, colorHex: String, isAdmin: Boolean) {
        db.collection(Collections.STAFF).document(uid).update(mapOf(
            "fullName" to fullName,
            "role" to role,
            "colorHex" to colorHex,
            "isAdmin" to isAdmin
        )).await()
    }
}
