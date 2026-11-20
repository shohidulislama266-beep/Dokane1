package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.InviteCodeConfig
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.util.AppLanguage
import com.example.util.AuthLocalizedText
import com.example.util.FirestoreErrorHandler
import com.example.util.ShopLocale
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthenticatedUser(
    val uid: String,
    val email: String = "",
    val displayName: String = ""
)

enum class AuthBusyAction {
    NONE,
    GOOGLE,
    EMAIL,
    RESET,
    OTHER
}

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class AuthenticatedNoStore(val user: AuthenticatedUser) : AuthUiState()
    data class AuthenticatedWithStore(
        val user: AuthenticatedUser,
        val storeId: String,
        val userProfile: UserProfile
    ) : AuthUiState()
    data class DemoMode(val storeId: String = "demo_store") : AuthUiState()
}

internal class LocalizedAuthException(
    val localized: AuthLocalizedText
) : Exception(localized.en)

internal interface AuthBackend {
    val currentUser: AuthenticatedUser?
    fun addAuthStateListener(onUserChanged: (AuthenticatedUser?) -> Unit): () -> Unit
    suspend fun signInWithGoogle(context: Context): AuthenticatedUser
    suspend fun signInWithEmail(email: String, password: String): AuthenticatedUser
    suspend fun signUpWithEmail(email: String, password: String): AuthenticatedUser
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun refreshIdToken()
    suspend fun signOut(context: Context)
}

internal class FirebaseAuthBackend : AuthBackend {
    private val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (e: Exception) {
        Log.w("AuthViewModel", "FirebaseAuth unavailable: ${e.message}")
        null
    }

