package com.taskflow.audit.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.taskflow.audit.data.model.Collections
import com.taskflow.audit.data.model.TimeSessionDocument
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TimesheetRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun startOfDay(): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(cal.time)
    }

    /**
     * Live stream of today's sessions for a staff member.
     * Queries by staffId only and filters/sorts client-side so no
     * composite Firestore index is required.
     */
    fun getTodaySessionsFlow(staffId: String): Flow<List<TimeSessionDocument>> = callbackFlow {
        val dayStart = startOfDay()
        val listener = db.collection(Collections.TIME_SESSIONS)
            .whereEqualTo("staffId", staffId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TimesheetRepo", "today sessions listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents
                    ?.mapNotNull { it.toObject(TimeSessionDocument::class.java) }
                    ?.filter { s -> s.isActive || (s.startTime != null && s.startTime >= dayStart) }
                    ?.sortedByDescending { it.startTime?.seconds ?: Long.MAX_VALUE }
                    ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /** All active sessions across all staff — for Admin Dashboard live status. */
    fun getActiveSessionsFlow(): Flow<List<TimeSessionDocument>> = callbackFlow {
        val listener = db.collection(Collections.TIME_SESSIONS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TimesheetRepo", "active sessions listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(TimeSessionDocument::class.java)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /**
     * All of today's sessions across all staff (active and completed) —
     * lets the Admin Dashboard tell "worked earlier today" apart from
     * "never logged in today". Single-field range query, auto-indexed.
     */
    fun getTodayAllSessionsFlow(): Flow<List<TimeSessionDocument>> = callbackFlow {
        val listener = db.collection(Collections.TIME_SESSIONS)
            .whereGreaterThanOrEqualTo("startTime", startOfDay())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TimesheetRepo", "today-all sessions listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(TimeSessionDocument::class.java)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Checks the staff member in. The device clock is the source of truth,
     * so startTime is written client-side — the session is immediately
     * visible in queries and works offline. Any session left active
     * (e.g. after a crash or double-tap) is closed first.
     */
    suspend fun checkIn(staffId: String, engagementId: String): String {
        closeAnyActiveSessions(staffId)

        val doc = db.collection(Collections.TIME_SESSIONS).document()
        val session = TimeSessionDocument(
            id = doc.id,
            staffId = staffId,
            engagementId = engagementId,
            startTime = Timestamp.now(),
            isActive = true
        )
        doc.set(session).await()
        return doc.id
    }

    /**
     * Closes every active session for the staff member, computing elapsed
     * minutes from each session's own startTime. Used as the check-out
     * fallback and to clean up stale sessions before a new check-in.
     */
    suspend fun closeAllActiveSessions(staffId: String) {
        val active = db.collection(Collections.TIME_SESSIONS)
            .whereEqualTo("staffId", staffId)
            .whereEqualTo("isActive", true)
            .get().await()
        for (docSnap in active.documents) {
            val s = docSnap.toObject(TimeSessionDocument::class.java) ?: continue
            val minutes = s.startTime?.let {
                ((Timestamp.now().seconds - it.seconds) / 60).toInt().coerceAtLeast(0)
            } ?: 0
            checkOut(docSnap.id, minutes)
        }
    }

    private suspend fun closeAnyActiveSessions(staffId: String) {
        try {
            closeAllActiveSessions(staffId)
        } catch (e: Exception) {
            Log.w("TimesheetRepo", "could not close stale active sessions", e)
        }
    }

    suspend fun checkOut(sessionId: String, durationMinutes: Int) {
        db.collection(Collections.TIME_SESSIONS).document(sessionId)
            .update(
                mapOf(
                    "endTime" to Timestamp.now(),
                    "isActive" to false,
                    "durationMinutes" to durationMinutes.coerceAtLeast(0)
                )
            ).await()
    }

    suspend fun switchEngagement(activeSessionId: String, staffId: String, newEngagementId: String, elapsedMinutes: Int): String {
        checkOut(activeSessionId, elapsedMinutes)
        return checkIn(staffId, newEngagementId)
    }
}
