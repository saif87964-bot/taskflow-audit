package com.taskflow.audit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// ─── Staff ──────────────────────────────────────────────────────────────────

data class StaffDocument(
    val uid: String = "",
    val shortId: String = "",
    val fullName: String = "",
    val role: String = "",
    val initials: String = "",
    val colorHex: String = "#1565C0",
    // @PropertyName needed: Kotlin "isXxx" booleans otherwise serialize as "xxx",
    // silently mismatching the field name used in queries and the seed script
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    val email: String = "",
    val pendingPinReset: Boolean = false
)

// ─── Engagement ─────────────────────────────────────────────────────────────

data class EngagementDocument(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val clientName: String = "",
    val type: String = "AUDIT",
    val colorHex: String = "#00897B",
    val budgetHours: Int = 20,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
)

// ─── Time Session ───────────────────────────────────────────────────────────

data class TimeSessionDocument(
    @DocumentId val id: String = "",
    val staffId: String = "",
    val engagementId: String = "",
    @ServerTimestamp val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationMinutes: Int = 0,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
)

// ─── Task ────────────────────────────────────────────────────────────────────

data class TaskDocument(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val engagementId: String = "",
    val assigneeId: String = "",
    val priority: String = "MEDIUM",
    val status: String = "PENDING",
    val dueDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val createdBy: String = ""
)

// ─── Log Entry ───────────────────────────────────────────────────────────────

data class LogEntryDocument(
    @DocumentId val id: String = "",
    val staffId: String = "",
    val engagementId: String = "",
    val note: String = "",
    val category: String = "OBSERVATION",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// ─── Firestore collection paths ──────────────────────────────────────────────

object Collections {
    const val STAFF = "staff"
    const val ENGAGEMENTS = "engagements"
    const val TIME_SESSIONS = "timeSessions"
    const val TASKS = "tasks"
    const val LOG_ENTRIES = "logEntries"
}
