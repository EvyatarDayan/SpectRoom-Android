package com.example.spectroom.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

/**
 * Mirrors iOS `FirebaseBootstrap.applyDebugPhoneAuthSettings()`.
 * On debug emulators only: skip Play Integrity / reCAPTCHA so Firebase Console
 * “Phone numbers for testing” work for MFA enrollment.
 */
object FirebasePhoneAuthDebug {
    private const val TAG = "FirebasePhoneAuthDebug"

    fun applyIfNeeded(context: Context? = null) {
        val debuggable = context?.let {
            (it.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } ?: true
        if (!debuggable || !isEmulator()) {
            FirebaseAuth.getInstance().firebaseAuthSettings
                .setAppVerificationDisabledForTesting(false)
            return
        }
        FirebaseAuth.getInstance().firebaseAuthSettings
            .setAppVerificationDisabledForTesting(true)
        Log.i(TAG, "Phone/MFA test mode ON (emulator). Use Firebase Console test phone numbers.")
    }

    fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.PRODUCT.contains("emulator", ignoreCase = true) ||
            Build.PRODUCT.contains("gphone", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)
    }
}
