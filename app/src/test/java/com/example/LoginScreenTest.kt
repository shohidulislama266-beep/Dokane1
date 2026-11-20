package com.example

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.AuthBackend
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthenticatedUser
import com.example.util.AppLanguage
import com.example.util.FirestoreErrorHandler
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("user_mode_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private class FakeAuthBackend(
        initialUser: AuthenticatedUser? = null
    ) : AuthBackend {
        override var currentUser: AuthenticatedUser? = initialUser
        private val listeners = mutableListOf<(AuthenticatedUser?) -> Unit>()

        var googleCalls = 0
        var emailSignInCalls = 0
        var emailSignUpCalls = 0
        var resetCalls = 0
        var lastEmail: String? = null
        var lastPassword: String? = null

        var googleHandler: suspend (Context) -> AuthenticatedUser = {
            AuthenticatedUser(uid = "google_new_user", email = "owner@shop.com", displayName = "Owner")
        }
        var emailSignInHandler: suspend (String, String) -> AuthenticatedUser = { email, _ ->
            AuthenticatedUser(uid = "email_user", email = email, displayName = "Shop Owner")
        }
        var emailSignUpHandler: suspend (String, String) -> AuthenticatedUser = { email, _ ->
            AuthenticatedUser(uid = "new_email_user", email = email, displayName = "")
        }
        var resetHandler: suspend (String) -> Unit = {}

        override fun addAuthStateListener(onUserChanged: (AuthenticatedUser?) -> Unit): () -> Unit {
            listeners += onUserChanged
            return { listeners -= onUserChanged }
        }

        override suspend fun signInWithGoogle(context: Context): AuthenticatedUser {
            googleCalls++
            val user = googleHandler(context)
            currentUser = user
            listeners.forEach { it(user) }
            return user
        }

        override suspend fun signInWithEmail(email: String, password: String): AuthenticatedUser {
            emailSignInCalls++
            lastEmail = email
            lastPassword = password
            val user = emailSignInHandler(email, password)
            currentUser = user
            listeners.forEach { it(user) }
            return user
        }

        override suspend fun signUpWithEmail(email: String, password: String): AuthenticatedUser {
            emailSignUpCalls++
            lastEmail = email
            lastPassword = password
            val user = emailSignUpHandler(email, password)
            currentUser = user
            listeners.forEach { it(user) }
            return user
        }

        override suspend fun sendPasswordResetEmail(email: String) {
            resetCalls++
            lastEmail = email
            resetHandler(email)
        }

        override suspend fun refreshIdToken() = Unit

        override suspend fun signOut(context: Context) {
            currentUser = null
            listeners.forEach { it(null) }
        }
    }

    private fun createTestAuthViewModel(
        backend: FakeAuthBackend = FakeAuthBackend(),
        profiles: MutableStateFlow<Map<String, UserProfile>> = MutableStateFlow(emptyMap()),
        isOnlineProvider: ((Context?) -> Boolean)? = { true },
        inviteJoiner: suspend (String, AuthenticatedUser) -> Result<String> = { _, _ ->
            Result.failure(IllegalArgumentException("Invalid invite"))
        }
    ): Pair<AuthViewModel, StoreRepository> {
        val repository = StoreRepository(db = null, appContext = context)
        val viewModel = AuthViewModel(
            repository = repository,
            authBackend = backend,
            isOnlineProvider = isOnlineProvider,
            profileObserver = { uid -> profiles.map { it[uid] } },
            profileLoader = { uid -> profiles.value[uid] },
            profileSaver = { profile ->
                profiles.value = profiles.value + (profile.id to profile)
            },
            inviteJoiner = inviteJoiner
        )
        return viewModel to repository
    }

    @Test
    fun `layout matches required top-to-bottom order and removes Explore Demo Store`() {
        val (vm, _) = createTestAuthViewModel()

        composeRule.setContent {
            StoreOSTheme {
                Box(modifier = Modifier.size(width = 412.dp, height = 915.dp)) {
                    AuthScreen(authViewModel = vm)
                }
            }
        }
        composeRule.waitForIdle()

        // Explore Demo Store must be completely removed
        assertEquals(0, composeRule.onAllNodesWithTag("explore_demo_store_btn").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Explore Demo Store", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("ডেমো স্টোর", substring = true).fetchSemanticsNodes().size)

        // All required elements exist
        val langBounds = composeRule.onNodeWithTag("login_language_toggle").assertIsDisplayed().getBoundsInRoot()
        val logoBounds = composeRule.onNodeWithTag("login_app_logo").assertIsDisplayed().getBoundsInRoot()
        val nameBounds = composeRule.onNodeWithTag("login_app_name").assertIsDisplayed().getBoundsInRoot()
        val googleBounds = composeRule.onNodeWithTag("google_sign_in_btn").assertIsDisplayed().getBoundsInRoot()
        val dividerBounds = composeRule.onNodeWithTag("login_or_divider").assertIsDisplayed().getBoundsInRoot()
        val emailBounds = composeRule.onNodeWithTag("login_email_input").assertIsDisplayed().getBoundsInRoot()
        val passwordBounds = composeRule.onNodeWithTag("login_password_input").assertIsDisplayed().getBoundsInRoot()
        val signInBounds = composeRule.onNodeWithTag("email_auth_submit_btn").assertIsDisplayed().getBoundsInRoot()
        val createBounds = composeRule.onNodeWithTag("create_account_link").assertIsDisplayed().getBoundsInRoot()
        val forgotBounds = composeRule.onNodeWithTag("forgot_password_link").assertIsDisplayed().getBoundsInRoot()
        val inviteBounds = composeRule.onNodeWithTag("join_with_invite_link").assertIsDisplayed().getBoundsInRoot()

        // Strict top-to-bottom order without overlap
        assertTrue("Language toggle should sit above logo", langBounds.bottom <= logoBounds.top)
        assertTrue("Logo should sit above app name", logoBounds.bottom <= nameBounds.top)
        assertTrue("App name should sit above Google sign-in", nameBounds.bottom <= googleBounds.top)
        assertTrue("Google sign-in should sit above 'or' divider", googleBounds.bottom <= dividerBounds.top)
        assertTrue("'or' divider should sit above email input", dividerBounds.bottom <= emailBounds.top)
        assertTrue("Email input should sit above password input", emailBounds.bottom <= passwordBounds.top)
        assertTrue("Password input should sit above Sign in button", passwordBounds.bottom <= signInBounds.top)
        assertTrue("Sign in button should sit above Create account link", signInBounds.bottom <= createBounds.top)
        assertTrue("Create account and Forgot password must be on separate lines", createBounds.bottom <= forgotBounds.top)
        assertTrue("Forgot password should sit above Join with invite code link", forgotBounds.bottom <= inviteBounds.top)

        // Google hero button is prominently sized and full width
        assertTrue("Google button should be at least as tall as Sign in button", googleBounds.height >= signInBounds.height)
        assertEquals(googleBounds.width.value, signInBounds.width.value, 2f)

        // Links are written in full
        composeRule.onNodeWithTag("create_account_link").assertTextContains("Create account")
        composeRule.onNodeWithTag("forgot_password_link").assertTextContains("Forgot password")
        composeRule.onNodeWithTag("join_with_invite_link").assertTextContains("Join with invite code")
    }

    @Test
    fun `sign in with Google routes new user to Store Setup and existing user straight to Home`() {
        // Case 1: New user (no store yet) -> Store Setup (AuthenticatedNoStore)
        val newUserBackend = FakeAuthBackend().apply {
            googleHandler = { AuthenticatedUser(uid = "uid_new_google", email = "new@shop.com") }
        }
        val profilesNew = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
        val (vmNew, _) = createTestAuthViewModel(backend = newUserBackend, profiles = profilesNew)

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vmNew)
            }
        }
        composeRule.onNodeWithTag("google_sign_in_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(1, newUserBackend.googleCalls)
        assertTrue(
            "New Google user must go to Store Setup (AuthenticatedNoStore), was ${vmNew.uiState.value}",
            vmNew.uiState.value is AuthUiState.AuthenticatedNoStore
        )

        // Case 2: Existing user (already has storeId) -> Home (AuthenticatedWithStore)
        val existingUserBackend = FakeAuthBackend().apply {
            googleHandler = { AuthenticatedUser(uid = "uid_existing_google", email = "owner@shop.com") }
        }
        val existingProfile = UserProfile(
            id = "uid_existing_google",
            email = "owner@shop.com",
            storeId = "store_123",
            role = "owner"
        )
        val profilesExisting = MutableStateFlow(mapOf("uid_existing_google" to existingProfile))
        val (vmExisting, _) = createTestAuthViewModel(backend = existingUserBackend, profiles = profilesExisting)

        vmExisting.signInWithGoogle(context)
        composeRule.waitForIdle()

        assertEquals(1, existingUserBackend.googleCalls)
        val state = vmExisting.uiState.value
        assertTrue("Existing Google user must go straight to Home, was $state", state is AuthUiState.AuthenticatedWithStore)
        assertEquals("store_123", (state as AuthUiState.AuthenticatedWithStore).storeId)
    }

    @Test
    fun `sign in and sign up with email work along with password eye toggle and forgot password`() {
        val backend = FakeAuthBackend().apply {
            emailSignInHandler = { email, _ -> AuthenticatedUser(uid = "uid_owner", email = email) }
            emailSignUpHandler = { email, _ -> AuthenticatedUser(uid = "uid_brand_new", email = email) }
        }
        val profiles = MutableStateFlow(
            mapOf(
                "uid_owner" to UserProfile(
                    id = "uid_owner",
                    email = "owner@storeos.app",
                    storeId = "store_home",
                    role = "owner"
                )
            )
        )
        val (vm, _) = createTestAuthViewModel(backend = backend, profiles = profiles)

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vm)
            }
        }

        // Toggle password visibility eye on and off
        composeRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
        composeRule.onNodeWithTag("login_password_visibility_btn").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        composeRule.onNodeWithTag("login_password_visibility_btn").performClick()
        composeRule.onNodeWithContentDescription("Show password").assertIsDisplayed()

        // Forgot password flow
        composeRule.onNodeWithTag("forgot_password_link").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_email_input").performScrollTo().performTextInput("owner@storeos.app")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, backend.resetCalls)
        composeRule.onNodeWithTag("login_success_banner").assertIsDisplayed()
            .assertTextContains("Password reset link sent", substring = true)

        // Back to Sign In and sign in with email -> existing user goes straight to Home
        composeRule.onNodeWithTag("back_to_sign_in_link").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("Secret123")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, backend.emailSignInCalls)
        assertEquals("owner@storeos.app", backend.lastEmail)
        val signedInState = vm.uiState.value
        assertTrue(signedInState is AuthUiState.AuthenticatedWithStore)
        assertEquals("store_home", (signedInState as AuthUiState.AuthenticatedWithStore).storeId)

        // Sign out and test Create account flow -> new user goes to Store Setup
        vm.signOut(context)
        composeRule.waitForIdle()
        assertTrue(vm.uiState.value is AuthUiState.Unauthenticated)

        composeRule.onNodeWithTag("create_account_link").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_confirm_password_input").performScrollTo().performTextInput("Secret123")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, backend.emailSignUpCalls)
        assertTrue(
            "New email sign-up user must go to Store Setup, was ${vm.uiState.value}",
            vm.uiState.value is AuthUiState.AuthenticatedNoStore
        )
    }

    @Test
    fun `wrong password and all auth errors show plain short messages in EN and BN without raw Firebase text`() {
        val backend = FakeAuthBackend().apply {
            emailSignInHandler = { _, _ ->
                throw FirebaseAuthInvalidCredentialsException(
                    "ERROR_WRONG_PASSWORD",
                    "com.google.firebase.auth.FirebaseAuthInvalidCredentialsException: The password is invalid or the user does not have a password."
                )
            }
        }
        val (vm, _) = createTestAuthViewModel(backend = backend)

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vm)
            }
        }

        // Trigger wrong password through the real UI
        composeRule.onNodeWithTag("login_email_input").performScrollTo().performTextInput("shop@example.com")
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("wrongpass")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        val enWrongPass = vm.errorMessage.value
        assertNotNull(enWrongPass)
        assertEquals("Wrong password. Please try again.", enWrongPass)
        assertFalse(FirestoreErrorHandler.containsRawTechnicalDetails(enWrongPass))
        composeRule.onNodeWithTag("login_error_banner").assertIsDisplayed()
            .assertTextContains("Wrong password", substring = true)

        // Switch language while error is visible -> error updates immediately to Bangla
        composeRule.onNodeWithTag("lang_option_bn").performScrollTo().performClick()
        composeRule.waitForIdle()
        val bnWrongPass = vm.errorMessage.value
        assertEquals("পাসওয়ার্ড ভুল হয়েছে। আবার চেষ্টা করুন।", bnWrongPass)
        assertFalse(FirestoreErrorHandler.containsRawTechnicalDetails(bnWrongPass))
        composeRule.onNodeWithTag("login_error_banner")
            .assertTextContains("পাসওয়ার্ড ভুল হয়েছে", substring = true)

        // Verify every required error type maps cleanly in both EN and BN with zero raw text
        val cases = listOf(
            FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "FirebaseAuthException raw") to false,
            FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "FirebaseAuthInvalidUserException raw") to false,
            FirebaseNetworkException("ERROR_NETWORK_REQUEST_FAILED: io.grpc UNAVAILABLE") to false,
            GetCredentialCancellationException("GetCredentialCancellationException: cancelled by user") to true,
            GetCredentialUnknownException("com.google.android.gms.common.api.ApiException: 16") to true,
            FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Weak password raw", "6 chars") to false,
            FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "FirebaseAuthUserCollisionException raw") to false
        )

        cases.forEach { (exception, isGoogle) ->
            val localized = FirestoreErrorHandler.mapAuthError(exception, isGoogleFlow = isGoogle)
            assertFalse("EN leaked raw details: ${localized.en}", FirestoreErrorHandler.containsRawTechnicalDetails(localized.en))
            assertFalse("BN leaked raw details: ${localized.bn}", FirestoreErrorHandler.containsRawTechnicalDetails(localized.bn))
            assertTrue("EN message should be concise: ${localized.en}", localized.en.length in 10..80)
            assertTrue("BN message should be concise: ${localized.bn}", localized.bn.length in 10..80)
        }
    }

    @Test
    fun `language switch EN and Bangla saves preference and applies immediately to all labels`() {
        val (vm, _) = createTestAuthViewModel()

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vm)
            }
        }

        // Starts in EN
        composeRule.onNodeWithTag("login_app_name").assertTextContains("StoreOS")
        composeRule.onNodeWithTag("google_sign_in_btn").assertTextContains("Sign in with Google")
        composeRule.onNodeWithTag("email_auth_submit_btn").assertTextContains("Sign in")
        composeRule.onNodeWithTag("create_account_link").assertTextContains("Create account")
        composeRule.onNodeWithTag("forgot_password_link").assertTextContains("Forgot password")
        composeRule.onNodeWithTag("join_with_invite_link").assertTextContains("Join with invite code")

        // Tap বাংলা -> applies immediately and persists
        composeRule.onNodeWithTag("lang_option_bn").performClick()
        composeRule.waitForIdle()

        assertEquals(AppLanguage.BN, vm.currentLanguage.value)
        val savedCode = context.getSharedPreferences("user_mode_prefs", Context.MODE_PRIVATE)
            .getString("app_language", null)
        assertEquals("bn", savedCode)

        composeRule.onNodeWithTag("login_app_name").assertTextContains("স্টোর ওএস")
        composeRule.onNodeWithTag("google_sign_in_btn").assertTextContains("গুগল দিয়ে সাইন ইন করুন")
        composeRule.onNodeWithTag("login_or_divider").assertTextContains("অথবা")
        composeRule.onNodeWithTag("email_auth_submit_btn").assertTextContains("সাইন ইন")
        composeRule.onNodeWithTag("create_account_link").assertTextContains("অ্যাকাউন্ট তৈরি করুন")
        composeRule.onNodeWithTag("forgot_password_link").assertTextContains("পাসওয়ার্ড ভুলে গেছেন?")
        composeRule.onNodeWithTag("join_with_invite_link").assertTextContains("আমন্ত্রণ কোড দিয়ে যোগ দিন")

        // Recreate repository + ViewModel to confirm language persists across restart
        val (restartedVm, _) = createTestAuthViewModel()
        assertEquals(AppLanguage.BN, restartedVm.currentLanguage.value)

        // Switch back to EN
        composeRule.onNodeWithTag("lang_option_en").performClick()
        composeRule.waitForIdle()
        assertEquals(AppLanguage.EN, vm.currentLanguage.value)
        assertEquals(
            "en",
            context.getSharedPreferences("user_mode_prefs", Context.MODE_PRIVATE).getString("app_language", null)
        )
        composeRule.onNodeWithTag("google_sign_in_btn").assertTextContains("Sign in with Google")
    }

    @Test
    fun `turning off internet shows plain offline error for Google and Email sign-in`() {
        // Turn off device network via Robolectric ConnectivityManager shadow (no fake online provider!)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        Shadows.shadowOf(cm).clearAllNetworks()

        val backend = FakeAuthBackend()
        val (vm, _) = createTestAuthViewModel(
            backend = backend,
            isOnlineProvider = null // exercises real ConnectivityManager path
        )

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vm)
            }
        }

        // Try Google sign-in offline -> plain EN message, backend never called
        composeRule.onNodeWithTag("google_sign_in_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, backend.googleCalls)
        composeRule.onNodeWithTag("login_error_banner").assertIsDisplayed()
            .assertTextContains("No internet connection", substring = true)

        // Switch to Bangla and try Email sign-in offline -> plain BN message, backend never called
        composeRule.onNodeWithTag("lang_option_bn").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_email_input").performScrollTo().performTextInput("owner@shop.com")
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("password123")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(0, backend.emailSignInCalls)
        composeRule.onNodeWithTag("login_error_banner").assertIsDisplayed()
            .assertTextContains("ইন্টারনেট সংযোগ নেই", substring = true)
    }

    @Test
    fun `signed-in user stays signed in across app restarts`() {
        val persistedUser = AuthenticatedUser(uid = "uid_persisted", email = "persisted@shop.com")
        val profiles = MutableStateFlow(
            mapOf(
                "uid_persisted" to UserProfile(
                    id = "uid_persisted",
                    email = "persisted@shop.com",
                    storeId = "store_persisted",
                    role = "owner"
                )
            )
        )

        // Simulate cold restart with persisted FirebaseAuth session
        val restartedBackend = FakeAuthBackend(initialUser = persistedUser)
        val (restartedVm, _) = createTestAuthViewModel(backend = restartedBackend, profiles = profiles)
        composeRule.waitForIdle()

        val state = restartedVm.uiState.value
        assertTrue("Persisted user with store must stay signed in and go to Home, was $state", state is AuthUiState.AuthenticatedWithStore)
        assertEquals("store_persisted", (state as AuthUiState.AuthenticatedWithStore).storeId)

        // Simulate cold restart for signed-in user who has not set up a store yet
        val newPersistedBackend = FakeAuthBackend(initialUser = AuthenticatedUser(uid = "uid_no_store", email = "new@shop.com"))
        val (restartedNewVm, _) = createTestAuthViewModel(backend = newPersistedBackend, profiles = MutableStateFlow(emptyMap()))
        composeRule.waitForIdle()

        assertTrue(
            "Persisted new user must stay signed in and resume at Store Setup, was ${restartedNewVm.uiState.value}",
            restartedNewVm.uiState.value is AuthUiState.AuthenticatedNoStore
        )
    }

    @Test
    fun `join with invite code arms code and auto-joins store after sign-in`() {
        val backend = FakeAuthBackend().apply {
            emailSignInHandler = { email, _ -> AuthenticatedUser(uid = "uid_staff", email = email) }
        }
        val profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
        var joinedCodeReceived: String? = null
        val (vm, _) = createTestAuthViewModel(
            backend = backend,
            profiles = profiles,
            inviteJoiner = { code, user ->
                joinedCodeReceived = code
                profiles.value = profiles.value + (
                    user.uid to UserProfile(
                        id = user.uid,
                        email = user.email,
                        storeId = "store_invited",
                        role = "staff"
                    )
                )
                Result.success("store_invited")
            }
        )

        composeRule.setContent {
            StoreOSTheme {
                AuthScreen(authViewModel = vm)
            }
        }

        composeRule.onNodeWithTag("join_with_invite_link").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_invite_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("login_invite_code_input").performTextInput("AB12CD34")
        composeRule.onNodeWithTag("invite_continue_email_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pending_invite_chip").performScrollTo().assertIsDisplayed()
            .assertTextContains("AB12CD34", substring = true)

        composeRule.onNodeWithTag("login_email_input").performScrollTo().performTextInput("staff@shop.com")
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("StaffPass1")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("AB12CD34", joinedCodeReceived)
        val state = vm.uiState.value
        assertTrue(state is AuthUiState.AuthenticatedWithStore)
        assertEquals("store_invited", (state as AuthUiState.AuthenticatedWithStore).storeId)
    }

    @Test
    fun `large font size does not break layout overlap or truncate any text`() {
        val backend = FakeAuthBackend().apply {
            emailSignInHandler = { _, _ ->
                throw FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "raw")
            }
        }
        val (vm, _) = createTestAuthViewModel(backend = backend)

        composeRule.setContent {
            StoreOSTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.85f)) {
                    Box(modifier = Modifier.size(width = 320.dp, height = 640.dp)) {
                        AuthScreen(authViewModel = vm)
                    }
                }
            }
        }

        // Trigger an active error banner too so we verify errors wrap cleanly at large font scale
        composeRule.onNodeWithTag("login_email_input").performScrollTo().performTextInput("shop@example.com")
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("bad")
        composeRule.onNodeWithTag("email_auth_submit_btn").performScrollTo().performClick()
        composeRule.waitForIdle()

        val orderedTags = listOf(
            "login_language_toggle",
            "login_app_logo",
            "login_app_name",
            "google_sign_in_btn",
            "login_or_divider",
            "login_email_input",
            "login_password_input",
            "login_error_banner",
            "email_auth_submit_btn",
            "create_account_link",
            "forgot_password_link",
            "join_with_invite_link"
        )

        fun assertAllReachableAndNoTruncation() {
            // 1. Every element is reachable via scroll and rendered with positive bounds in compact 320x640 viewport
            orderedTags.forEach { tag ->
                val node = composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                val bounds = node.getBoundsInRoot()
                assertTrue("$tag width must be > 0", bounds.width.value > 0f)
                assertTrue("$tag height must be > 0", bounds.height.value > 0f)
            }

            // 2. Capture all node rects in a SINGLE frame (no scrolling in between) to assert zero vertical overlap
            var previousBottom = Float.NEGATIVE_INFINITY
            orderedTags.forEach { tag ->
                val semanticsNode = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                val top = semanticsNode.positionInRoot.y
                val bottom = top + semanticsNode.size.height
                assertTrue(
                    "Element $tag overlapped previous element at 1.85x fontScale (prevBottom=$previousBottom, top=$top)",
                    top >= previousBottom - 1f
                )
                previousBottom = bottom
            }

            // 3. Ensure no rendered text anywhere contains ellipsis or overflows visually
            val allNodes = collectAllNodes(composeRule.onRoot().fetchSemanticsNode())
            val allTexts = mutableListOf<String>()
            allNodes.forEach { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.forEach { annotated ->
                    if (annotated.text.isNotBlank()) {
                        allTexts += annotated.text
                        assertFalse("Truncated text with '...' found: ${annotated.text}", annotated.text.contains("..."))
                        assertFalse("Truncated text with '…' found: ${annotated.text}", annotated.text.contains("…"))
                    }
                }
                val getLayoutAction = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult)
                if (getLayoutAction != null && !node.config.contains(SemanticsProperties.EditableText)) {
                    val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    getLayoutAction.action?.invoke(results)
                    results.forEach { layoutResult ->
                        assertFalse(
                            "Vertical text clipping detected for '${layoutResult.layoutInput.text}' " +
                                "(size=${layoutResult.size}, mpH=${layoutResult.multiParagraph.height}, lines=${layoutResult.lineCount})",
                            layoutResult.didOverflowHeight
                        )
                        for (line in 0 until layoutResult.lineCount) {
                            assertFalse(
                                "Ellipsized line detected for '${layoutResult.layoutInput.text}' at line $line",
                                layoutResult.isLineEllipsized(line)
                            )
                        }
                    }
                }
            }
            assertTrue("Expected non-empty text labels", allTexts.isNotEmpty())
        }

        // Verify in English at 1.85x font scale on 320dp narrow screen
        assertAllReachableAndNoTruncation()

        // Switch to Bangla at 1.85x font scale on 320dp narrow screen and re-verify
        composeRule.onNodeWithTag("lang_option_bn").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertAllReachableAndNoTruncation()
    }

    private fun collectAllNodes(node: SemanticsNode): List<SemanticsNode> {
        val result = mutableListOf(node)
        node.children.forEach { child ->
            result += collectAllNodes(child)
        }
        return result
    }
}
