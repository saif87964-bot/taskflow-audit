# ─── TaskFlow Audit ProGuard / R8 Rules ────────────────────────────────────
# R8 full mode is enabled in build.gradle.kts (proguard-android-optimize.txt
# already sets aggressiveOptimization=true). Add only what must be preserved.

# ─── Firebase ───────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firestore data model classes so @DocumentId / @ServerTimestamp work
-keep class com.taskflow.audit.data.model.** { *; }

# ─── Kotlin coroutines ──────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ─── Kotlin metadata (needed for sealed classes, data classes) ──────────────
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable   # Keeps stack traces readable in Crashlytics

# ─── Compose ────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Security Crypto / Biometric ────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

# ─── WorkManager ────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Prevent reflection attacks on ViewModels ───────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel$Factory { *; }

# ─── Remove logging in release ──────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
