package com.taskflow.audit.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Lightweight root detection. Not foolproof against sophisticated root-hiding tools,
 * but catches the vast majority of rooted devices and emulators.
 *
 * On a confirmed rooted device the app shows a blocking warning rather than crashing —
 * this avoids false-positive lockouts on legitimate devices while still discouraging use
 * on compromised environments.
 */
object RootDetector {

    private val suBinaryPaths = listOf(
        "/system/app/Superuser.apk",
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/system/sd/xbin/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu"
    )

    private val rootPackages = listOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.topjohnwu.magisk",
        "io.github.huskydg.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root"
    )

    fun isRooted(context: Context): Boolean =
        checkSuBinaries() || checkRootPackages(context) || checkBuildTags() || checkTestKeys()

    private fun checkSuBinaries(): Boolean =
        suBinaryPaths.any { File(it).exists() }

    private fun checkRootPackages(context: Context): Boolean {
        val pm = context.packageManager
        return rootPackages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun checkBuildTags(): Boolean =
        Build.TAGS?.contains("test-keys") == true

    private fun checkTestKeys(): Boolean =
        Build.FINGERPRINT?.let {
            it.contains("generic") || it.contains("unknown") ||
                it.contains("test-keys") || it.contains("userdebug")
        } == true
}
