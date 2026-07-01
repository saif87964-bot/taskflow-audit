package com.taskflow.audit.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.EngagementDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EngagementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** Active engagements — cached offline by Firestore SDK automatically. */
    fun getActiveEngagementsFlow(): Flow<List<EngagementDocument>> = callbackFlow {
        val listener = db.collection(Collections.ENGAGEMENTS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                val engagements = snapshot?.documents?.mapNotNull {
                    it.toObject(EngagementDocument::class.java)
                } ?: emptyList()
                trySend(engagements)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getEngagementById(id: String): EngagementDocument? =
        db.collection(Collections.ENGAGEMENTS).document(id).get().await()
            .toObject(EngagementDocument::class.java)
}
