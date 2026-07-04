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

    /**
     * Staff's own log entries. Sorted client-side so the equality-filter
     * query needs no composite Firestore index.
     */
    fun getStaffLogFlow(staffId: String): Flow<List<LogEntryDocument>> = callbackFlow {
        val listener = db.collection(Collections.LOG_ENTRIES)
            .whereEqualTo("staffId", staffId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("LogRepo", "staff log listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(LogEntryDocument::class.java)
                }?.sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    fun getAllLogEntriesFlow(): Flow<List<LogEntryDocument>> = callbackFlow {
        val listener = db.collection(Collections.LOG_ENTRIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("LogRepo", "all logs listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
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
