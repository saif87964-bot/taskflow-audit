package com.taskflow.audit.data.mock

import androidx.compose.ui.graphics.Color

data class StaffMember(
    val id: String,
    val initials: String,
    val fullName: String,
    val role: String,
    val isAdmin: Boolean = false,
    val avatarColor: Color
)

data class Engagement(
    val id: String,
    val clientName: String,
    val code: String,
    val type: EngagementType,
    val color: Color
)

data class TimeSession(
    val engagementId: String,
    val startTime: Long,
    val endTime: Long?,
    val date: String
)

data class StaffStatus(
    val staff: StaffMember,
    val isCheckedIn: Boolean,
    val currentEngagement: Engagement?,
    val hoursToday: Float,
    val sessions: List<TimeSession>
)

enum class EngagementType { AUDIT, TAX, ADVISORY, ADMIN }

object MockData {

    val engagementColors = listOf(
        Color(0xFF1565C0), Color(0xFF00695C), Color(0xFF6A1B9A),
        Color(0xFFE65100), Color(0xFF558B2F), Color(0xFF37474F),
        Color(0xFF880E4F), Color(0xFF455A64)
    )

    val engagements = listOf(
        Engagement("e1", "Al Baneen Connect", "ABC-2026", EngagementType.AUDIT, Color(0xFF1565C0)),
        Engagement("e2", "TRA – Statutory Follow-up", "TRA-SFU", EngagementType.TAX, Color(0xFFE65100)),
        Engagement("e3", "Moyo Zanzibar Limited", "MZL-2026", EngagementType.AUDIT, Color(0xFF00695C)),
        Engagement("e4", "Union Trust Resorts Ltd", "UTR-2026", EngagementType.AUDIT, Color(0xFF6A1B9A)),
        Engagement("e5", "Aqua Cool Limited", "ACL-2026", EngagementType.ADVISORY, Color(0xFF558B2F)),
        Engagement("e6", "New Jambiani Hotel Ltd", "NJH-2026", EngagementType.AUDIT, Color(0xFF880E4F)),
        Engagement("e7", "CPHK", "CPHK-2026", EngagementType.TAX, Color(0xFF37474F)),
        Engagement("e8", "Admin", "ADMIN", EngagementType.ADMIN, Color(0xFF455A64)),
    )

    val staffMembers = listOf(
        StaffMember("im", "IM", "Imtiaz M", "Senior Auditor", false, Color(0xFF1565C0)),
        StaffMember("kh", "KH", "Khalid H", "Audit Manager", true, Color(0xFF00695C)),
        StaffMember("av", "AV", "Anita V", "Auditor", false, Color(0xFF6A1B9A)),
        StaffMember("an", "AN", "Amina N", "Tax Consultant", false, Color(0xFFE65100)),
        StaffMember("jp", "JP", "James P", "Partner", true, Color(0xFF37474F)),
    )

    val todaySessions = mapOf(
        "im" to listOf(
            TimeSession("e1", 8_00_00_000L, 9_15_00_000L, "2026-06-27"),
            TimeSession("e2", 9_15_00_000L, 12_00_00_000L, "2026-06-27"),
            TimeSession("e3", 13_00_00_000L, null, "2026-06-27"),
        ),
        "kh" to listOf(
            TimeSession("e4", 8_30_00_000L, 10_00_00_000L, "2026-06-27"),
            TimeSession("e7", 10_00_00_000L, null, "2026-06-27"),
        ),
        "av" to listOf(
            TimeSession("e5", 9_00_00_000L, 11_30_00_000L, "2026-06-27"),
            TimeSession("e8", 11_30_00_000L, 12_30_00_000L, "2026-06-27"),
        ),
        "an" to listOf(
            TimeSession("e2", 8_00_00_000L, 9_45_00_000L, "2026-06-27"),
            TimeSession("e6", 9_45_00_000L, null, "2026-06-27"),
        ),
        "jp" to emptyList(),
    )

    fun getEngagementById(id: String) = engagements.find { it.id == id }

    fun getStaffStatuses(): List<StaffStatus> = staffMembers.map { staff ->
        val sessions = todaySessions[staff.id] ?: emptyList()
        val activeSession = sessions.find { it.endTime == null }
        val currentEngagement = activeSession?.let { getEngagementById(it.engagementId) }
        val completedHours = sessions
            .filter { it.endTime != null }
            .sumOf { (it.endTime!! - it.startTime).toDouble() / 3_600_000.0 }
            .toFloat()

        StaffStatus(
            staff = staff,
            isCheckedIn = activeSession != null,
            currentEngagement = currentEngagement,
            hoursToday = if (activeSession != null) completedHours + 1.5f else completedHours,
            sessions = sessions
        )
    }

    fun getStaffStatus(staffId: String): StaffStatus? = getStaffStatuses().find { it.staff.id == staffId }

    val notLoggedToday = listOf("jp")

    // Weekly hours per engagement (for admin view)
    val weeklyEngagementHours = mapOf(
        "e1" to 12.5f,
        "e2" to 18.0f,
        "e3" to 8.75f,
        "e4" to 14.0f,
        "e5" to 6.5f,
        "e6" to 9.25f,
        "e7" to 11.0f,
        "e8" to 4.0f,
    )

    val weeklyBudgetHours = mapOf(
        "e1" to 20.0f,
        "e2" to 30.0f,
        "e3" to 15.0f,
        "e4" to 25.0f,
        "e5" to 10.0f,
        "e6" to 20.0f,
        "e7" to 15.0f,
        "e8" to 8.0f,
    )
}
