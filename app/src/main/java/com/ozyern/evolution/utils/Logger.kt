package com.ozyern.evolution.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log
import com.ozyern.evolution.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

fun logVerbose(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.v(tag.ensureLength(), message, cause) else Log.v(tag.ensureLength(), message)
}

fun logDebug(tag: String, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag.ensureLength(), message)
}

fun logInfo(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.i(tag.ensureLength(), message, cause) else Log.i(tag.ensureLength(), message)
}

/**
 * Logs recoverable problems to Logcat. Replaces Firebase Crashlytics, which was removed
 * because this app doesn't have a crash-reporting backend of its own.
 */
@Singleton
class CrashLogger @Inject constructor() {

    /** Log a recoverable exception at warning level */
    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        if (cause != null) Log.w(tag.ensureLength(), cause.message ?: message, cause)
        else Log.w(tag.ensureLength(), message)
    }

    /** Log a recoverable exception at error level */
    fun logError(tag: String, message: String, cause: Throwable) {
        Log.e(tag.ensureLength(), message, cause)
    }
}

/** Log tags must be max 23 characters before Android 8/Oreo */
private fun String.ensureLength() =
    if (length <= MaxTagLength || SDK_INT >= VERSION_CODES.O) this else substring(0, MaxTagLength)

private const val MaxTagLength = 23
