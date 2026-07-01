package com.taskflow.audit

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.taskflow.audit.data.AppRepositories

class TaskFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // App Check — Play Integrity in production, debug token in debug builds
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.USE_APP_CHECK_DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        // Firestore: enable offline persistence with a 100 MB local cache
        val cacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100L * 1024 * 1024)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings)
            .build()

        // Initialise the repository layer (sets up EncryptedPrefs context)
        AppRepositories.init(this)
    }
}
