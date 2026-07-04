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
            .addSnapshotListener { snapshot, error ->
                if (error != null) android.util.Log.w("EngagementRepo", "engagements listener error", error)
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

    /**
     * Creates a new engagement document with an auto-generated ID.
     */
    suspend fun createEngagement(
        code: String,
        clientName: String,
        type: String,
        colorHex: String,
        budgetHours: Int
    ) {
        val docRef = db.collection(Collections.ENGAGEMENTS).document()
        val doc = EngagementDocument(
            id = docRef.id,
            code = code,
            name = clientName,
            clientName = clientName,
            type = type,
            colorHex = colorHex,
            budgetHours = budgetHours,
            isActive = true
        )
        docRef.set(doc).await()
    }

    /**
     * Updates an existing engagement's mutable fields.
     */
    suspend fun updateEngagement(
        id: String,
        code: String,
        clientName: String,
        type: String,
        colorHex: String,
        budgetHours: Int
    ) {
        db.collection(Collections.ENGAGEMENTS).document(id)
            .update(
                mapOf(
                    "code" to code,
                    "name" to clientName,
                    "clientName" to clientName,
                    "type" to type,
                    "colorHex" to colorHex,
                    "budgetHours" to budgetHours
                )
            ).await()
    }

    /** Soft-deletes an engagement by marking it inactive. */
    suspend fun archiveEngagement(id: String) {
        db.collection(Collections.ENGAGEMENTS).document(id)
            .update("isActive", false).await()
    }
}
