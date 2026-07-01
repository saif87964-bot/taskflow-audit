package com.taskflow.audit.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.taskflow.audit.data.repository.AuthRepository
import com.taskflow.audit.data.repository.EngagementRepository
import com.taskflow.audit.data.repository.LogRepository
import com.taskflow.audit.data.repository.StaffRepository
import com.taskflow.audit.data.repository.TaskRepository
import com.taskflow.audit.data.repository.TimesheetRepository
import com.taskflow.audit.security.EncryptedPrefs

/**
 * Singleton service locator. Initialised once in TaskFlowApp.onCreate().
 * Provides access to all repositories without Hilt DI overhead.
 */
object AppRepositories {

    lateinit var appContext: android.content.Context
        private set

    lateinit var auth: AuthRepository
        private set

    lateinit var staff: StaffRepository
        private set

    lateinit var engagements: EngagementRepository
        private set

    lateinit var timesheet: TimesheetRepository
        private set

    lateinit var tasks: TaskRepository
        private set

    lateinit var log: LogRepository
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        EncryptedPrefs.init(context)
        auth = AuthRepository(FirebaseAuth.getInstance())
        staff = StaffRepository()
        engagements = EngagementRepository()
        timesheet = TimesheetRepository()
        tasks = TaskRepository()
        log = LogRepository()
    }
}
