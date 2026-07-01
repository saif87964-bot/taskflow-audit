package com.taskflow.audit.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.LogEntryDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LogRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getStaffLogFlow(staffId: String): Flow<List<LogEntryDocument>> = callbackFlow {
        val listener = db.collection(Collections.LOG_ENTRIES)
            .whereEqualTo("staffId", staffId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(LogEntryDocument::class.java)
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    fun getAllLogEntriesFlow(): Flow<List<LogEntryDocument>> = callbackFlow {
        val listener = db.collection(Collections.LOG_ENTRIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(LogEntryDocument::class.java)
                } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addEntry(entry: LogEntryDocument): String {
        val doc = db.collection(Collections.LOG_ENTRIES).document()
        doc.set(entry.copy(id = doc.id)).await()
        return doc.id
    }
}
