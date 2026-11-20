package com.example.util

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

data class AuthLocalizedText(
    val en: String,
    val bn: String
) {
    fun resolve(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

object FirestoreErrorHandler {

    fun handleFirestoreError(
        exception: Throwable,
        operationType: OperationType,
        path: String?
    ): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = runCatching {
            com.google.firebase.FirebaseApp.getInstance()
            FirebaseAuth.getInstance().currentUser
        }.getOrNull()

        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        runCatching { Log.e("FirestoreError", "Firestore Error: $jsonString") }
        return jsonString
    }

    fun isPermissionDenied(exception: Throwable): Boolean {
        val causes = generateSequence(exception) { it.cause }
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return true
        if (causes.any { it is SecurityException }) return true
        val msg = causes.mapNotNull { it.message }.joinToString(" ")
        return msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
    }

    fun isNetworkError(exception: Throwable): Boolean {
        val causes = generateSequence(exception) { it.cause }.toList()
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            fsEx?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) {
            return true
        }
        val authEx = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
        if (authEx?.errorCode?.equals("ERROR_NETWORK_REQUEST_FAILED", ignoreCase = true) == true) {
            return true
        }
        if (causes.any {
                it is FirebaseNetworkException ||
                    it is UnknownHostException ||
                    it is SocketTimeoutException ||
                    it is ConnectException ||
                    it is IOException
            }
        ) {
            return true
        }
        val rawMsg = causes.mapNotNull { it.message }.joinToString(" ")
        return rawMsg.contains("network error", ignoreCase = true) ||
            rawMsg.contains("unable to resolve host", ignoreCase = true) ||
            rawMsg.contains("failed to connect", ignoreCase = true)
    }

    fun containsRawTechnicalDetails(message: String?): Boolean {
        if (message.isNullOrBlank()) return true
        val blocked = listOf(
            "PERMISSION_DENIED",
            "Missing or insufficient permissions",
            "FAILED_PRECONDITION",
            "UNAUTHENTICATED",
            "UNAVAILABLE",
            "DEADLINE_EXCEEDED",
            "FirebaseFirestoreException",
            "FirebaseAuthException",
            "FirebaseNetworkException",
            "FirebaseException",
            "ERROR_",
            "GetCredential",
            "io.grpc",
            "com.google.",
            "Status{code=",
            "Exception:"
        )
        return blocked.any { message.contains(it, ignoreCase = true) }
    }

    fun mapAuthError(
        exception: Throwable,
        isGoogleFlow: Boolean = false,
        fallback: AuthLocalizedText = if (isGoogleFlow) {
            AuthLocalizedText(
                en = "Google sign-in failed. Please try again.",
                bn = "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
            )
        } else {
            AuthLocalizedText(
                en = "Could not sign in. Please try again.",
                bn = "সাইন ইন করা যায়নি। আবার চেষ্টা করুন।"
            )
        }
    ): AuthLocalizedText {
        val causes = generateSequence(exception) { it.cause }.toList()

        if (causes.any { it is GetCredentialCancellationException }) {
            return AuthLocalizedText(
                en = "Google sign-in was cancelled.",
                bn = "গুগল সাইন-ইন বাতিল করা হয়েছে।"
            )
        }

        if (isNetworkError(exception)) {
            return AuthLocalizedText(
                en = "No internet connection. Check your network and try again.",
                bn = "ইন্টারনেট সংযোগ নেই। নেটওয়ার্ক চালু করে আবার চেষ্টা করুন।"
            )
        }

        if (causes.any { it is FirebaseTooManyRequestsException }) {
            return AuthLocalizedText(
                en = "Too many attempts. Please wait a moment and try again.",
                bn = "অনেকবার চেষ্টা করা হয়েছে। একটু পর আবার চেষ্টা করুন।"
            )
        }

        val authEx = causes.filterIsInstance<FirebaseAuthException>().firstOrNull()
        if (authEx != null) {
            val code = authEx.errorCode.uppercase()
            // Check WeakPassword BEFORE InvalidCredentials (WeakPassword extends InvalidCredentials)
            if (authEx is FirebaseAuthWeakPasswordException || code == "ERROR_WEAK_PASSWORD") {
                return AuthLocalizedText(
                    en = "Password is too weak. Use at least 6 characters.",
                    bn = "পাসওয়ার্ড দুর্বল। কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন।"
                )
            }
            if (authEx is FirebaseAuthUserCollisionException ||
                code == "ERROR_EMAIL_ALREADY_IN_USE" ||
                code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
            ) {
                return AuthLocalizedText(
                    en = "This email is already in use. Sign in instead.",
                    bn = "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট আছে। সাইন ইন করুন।"
                )
            }
            if (authEx is FirebaseAuthInvalidUserException ||
                code == "ERROR_USER_NOT_FOUND" ||
                code == "ERROR_USER_DISABLED"
            ) {
                return AuthLocalizedText(
                    en = "No account found with this email.",
                    bn = "এই ইমেইলে কোনো অ্যাকাউন্ট নেই।"
                )
            }
            if (code == "ERROR_INVALID_EMAIL") {
                return AuthLocalizedText(
                    en = "Please enter a valid email address.",
                    bn = "সঠিক ইমেইল ঠিকানা লিখুন।"
                )
            }
            if (code == "ERROR_WRONG_PASSWORD") {
                return AuthLocalizedText(
                    en = "Wrong password. Please try again.",
                    bn = "পাসওয়ার্ড ভুল হয়েছে। আবার চেষ্টা করুন।"
                )
            }
            if (code == "ERROR_TOO_MANY_REQUESTS") {
                return AuthLocalizedText(
                    en = "Too many attempts. Please wait a moment and try again.",
                    bn = "অনেকবার চেষ্টা করা হয়েছে। একটু পর আবার চেষ্টা করুন।"
                )
            }
            if (authEx is FirebaseAuthInvalidCredentialsException ||
                code == "ERROR_INVALID_CREDENTIAL" ||
                code == "ERROR_INVALID_LOGIN_CREDENTIALS"
            ) {
                return if (isGoogleFlow) {
                    AuthLocalizedText(
                        en = "Google sign-in failed. Please try again.",
                        bn = "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                    )
                } else {
                    AuthLocalizedText(
                        en = "Wrong email or password. Please try again.",
                        bn = "ইমেইল বা পাসওয়ার্ড ভুল হয়েছে। আবার চেষ্টা করুন।"
                    )
                }
            }
            return fallback
        }

        if (causes.any { it is GetCredentialException }) {
            return AuthLocalizedText(
                en = "Google sign-in failed. Please try again.",
                bn = "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
            )
        }

        return fallback
    }

    fun toUserFriendlyMessage(
        exception: Throwable,
        lang: AppLanguage,
        fallbackEn: String = "Something went wrong. Please try again.",
        fallbackBn: String = "কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করুন।"
    ): String {
        val causes = generateSequence(exception) { it.cause }.toList()
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsEx != null) {
            return when (fsEx.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    if (lang == AppLanguage.BN) {
                        "এই কাজটি করার অনুমতি নেই বা দোকানের তথ্য লোড হতে পারেনি।"
                    } else {
                        "You don't have permission for this action."
                    }
                }
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                    if (lang == AppLanguage.BN) {
                        "চালিয়ে যেতে আবার সাইন ইন করুন।"
                    } else {
                        "Please sign in again to continue."
                    }
                }
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                    if (lang == AppLanguage.BN) {
                        "ইন্টারনেট সংযোগ নেই। নেটওয়ার্ক চালু করে আবার চেষ্টা করুন।"
                    } else {
                        "No internet connection. Check your network and try again."
                    }
                }
                FirebaseFirestoreException.Code.NOT_FOUND -> {
                    if (lang == AppLanguage.BN) {
                        "প্রয়োজনীয় তথ্য পাওয়া যায়নি।"
                    } else {
                        "Requested record could not be found."
                    }
                }
                else -> if (lang == AppLanguage.BN) fallbackBn else fallbackEn
            }
        }

        val hasAuthOrCredEx = causes.any {
            it is FirebaseAuthException ||
                it is FirebaseTooManyRequestsException ||
                it is GetCredentialException
        }
        if (hasAuthOrCredEx) {
            return mapAuthError(
                exception = exception,
                isGoogleFlow = causes.any { it is GetCredentialException },
                fallback = AuthLocalizedText(en = fallbackEn, bn = fallbackBn)
            ).resolve(lang)
        }

        if (isPermissionDenied(exception)) {
            return if (lang == AppLanguage.BN) {
                "এই কাজটি করার অনুমতি নেই।"
            } else {
                "You don't have permission for this action."
            }
        }

        if (isNetworkError(exception)) {
            return if (lang == AppLanguage.BN) {
                "ইন্টারনেট সংযোগ নেই। নেটওয়ার্ক চালু করে আবার চেষ্টা করুন।"
            } else {
                "No internet connection. Check your network and try again."
            }
        }

        // Preserve deliberate, user-facing validation / domain messages from ShopLocale
        if (exception !is FirebaseException && !containsRawTechnicalDetails(exception.message)) {
            val clean = exception.message?.trim()
            if (!clean.isNullOrBlank()) return clean
        }

        return if (lang == AppLanguage.BN) fallbackBn else fallbackEn
    }
}
