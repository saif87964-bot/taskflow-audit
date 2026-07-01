package com.taskflow.audit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.TaskDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** Real-time stream of tasks assigned to a specific staff member. */
    fun getTasksForStaffFlow(staffId: String): Flow<List<TaskDocument>> = callbackFlow {
        val listener = db.collection(Collections.TASKS)
            .whereEqualTo("assigneeId", staffId)
            .addSnapshotListener { snapshot, _ ->
                val tasks = snapshot?.documents?.mapNotNull {
                    it.toObject(TaskDocument::class.java)
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    /** All tasks — admin view. */
    fun getAllTasksFlow(): Flow<List<TaskDocument>> = callbackFlow {
        val listener = db.collection(Collections.TASKS)
            .addSnapshotListener { snapshot, _ ->
                val tasks = snapshot?.documents?.mapNotNull {
                    it.toObject(TaskDocument::class.java)
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Staff can only update the status field on their own tasks.
     * Firestore Security Rules enforce this — the update will be rejected
     * server-side if the caller tries to change any other field.
     */
    suspend fun updateStatus(taskId: String, newStatus: String) {
        db.collection(Collections.TASKS).document(taskId)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
    }

    suspend fun createTask(task: TaskDocument): String {
        val doc = db.collection(Collections.TASKS).document()
        doc.set(task.copy(id = doc.id)).await()
        return doc.id
    }

    suspend fun deleteTask(taskId: String) {
        db.collection(Collections.TASKS).document(taskId).delete().await()
    }
}
