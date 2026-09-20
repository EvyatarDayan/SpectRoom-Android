package com.example.spectroom.data.remote

import android.app.Activity
import com.example.spectroom.repository.AccessGate
import com.example.spectroom.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class MfaSignInResult {
    data object Completed : MfaSignInResult()
    data class MfaRequired(val maskedPhone: String) : MfaSignInResult()
}

class MfaAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    @Volatile
    private var pendingResolver: MultiFactorResolver? = null

    @Volatile
    private var pendingVerificationId: String? = null

    @Volatile
    private var pendingEnrollPhoneE164: String? = null

    val isMfaEnrolled: Boolean
        get() = auth.currentUser?.multiFactor?.enrolledFactors?.isNotEmpty() == true

    /**
     * True when SMS MFA is enrolled in Firebase Auth.
     * Do not trust Firestore `mfaEnrolled` alone — after Auth wipe/restore the flag can
     * stay true with zero Auth factors, which would skip sign-in MFA and enrollment.
     */
    suspend fun isMfaEnrolledOrBypassed(): Boolean = isMfaEnrolled

    /** True while email/password succeeded but SMS MFA sign-in is still pending. */
    val hasPendingSignIn: Boolean
        get() = pendingResolver != null

    suspend fun signInWithPossibleMfa(email: String, password: String): MfaSignInResult {
        val normalized = email.trim().lowercase()
        // Do NOT call checkEmailPendingDeletion before Auth.
        // After admin restore, deletedEmails can stay stale and would show "Invalid credentials"
        // instead of the MFA code screen. Password validity is decided by Firebase Auth;
        // still-deleted accounts are blocked after Auth succeeds via evaluateAccessGate.
        return try {
            auth.signInWithEmailAndPassword(normalized, password).await()
            enforceAccessAfterPasswordSignIn()
            MfaSignInResult.Completed
        } catch (e: FirebaseAuthMultiFactorException) {
            // Password is correct — always open the Security Code screen (iOS parity for restore).
            beginMfaChallenge(e.resolver)
        } catch (e: Exception) {
            val mfa = unwrapMultiFactorException(e)
            if (mfa != null) {
                beginMfaChallenge(mfa.resolver)
            } else {
                throw unwrapAuthException(e)
            }
        }
    }

    /**
     * Non-MFA password success: reject soft/admin-deleted accounts before entering the app.
     */
    private suspend fun enforceAccessAfterPasswordSignIn() {
        when (val gate = authRepository.evaluateAccessGate()) {
            is AccessGate.Deleted -> {
                runCatching { auth.signOut() }
                error(
                    if (gate.byAdmin) {
                        "This account has been deleted."
                    } else {
                        "This email is linked to a recently deleted account. Try again later or contact support."
                    },
                )
            }
            is AccessGate.Banned -> {
                runCatching { auth.signOut() }
                error(gate.message)
            }
            is AccessGate.Frozen -> {
                runCatching { auth.signOut() }
                error(gate.message)
            }
            AccessGate.Allowed -> Unit
        }
    }

    private fun beginMfaChallenge(resolver: MultiFactorResolver): MfaSignInResult {
        pendingResolver = resolver
        val hint = resolver.hints.filterIsInstance<PhoneMultiFactorInfo>().firstOrNull()
        val masked = displayPhoneForHint(hint)
        return MfaSignInResult.MfaRequired(maskedPhone = masked)
    }

    private fun unwrapMultiFactorException(error: Throwable): FirebaseAuthMultiFactorException? {
        if (error is FirebaseAuthMultiFactorException) return error
        var current: Throwable? = error
        while (current != null) {
            if (current is FirebaseAuthMultiFactorException) return current
            current = current.cause
        }
        return null
    }

    private fun unwrapAuthException(error: Throwable): Throwable {
        var current: Throwable? = error
        while (current != null) {
            if (current is FirebaseAuthException) return current
            current = current.cause
        }
        return error
    }

    suspend fun sendSignInMfaCode(activity: Activity) {
        FirebasePhoneAuthDebug.applyIfNeeded(activity)
        val resolver = pendingResolver ?: error("MFA session expired")
        val hint = resolver.hints.filterIsInstance<PhoneMultiFactorInfo>().firstOrNull()
            ?: error("No phone MFA factor")
        pendingVerificationId = verifyPhoneForSignIn(activity, resolver, hint)
    }

    suspend fun confirmSignInMfa(code: String) {
        val resolver = pendingResolver ?: error("MFA session expired")
        val verificationId = pendingVerificationId ?: error("MFA session expired")
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        resolver.resolveSignIn(assertion).await()
        clearPending()
    }

    suspend fun startEnroll(activity: Activity, phoneE164: String) {
        FirebasePhoneAuthDebug.applyIfNeeded(activity)
        val user = auth.currentUser ?: error("Not signed in")
        user.reload().await()
        if (!user.isEmailVerified) {
            error("Verify your email before enabling SMS MFA")
        }
        pendingEnrollPhoneE164 = phoneE164
        val session = user.multiFactor.session.await()
        pendingVerificationId = verifyPhoneForEnroll(activity, phoneE164, session)
        db.collection("users").document(user.uid).set(
            mapOf(
                "phoneE164" to phoneE164,
                "mfaEnrolled" to false,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun fetchStoredPhoneE164(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.getString("phoneE164")?.trim()?.takeIf { it.startsWith("+") && it.length >= 10 }
    }

    suspend fun savePendingPhone(phoneE164: String) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        pendingEnrollPhoneE164 = phoneE164
        db.collection("users").document(uid).set(
            mapOf(
                "phoneE164" to phoneE164,
                "mfaEnrolled" to false,
            ),
            SetOptions.merge(),
        ).await()
    }

    fun isRateLimited(error: Throwable): Boolean {
        val message = (error.message ?: "").lowercase()
        val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
        return code.equals("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ||
            message.contains("too many requests") ||
            message.contains("unusual activity") ||
            message.contains("blocked all requests")
    }

    fun pendingEnrollPhone(): String? = pendingEnrollPhoneE164

    suspend fun sendEmailVerificationForMfa() {
        val user = auth.currentUser ?: error("Not signed in")
        user.reload().await()
        if (user.isEmailVerified) return
        try {
            user.getIdToken(true).await()
            com.google.firebase.functions.FirebaseFunctions
                .getInstance("us-central1")
                .getHttpsCallable("sendSpectRoomEmailVerificationEmail")
                .call(emptyMap<String, Any>())
                .await()
        } catch (_: Exception) {
            // Fall back to Firebase's default verification email if the branded callable fails.
            user.sendEmailVerification().await()
        }
    }

    suspend fun reloadAndIsEmailVerified(): Boolean {
        val user = auth.currentUser ?: error("Not signed in")
        user.reload().await()
        return user.isEmailVerified
    }

    fun currentEmail(): String = auth.currentUser?.email.orEmpty()

    suspend fun confirmEnroll(code: String, displayName: String = "Phone") {
        val user = auth.currentUser ?: error("Not signed in")
        val verificationId = pendingVerificationId ?: error("MFA session expired")
        val phone = pendingEnrollPhoneE164 ?: error("MFA session expired")
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        user.multiFactor.enroll(assertion, displayName).await()
        db.collection("users").document(user.uid).set(
            mapOf(
                "phoneE164" to phone,
                "phoneVerifiedAt" to System.currentTimeMillis() / 1000.0,
                "mfaEnrolled" to true,
            ),
            SetOptions.merge(),
        ).await()
        clearPending()
    }

    fun clearPending() {
        pendingResolver = null
        pendingVerificationId = null
        pendingEnrollPhoneE164 = null
    }

    /**
     * Prefer the real phone number over MFA display names like "Phone"
     * (iOS `FirebaseAuthService+MFA.maskedPhone(for:)` preference order).
     * SpectRoom shows the full number on the security-code screen (not masked).
     */
    private fun displayPhoneForHint(hint: PhoneMultiFactorInfo?): String {
        val phone = hint?.phoneNumber?.trim().orEmpty()
        if (phone.isNotEmpty()) return phone
        val name = hint?.displayName?.trim().orEmpty()
        if (name.isNotEmpty() && name.lowercase() !in GENERIC_MFA_DISPLAY_NAMES) {
            return name
        }
        return ""
    }

    companion object {
        private val GENERIC_MFA_DISPLAY_NAMES = setOf("phone", "sms", "mobile", "mfa")
    }

    private suspend fun verifyPhoneForSignIn(
        activity: Activity,
        resolver: MultiFactorResolver,
        hint: PhoneMultiFactorInfo,
    ): String = suspendCancellableCoroutine { cont ->
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval path unused for MFA confirm UI.
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                if (cont.isActive) cont.resume(verificationId)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setActivity(activity)
            .setMultiFactorHint(hint)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setMultiFactorSession(resolver.session)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private suspend fun verifyPhoneForEnroll(
        activity: Activity,
        phoneE164: String,
        session: com.google.firebase.auth.MultiFactorSession,
    ): String = suspendCancellableCoroutine { cont ->
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = Unit

            override fun onVerificationFailed(e: FirebaseException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                if (cont.isActive) cont.resume(verificationId)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setMultiFactorSession(session)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
