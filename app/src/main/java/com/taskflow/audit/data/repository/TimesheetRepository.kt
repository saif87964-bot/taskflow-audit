package com.taskflow.audit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.TimeSessionDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TimesheetRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** Live stream of today's sessions for a staff member. */
    fun getTodaySessionsFlow(staffId: String): Flow<List<TimeSessionDocument>> = callbackFlow {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time

        val listener = db.collection(Collections.TIME_SESSIONS)
            .whereEqualTo("staffId", staffId)
            .whereGreaterThanOrEqualTo("startTime", Timestamp(startOfDay))
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(TimeSessionDocument::class.java)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /** All active sessions across all staff — for Admin Dashboard. */
    fun getActiveSessionsFlow(): Flow<List<TimeSessionDocument>> = callbackFlow {
        val listener = db.collection(Collections.TIME_SESSIONS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(TimeSessionDocument::class.java)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    suspend fun checkIn(staffId: String, engagementId: String): String {
        val doc = db.collection(Collections.TIME_SESSIONS).document()
        val session = TimeSessionDocument(
            id = doc.id,
            staffId = staffId,
            engagementId = engagementId,
            isActive = true
        )
        doc.set(session).await()
        return doc.id
    }

    suspend fun checkOut(sessionId: String, durationMinutes: Int) {
        db.collection(Collections.TIME_SESSIONS).document(sessionId)
            .update(
                mapOf(
                    "endTime" to Timestamp.now(),
                    "isActive" to false,
                    "durationMinutes" to durationMinutes
                )
            ).await()
    }

    suspend fun switchEngagement(activeSessionId: String, staffId: String, newEngagementId: String, elapsedMinutes: Int): String {
        checkOut(activeSessionId, elapsedMinutes)
        return checkIn(staffId, newEngagementId)
    }
}
