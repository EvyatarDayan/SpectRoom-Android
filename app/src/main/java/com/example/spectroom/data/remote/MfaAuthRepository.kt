package com.example.spectroom.data.remote

import android.app.Activity
import com.example.spectroom.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
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

    /** True when SMS MFA is enrolled in Auth, or Firestore marks a seeded/test account as ready. */
    suspend fun isMfaEnrolledOrBypassed(): Boolean {
        if (isMfaEnrolled) return true
        val uid = auth.currentUser?.uid ?: return false
        val snapshot = runCatching {
            db.collection("users").document(uid).get().await()
        }.getOrNull() ?: return false
        return snapshot.getBoolean("mfaEnrolled") == true
    }

    /** True while email/password succeeded but SMS MFA sign-in is still pending. */
    val hasPendingSignIn: Boolean
        get() = pendingResolver != null

    suspend fun signInWithPossibleMfa(email: String, password: String): MfaSignInResult {
        val normalized = email.trim().lowercase()
        authRepository.ensureEmailAllowedForAuth(normalized)
        return try {
            auth.signInWithEmailAndPassword(normalized, password).await()
            MfaSignInResult.Completed
        } catch (e: FirebaseAuthMultiFactorException) {
            // Password was correct but MFA is required — re-check deletion before SMS.
            authRepository.ensureEmailAllowedForAuth(normalized)
            val resolver = e.resolver
            pendingResolver = resolver
            val hint = resolver.hints.filterIsInstance<PhoneMultiFactorInfo>().firstOrNull()
            val masked = hint?.displayName?.takeIf { it.isNotBlank() }
                ?: hint?.phoneNumber?.let { maskPhone(it) }
                ?: "your phone"
            MfaSignInResult.MfaRequired(maskedPhone = masked)
        }
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

    private fun maskPhone(phone: String): String {
        if (phone.length < 4) return phone
        return "${phone.take(3)}••••${phone.takeLast(2)}"
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