    private fun FirebaseUser.toAuthenticatedUser(): AuthenticatedUser = AuthenticatedUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty()
    )

    override val currentUser: AuthenticatedUser?
        get() = auth?.currentUser?.toAuthenticatedUser()

    override fun addAuthStateListener(onUserChanged: (AuthenticatedUser?) -> Unit): () -> Unit {
        val firebaseAuth = auth ?: return {}
        val listener = FirebaseAuth.AuthStateListener { fa ->
            onUserChanged(fa.currentUser?.toAuthenticatedUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        return { runCatching { firebaseAuth.removeAuthStateListener(listener) } }
    }

    private fun requireAuth(): FirebaseAuth {
        return auth ?: throw LocalizedAuthException(
            AuthLocalizedText(
                en = "Sign-in service is unavailable right now.",
                bn = "এই মুহূর্তে সাইন-ইন সেবা চালু নেই।"
            )
        )
    }

    override suspend fun signInWithGoogle(context: Context): AuthenticatedUser {
        val firebaseAuth = requireAuth()
        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }
        if (serverClientId.isBlank()) {
            throw LocalizedAuthException(
                AuthLocalizedText(
                    en = "Google sign-in is not configured.",
                    bn = "গুগল সাইন-ইন সেটআপ করা নেই।"
                )
            )
        }

        val credentialManager = CredentialManager.create(context)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = serverClientId
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val response = credentialManager.getCredential(context = context, request = request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            firebaseAuth.signInWithCredential(authCredential).await()
            val user = firebaseAuth.currentUser?.toAuthenticatedUser()
                ?: throw LocalizedAuthException(
                    AuthLocalizedText(
                        en = "Google sign-in failed. Please try again.",
                        bn = "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                    )
                )
            return user
        } else {
            throw LocalizedAuthException(
                AuthLocalizedText(
                    en = "Google sign-in failed. Please try again.",
                    bn = "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            )
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthenticatedUser {
        val firebaseAuth = requireAuth()
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return firebaseAuth.currentUser?.toAuthenticatedUser()
            ?: throw LocalizedAuthException(
                AuthLocalizedText(
                    en = "Could not sign in. Please try again.",
                    bn = "সাইন ইন করা যায়নি। আবার চেষ্টা করুন।"
                )
            )
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthenticatedUser {
        val firebaseAuth = requireAuth()
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return firebaseAuth.currentUser?.toAuthenticatedUser()
            ?: throw LocalizedAuthException(
                AuthLocalizedText(
                    en = "Could not create account. Please try again.",
                    bn = "অ্যাকাউন্ট তৈরি করা যায়নি। আবার চেষ্টা করুন।"
                )
            )
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        val firebaseAuth = requireAuth()
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun refreshIdToken() {
        runCatching { auth?.currentUser?.getIdToken(false)?.await() }
    }

    override suspend fun signOut(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(
                ClearCredentialStateRequest()
            )
        }
        runCatching { auth?.signOut() }
    }
}

internal class DemoAuthBackend : AuthBackend {
    override val currentUser: AuthenticatedUser? = null

    override fun addAuthStateListener(onUserChanged: (AuthenticatedUser?) -> Unit): () -> Unit = {}

    private fun demoUnsupported(): Nothing = throw LocalizedAuthException(
        AuthLocalizedText(
            en = "Exit Demo first to sign in or create an account.",
            bn = "সাইন ইন বা অ্যাকাউন্ট খুলতে আগে ডেমো থেকে বের হন।"
        )
    )

    override suspend fun signInWithGoogle(context: Context): AuthenticatedUser = demoUnsupported()
    override suspend fun signInWithEmail(email: String, password: String): AuthenticatedUser = demoUnsupported()
    override suspend fun signUpWithEmail(email: String, password: String): AuthenticatedUser = demoUnsupported()
    override suspend fun sendPasswordResetEmail(email: String) = demoUnsupported()
    override suspend fun refreshIdToken() = Unit
    override suspend fun signOut(context: Context) = Unit
}

class AuthViewModel internal constructor(
    private val repository: StoreRepository,
    private val authBackend: AuthBackend,
    private val isOnlineProvider: ((Context?) -> Boolean)?,
    private val profileObserver: (String) -> Flow<UserProfile?>,
    private val profileLoader: suspend (String) -> UserProfile?,
    private val profileSaver: suspend (UserProfile) -> Unit,
    private val inviteJoiner: suspend (String, AuthenticatedUser) -> Result<String>,
    initialDemoMode: Boolean = false,
    internal var onExitDemoCallback: (() -> Unit)? = null
) : ViewModel() {

    constructor(repository: StoreRepository) : this(
        repository = repository,
        authBackend = if (repository.hasCloudDatabase) FirebaseAuthBackend() else DemoAuthBackend(),
        isOnlineProvider = null,
        profileObserver = { uid -> repository.observeUserProfile(uid) },
        profileLoader = { uid -> repository.getUserProfileOnce(uid) },
        profileSaver = { profile -> repository.saveUserProfile(profile) },
        inviteJoiner = { code, user ->
            repository.joinStoreWithInvite(
                code = code,
                uid = user.uid,
                email = user.email,
                displayName = user.displayName
            )
        }
    )

    val currentLanguage: StateFlow<AppLanguage> = repository.currentLanguage

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (initialDemoMode) AuthUiState.DemoMode(storeId = "demo_store") else AuthUiState.Loading
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _busyAction = MutableStateFlow(AuthBusyAction.NONE)
    val busyAction: StateFlow<AuthBusyAction> = _busyAction.asStateFlow()

    private var activeErrorLocalized: AuthLocalizedText? = null
    private var activeSuccessLocalized: AuthLocalizedText? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _pendingInviteCode = MutableStateFlow("")
    val pendingInviteCode: StateFlow<String> = _pendingInviteCode.asStateFlow()

    private var profileJob: Job? = null
    private var languageSyncJob: Job? = null
    private var boundUserId: String? = null
    private var isInDemoMode = initialDemoMode
    private var isAuthFlowInProgress = false
    private var removeAuthListener: (() -> Unit)? = null

    init {
        languageSyncJob = viewModelScope.launch {
            currentLanguage.collect { lang ->
                refreshDisplayedMessages(lang)
            }
        }

        if (!initialDemoMode) {
            val existingUser = authBackend.currentUser
            if (existingUser == null) {
                _uiState.value = AuthUiState.Unauthenticated
            } else {
                observeUserStoreBinding(existingUser)
            }

            removeAuthListener = authBackend.addAuthStateListener { user ->
                if (isInDemoMode || isAuthFlowInProgress) return@addAuthStateListener
                if (user == null) {
                    boundUserId = null
                    profileJob?.cancel()
                    _uiState.value = AuthUiState.Unauthenticated
                } else {
                    observeUserStoreBinding(user)
                }
            }
        }
    }

    private fun refreshDisplayedMessages(lang: AppLanguage = currentLanguage.value) {
        _errorMessage.value = activeErrorLocalized?.resolve(lang)
        _successMessage.value = activeSuccessLocalized?.resolve(lang)
    }

    private fun setLocalizedError(message: AuthLocalizedText) {
        activeSuccessLocalized = null
        activeErrorLocalized = message
        refreshDisplayedMessages()
    }

    private fun setLocalizedSuccess(message: AuthLocalizedText) {
        activeErrorLocalized = null
        activeSuccessLocalized = message
        refreshDisplayedMessages()
    }

    private fun setBusy(action: AuthBusyAction) {
        _busyAction.value = action
        _isBusy.value = action != AuthBusyAction.NONE
    }

    private fun checkOnline(context: Context?): Boolean {
        isOnlineProvider?.let { return it(context) }
        val ctx = context ?: repository.appContext ?: return true
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        return runCatching {
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    private fun showOfflineError() {
        setLocalizedError(
            AuthLocalizedText(
                en = "No internet connection. Check your network and try again.",
                bn = "ইন্টারনেট সংযোগ নেই। নেটওয়ার্ক চালু করে আবার চেষ্টা করুন।"
            )
        )
    }

    private fun observeUserStoreBinding(user: AuthenticatedUser) {
        if (profileJob?.isActive == true && boundUserId == user.uid) return
        val switchedAccount = boundUserId != user.uid
        boundUserId = user.uid
        if (switchedAccount && activeErrorLocalized == null) {
            clearMessages()
        }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            profileObserver(user.uid).collectLatest { profile ->
                if (isInDemoMode) return@collectLatest
                if (profile != null && profile.storeId.isNotBlank()) {
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = user,
                        storeId = profile.storeId,
                        userProfile = profile
                    )
                } else {
                    _uiState.value = AuthUiState.AuthenticatedNoStore(user)
                }
            }
        }
    }

    private suspend fun handlePostSignIn(user: AuthenticatedUser) {
        isInDemoMode = false
        runCatching { authBackend.refreshIdToken() }

        val existing = runCatching { profileLoader(user.uid) }.getOrNull()
        if (existing == null) {
            runCatching {
                profileSaver(
                    UserProfile(
                        id = user.uid,
                        email = user.email,
                        storeId = "",
                        role = "owner",
                        isSimpleMode = repository.getCachedUserSimpleMode(user.uid)
                    )
                )
            }.onFailure { err ->
                Log.w("AuthViewModel", "Initial profile bootstrap deferred: ${err.message}")
            }
        }

        val pendingCode = _pendingInviteCode.value
        if (pendingCode.isNotBlank() && (existing == null || existing.storeId.isBlank())) {
            _pendingInviteCode.value = ""
            val joinResult = runCatching { inviteJoiner(pendingCode, user) }
                .getOrElse { Result.failure(it) }
            joinResult.onSuccess { joinedStoreId ->
                clearMessages()
                val joinedProfile = runCatching { profileLoader(user.uid) }.getOrNull()
                    ?: UserProfile(
                        id = user.uid,
                        email = user.email,
                        storeId = joinedStoreId,
                        role = "staff",
                        isSimpleMode = false
                    )
                _uiState.value = AuthUiState.AuthenticatedWithStore(
                    user = user,
                    storeId = joinedStoreId,
                    userProfile = joinedProfile.copy(storeId = joinedStoreId)
                )
                observeUserStoreBinding(user)
                return
            }.onFailure { err ->
                setLocalizedError(
                    AuthLocalizedText(
                        en = FirestoreErrorHandler.toUserFriendlyMessage(
                            err,
                            AppLanguage.EN,
                            fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                        ),
                        bn = FirestoreErrorHandler.toUserFriendlyMessage(
                            err,
                            AppLanguage.BN,
                            fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                        )
                    )
                )
                _uiState.value = AuthUiState.AuthenticatedNoStore(user)
                observeUserStoreBinding(user)
                return
            }
        }

        clearMessages()
        if (existing != null && existing.storeId.isNotBlank()) {
            _uiState.value = AuthUiState.AuthenticatedWithStore(
                user = user,
                storeId = existing.storeId,
                userProfile = existing
            )
        } else {
            _uiState.value = AuthUiState.AuthenticatedNoStore(user)
        }
        observeUserStoreBinding(user)
    }

    fun clearMessages() {
        activeErrorLocalized = null
        activeSuccessLocalized = null
        refreshDisplayedMessages()
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        refreshDisplayedMessages(language)
    }

    fun toggleLanguage() {
        repository.toggleLanguage()
        refreshDisplayedMessages(repository.currentLanguage.value)
    }

    fun setPendingInviteCode(code: String) {
        _pendingInviteCode.value = InviteCodeConfig.normalize(code)
    }

    fun clearPendingInviteCode() {
        _pendingInviteCode.value = ""
    }

    fun launchDemoMode(shopType: ShopType? = null) {
        isInDemoMode = true
        profileJob?.cancel()
        repository.configureDemoStore(shopType)
        clearMessages()
        _uiState.value = AuthUiState.DemoMode(storeId = "demo_store")
    }

    fun signInWithGoogle(context: Context) {
        if (!checkOnline(context)) {
            showOfflineError()
            return
        }

        viewModelScope.launch {
            isAuthFlowInProgress = true
            setBusy(AuthBusyAction.GOOGLE)
            clearMessages()
            try {
                val user = authBackend.signInWithGoogle(context)
                handlePostSignIn(user)
            } catch (e: LocalizedAuthException) {
                setLocalizedError(e.localized)
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthViewModel", "Google Sign-In cancelled: ${e.message}")
                setLocalizedError(FirestoreErrorHandler.mapAuthError(e, isGoogleFlow = true))
            } catch (e: GetCredentialException) {
                setLocalizedError(FirestoreErrorHandler.mapAuthError(e, isGoogleFlow = true))
            } catch (e: Exception) {
                setLocalizedError(FirestoreErrorHandler.mapAuthError(e, isGoogleFlow = true))
            } finally {
                isAuthFlowInProgress = false
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        context: Context? = null
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter your email.",
                    bn = "আপনার ইমেইল লিখুন।"
                )
            )
            return
        }
        if (!isValidEmail(cleanEmail)) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter a valid email address.",
                    bn = "সঠিক ইমেইল ঠিকানা লিখুন।"
                )
            )
            return
        }
        if (password.isEmpty()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter your password.",
                    bn = "আপনার পাসওয়ার্ড লিখুন।"
                )
            )
            return
        }
        if (!checkOnline(context)) {
            showOfflineError()
            return
        }

        viewModelScope.launch {
            isAuthFlowInProgress = true
            setBusy(AuthBusyAction.EMAIL)
            clearMessages()
            try {
                val user = authBackend.signInWithEmail(cleanEmail, password)
                handlePostSignIn(user)
            } catch (e: LocalizedAuthException) {
                setLocalizedError(e.localized)
            } catch (e: Exception) {
                setLocalizedError(FirestoreErrorHandler.mapAuthError(e, isGoogleFlow = false))
            } finally {
                isAuthFlowInProgress = false
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        context: Context? = null
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter your email.",
                    bn = "আপনার ইমেইল লিখুন।"
                )
            )
            return
        }
        if (!isValidEmail(cleanEmail)) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter a valid email address.",
                    bn = "সঠিক ইমেইল ঠিকানা লিখুন।"
                )
            )
            return
        }
        if (password.isEmpty()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter a password.",
                    bn = "পাসওয়ার্ড লিখুন।"
                )
            )
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Password is too weak. Use at least 6 characters.",
                    bn = "পাসওয়ার্ড দুর্বল। কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন।"
                )
            )
            return
        }
        if (password != confirmPassword) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Passwords do not match.",
                    bn = "পাসওয়ার্ড দুটি মিলছে না।"
                )
            )
            return
        }
        if (!checkOnline(context)) {
            showOfflineError()
            return
        }

        viewModelScope.launch {
            isAuthFlowInProgress = true
            setBusy(AuthBusyAction.EMAIL)
            clearMessages()
            try {
                val user = authBackend.signUpWithEmail(cleanEmail, password)
                handlePostSignIn(user)
            } catch (e: LocalizedAuthException) {
                setLocalizedError(e.localized)
            } catch (e: Exception) {
                setLocalizedError(
                    FirestoreErrorHandler.mapAuthError(
                        exception = e,
                        isGoogleFlow = false,
                        fallback = AuthLocalizedText(
                            en = "Could not create account. Please try again.",
                            bn = "অ্যাকাউন্ট তৈরি করা যায়নি। আবার চেষ্টা করুন।"
                        )
                    )
                )
            } finally {
                isAuthFlowInProgress = false
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        context: Context? = null
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter your email.",
                    bn = "আপনার ইমেইল লিখুন।"
                )
            )
            return
        }
        if (!isValidEmail(cleanEmail)) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter a valid email address.",
                    bn = "সঠিক ইমেইল ঠিকানা লিখুন।"
                )
            )
            return
        }
        if (!checkOnline(context)) {
            showOfflineError()
            return
        }

        viewModelScope.launch {
            setBusy(AuthBusyAction.RESET)
            clearMessages()
            try {
                authBackend.sendPasswordResetEmail(cleanEmail)
                setLocalizedSuccess(
                    AuthLocalizedText(
                        en = "Password reset link sent. Check your email.",
                        bn = "পাসওয়ার্ড রিসেট লিংক পাঠানো হয়েছে। ইমেইল দেখুন।"
                    )
                )
            } catch (e: LocalizedAuthException) {
                setLocalizedError(e.localized)
            } catch (e: Exception) {
                setLocalizedError(
                    FirestoreErrorHandler.mapAuthError(
                        exception = e,
                        isGoogleFlow = false,
                        fallback = AuthLocalizedText(
                            en = "Could not send reset link. Please try again.",
                            bn = "রিসেট লিংক পাঠানো যায়নি। আবার চেষ্টা করুন।"
                        )
                    )
                )
            } finally {
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun createStore(
        name: String,
        phone: String,
        address: String,
        shopType: ShopType?
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            setLocalizedError(
                AuthLocalizedText(
                    en = "Please enter your shop name.",
                    bn = "দোকানের নাম দিন।"
                )
            )
            return
        }
        viewModelScope.launch {
            setBusy(AuthBusyAction.OTHER)
            clearMessages()
            try {
                val categories = if (shopType != null) {
                    ShopTypeCatalog.startingCategories(shopType)
                } else {
                    emptyList()
                }
                val storeId = repository.createStore(
                    name = cleanName,
                    phone = phone.trim(),
                    address = address.trim(),
                    shopType = shopType?.id ?: "",
                    productCategories = categories
                )
                val currentUser = authBackend.currentUser
                if (currentUser != null) {
                    val profile = runCatching { profileLoader(currentUser.uid) }.getOrNull()
                        ?: UserProfile(
                            id = currentUser.uid,
                            email = currentUser.email,
                            storeId = storeId,
                            role = "owner",
                            isSimpleMode = false
                        )
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = currentUser,
                        storeId = storeId,
                        userProfile = profile.copy(storeId = storeId)
                    )
                }
            } catch (e: Exception) {
                setLocalizedError(
                    AuthLocalizedText(
                        en = FirestoreErrorHandler.toUserFriendlyMessage(
                            e,
                            AppLanguage.EN,
                            fallbackEn = ShopLocale.get("err_create_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_create_store_failed", AppLanguage.BN)
                        ),
                        bn = FirestoreErrorHandler.toUserFriendlyMessage(
                            e,
                            AppLanguage.BN,
                            fallbackEn = ShopLocale.get("err_create_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_create_store_failed", AppLanguage.BN)
                        )
                    )
                )
            } finally {
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun joinStoreWithInvite(inviteCode: String) {
        val cleanCode = InviteCodeConfig.normalize(inviteCode)
        val user = authBackend.currentUser
        if (user == null) {
            setLocalizedError(
                AuthLocalizedText(
                    en = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                    bn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                )
            )
            return
        }
        viewModelScope.launch {
            setBusy(AuthBusyAction.OTHER)
            clearMessages()
            try {
                val result = inviteJoiner(cleanCode, user)
                result.onSuccess { joinedStoreId ->
                    val profile = runCatching { profileLoader(user.uid) }.getOrNull()
                        ?: UserProfile(
                            id = user.uid,
                            email = user.email,
                            storeId = joinedStoreId,
                            role = "staff",
                            isSimpleMode = false
                        )
                    _uiState.value = AuthUiState.AuthenticatedWithStore(
                        user = user,
                        storeId = joinedStoreId,
                        userProfile = profile.copy(storeId = joinedStoreId)
                    )
                }.onFailure { err ->
                    setLocalizedError(
                        AuthLocalizedText(
                            en = FirestoreErrorHandler.toUserFriendlyMessage(
                                err,
                                AppLanguage.EN,
                                fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                                fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                            ),
                            bn = FirestoreErrorHandler.toUserFriendlyMessage(
                                err,
                                AppLanguage.BN,
                                fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                                fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                setLocalizedError(
                    AuthLocalizedText(
                        en = FirestoreErrorHandler.toUserFriendlyMessage(
                            e,
                            AppLanguage.EN,
                            fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                        ),
                        bn = FirestoreErrorHandler.toUserFriendlyMessage(
                            e,
                            AppLanguage.BN,
                            fallbackEn = ShopLocale.get("err_join_store_failed", AppLanguage.EN),
                            fallbackBn = ShopLocale.get("err_join_store_failed", AppLanguage.BN)
                        )
                    )
                )
            } finally {
                setBusy(AuthBusyAction.NONE)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val wasDemo = isInDemoMode || _uiState.value is AuthUiState.DemoMode
                isInDemoMode = false
                isAuthFlowInProgress = false
                boundUserId = null
                _pendingInviteCode.value = ""
                profileJob?.cancel()
                if (wasDemo) {
                    repository.clearDemoLocalPrefs()
                } else {
                    authBackend.signOut(context)
                }
                clearMessages()
                _uiState.value = AuthUiState.Unauthenticated
                if (wasDemo) {
                    onExitDemoCallback?.invoke()
                }
            }
        }
    }

    internal fun dispose() {
        onCleared()
        viewModelScope.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        profileJob?.cancel()
        languageSyncJob?.cancel()
        removeAuthListener?.invoke()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        internal fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

        fun createDemo(
            repository: StoreRepository,
            shopType: ShopType? = null,
            onExitDemo: () -> Unit = {}
        ): AuthViewModel {
            repository.configureDemoStore(shopType)
            return AuthViewModel(
                repository = repository,
                authBackend = DemoAuthBackend(),
                isOnlineProvider = { false },
                profileObserver = { uid -> repository.observeUserProfile(uid) },
                profileLoader = { uid -> repository.getUserProfileOnce(uid) },
                profileSaver = { profile -> repository.saveUserProfile(profile) },
                inviteJoiner = { _, _ ->
                    Result.failure(IllegalStateException("Invites are unavailable in demo mode."))
                },
                initialDemoMode = true,
                onExitDemoCallback = onExitDemo
            )
        }
    }
}
