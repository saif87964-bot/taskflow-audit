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
}
