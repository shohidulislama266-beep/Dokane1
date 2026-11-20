package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ShopType
import com.example.ui.components.DemoShopTypePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGreenContainer
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthBusyAction
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import kotlinx.coroutines.launch

private enum class LoginMode {
    SIGN_IN,
    CREATE_ACCOUNT,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onEnterDemo: ((ShopType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val isBusy by authViewModel.isBusy.collectAsState()
    val busyAction by authViewModel.busyAction.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val successMessage by authViewModel.successMessage.collectAsState()
    val currentLanguage by authViewModel.currentLanguage.collectAsState()
    val pendingInviteCode by authViewModel.pendingInviteCode.collectAsState()

    val isBn = currentLanguage == AppLanguage.BN

    var mode by rememberSaveable { mutableStateOf(LoginMode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    var showDemoTypeDialog by rememberSaveable { mutableStateOf(false) }

    val submitBringIntoView = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()

    BackHandler(enabled = mode != LoginMode.SIGN_IN) {
        mode = LoginMode.SIGN_IN
        password = ""
        confirmPassword = ""
        authViewModel.clearMessages()
    }

    fun submitEmailForm() {
        focusManager.clearFocus()
        when (mode) {
            LoginMode.SIGN_IN -> authViewModel.signInWithEmail(email, password, context)
            LoginMode.CREATE_ACCOUNT -> authViewModel.signUpWithEmail(email, password, confirmPassword, context)
            LoginMode.FORGOT_PASSWORD -> authViewModel.sendPasswordResetEmail(email, context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VividGreen.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val minContentHeight = (maxHeight - 24.dp).coerceAtLeast(0.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minContentHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top safe-area corner: EN | বাংলা switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LoginLanguageSwitch(
                            currentLanguage = currentLanguage,
                            onSelectLanguage = { authViewModel.setLanguage(it) }
                        )
                    }

                    // Main Login Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. App Logo & Name
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            VividGreen.copy(alpha = 0.24f),
                                            Color(0xFF1A2218)
                                        )
                                    )
                                )
                                .testTag("login_app_logo"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = if (isBn) "স্টোর ওএস লোগো" else "StoreOS logo",
                                        tint = VividGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isBn) "স্টোর ওএস" else "StoreOS",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_app_name")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dark Glass Auth Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "auth_card",
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (pendingInviteCode.isNotBlank()) {
                                    PendingInviteBanner(
                                        code = pendingInviteCode,
                                        isBn = isBn,
                                        onClear = { authViewModel.clearPendingInviteCode() }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // 2. Big "Sign in with Google" Hero Button
                                GoogleHeroSignInButton(
                                    text = if (isBn) "গুগল দিয়ে সাইন ইন করুন" else "Sign in with Google",
                                    isLoading = busyAction == AuthBusyAction.GOOGLE,
                                    enabled = !isBusy,
                                    onClick = {
                                        focusManager.clearFocus()
                                        authViewModel.clearMessages()
                                        authViewModel.signInWithGoogle(context)
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // 3. Divider "or"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {}
                                        .testTag("login_or_divider"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        thickness = 1.dp,
                                        color = GlassBorder
                                    )
                                    Text(
                                        text = if (isBn) "অথবা" else "or",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        thickness = 1.dp,
                                        color = GlassBorder
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (mode != LoginMode.SIGN_IN) {
                                    Text(
                                        text = when (mode) {
                                            LoginMode.CREATE_ACCOUNT -> if (isBn) "নতুন অ্যাকাউন্ট তৈরি করুন" else "Create account"
                                            LoginMode.FORGOT_PASSWORD -> if (isBn) "পাসওয়ার্ড রিসেট করুন" else "Reset password"
                                            LoginMode.SIGN_IN -> ""
                                        },
                                        color = TextPrimary,
                                        fontSize = 17.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_mode_heading")
                                    )
                                    if (mode == LoginMode.FORGOT_PASSWORD) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isBn) {
                                                "রিসেট লিংক পেতে আপনার ইমেইল লিখুন।"
                                            } else {
                                                "Enter your email to get a reset link."
                                            },
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // 4. Email field
                                LoginInputField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        if (errorMessage != null || successMessage != null) {
                                            authViewModel.clearMessages()
                                        }
                                    },
                                    label = if (isBn) "ইমেইল" else "Email",
                                    placeholder = "name@mail.com",
                                    leadingIcon = Icons.Outlined.Email,
                                    enabled = !isBusy,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = if (mode == LoginMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                        onDone = { submitEmailForm() }
                                    ),
                                    onFocused = {
                                        coroutineScope.launch { submitBringIntoView.bringIntoView() }
                                    },
                                    testTag = "login_email_input"
                                )

                                // 5. Password field with show/hide eye
                                if (mode != LoginMode.FORGOT_PASSWORD) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LoginPasswordField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            if (errorMessage != null || successMessage != null) {
                                                authViewModel.clearMessages()
                                            }
                                        },
                                        label = if (isBn) "পাসওয়ার্ড" else "Password",
                                        isBn = isBn,
                                        visible = passwordVisible,
                                        onToggleVisibility = { passwordVisible = !passwordVisible },
                                        enabled = !isBusy,
                                        imeAction = if (mode == LoginMode.CREATE_ACCOUNT) ImeAction.Next else ImeAction.Done,
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                            onDone = { submitEmailForm() }
                                        ),
                                        onFocused = {
                                            coroutineScope.launch { submitBringIntoView.bringIntoView() }
                                        },
                                        fieldTestTag = "login_password_input",
                                        eyeTestTag = "login_password_visibility_btn"
                                    )
                                }

                                // Confirm password field in Create Account mode
                                if (mode == LoginMode.CREATE_ACCOUNT) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LoginPasswordField(
                                        value = confirmPassword,
                                        onValueChange = {
                                            confirmPassword = it
                                            if (errorMessage != null || successMessage != null) {
                                                authViewModel.clearMessages()
                                            }
                                        },
                                        label = if (isBn) "পাসওয়ার্ড নিশ্চিত করুন" else "Confirm password",
                                        isBn = isBn,
                                        visible = confirmPasswordVisible,
                                        onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                        enabled = !isBusy,
                                        imeAction = ImeAction.Done,
                                        keyboardActions = KeyboardActions(
                                            onDone = { submitEmailForm() }
                                        ),
                                        onFocused = {
                                            coroutineScope.launch { submitBringIntoView.bringIntoView() }
                                        },
                                        fieldTestTag = "login_confirm_password_input",
                                        eyeTestTag = "login_confirm_password_visibility_btn"
                                    )
                                }

                                // Plain localized error banner
                                if (!errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = AlertRedContainer,
                                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics(mergeDescendants = true) {}
                                            .testTag("login_error_banner")
                                    ) {
                                        Text(
                                            text = errorMessage.orEmpty(),
                                            color = Color(0xFFFFB4AB),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                // Plain localized success banner
                                if (!successMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkGreenContainer,
                                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics(mergeDescendants = true) {}
                                            .testTag("login_success_banner")
                                    ) {
                                        Text(
                                            text = successMessage.orEmpty(),
                                            color = VividGreen,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 6. Sign in / Create account / Send reset link button
                                val emailSubmitLabel = when (mode) {
                                    LoginMode.SIGN_IN -> if (isBn) "সাইন ইন" else "Sign in"
                                    LoginMode.CREATE_ACCOUNT -> if (isBn) "অ্যাকাউন্ট তৈরি করুন" else "Create account"
                                    LoginMode.FORGOT_PASSWORD -> if (isBn) "রিসেট লিংক পাঠান" else "Send reset link"
                                }
                                val isEmailSubmitLoading =
                                    busyAction == AuthBusyAction.EMAIL || busyAction == AuthBusyAction.RESET

                                OutlinedButton(
                                    onClick = { submitEmailForm() },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF252D24),
                                        contentColor = TextPrimary,
                                        disabledContainerColor = Color(0xFF1C201C),
                                        disabledContentColor = TextMuted
                                    ),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 52.dp)
                                        .bringIntoViewRequester(submitBringIntoView)
                                        .testTag("email_auth_submit_btn")
                                ) {
                                    if (isEmailSubmitLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = VividGreen,
                                            strokeWidth = 2.4.dp
                                        )
                                    } else {
                                        Text(
                                            text = emailSubmitLabel,
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 7. Links: "Create account" and "Forgot password" — written in full on one line each
                                if (mode == LoginMode.SIGN_IN) {
                                    FullLineLoginLink(
                                        text = if (isBn) "অ্যাকাউন্ট তৈরি করুন" else "Create account",
                                        color = VividGreen,
                                        enabled = !isBusy,
                                        testTag = "create_account_link",
                                        onClick = {
                                            authViewModel.clearMessages()
                                            confirmPassword = ""
                                            mode = LoginMode.CREATE_ACCOUNT
                                        }
                                    )
                                    FullLineLoginLink(
                                        text = if (isBn) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot password",
                                        color = TextSecondary,
                                        enabled = !isBusy,
                                        testTag = "forgot_password_link",
                                        onClick = {
                                            authViewModel.clearMessages()
                                            mode = LoginMode.FORGOT_PASSWORD
                                        }
                                    )
                                } else {
                                    FullLineLoginLink(
                                        text = if (isBn) "সাইন ইনে ফিরে যান" else "Back to sign in",
                                        color = VividGreen,
                                        enabled = !isBusy,
                                        testTag = "back_to_sign_in_link",
                                        onClick = {
                                            authViewModel.clearMessages()
                                            confirmPassword = ""
                                            mode = LoginMode.SIGN_IN
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 8. Small "Join with invite code" link + "Try Sample Shop" at the bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(
                                    enabled = !isBusy,
                                    role = Role.Button,
                                    onClick = {
                                        authViewModel.clearMessages()
                                        showInviteDialog = true
                                    }
                                )
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("join_with_invite_link"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "আমন্ত্রণ কোড দিয়ে যোগ দিন" else "Join with invite code",
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(
                                    enabled = !isBusy,
                                    role = Role.Button,
                                    onClick = {
                                        authViewModel.clearMessages()
                                        showDemoTypeDialog = true
                                    }
                                )
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("demo_mode_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = VividGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ShopLocale.get("try_sample_shop_btn", currentLanguage),
                                color = VividGreen,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        JoinWithInviteDialog(
            initialCode = pendingInviteCode,
            isBn = isBn,
            onContinueWithGoogle = { cleanCode ->
                authViewModel.setPendingInviteCode(cleanCode)
                showInviteDialog = false
                authViewModel.clearMessages()
                authViewModel.signInWithGoogle(context)
            },
            onContinueWithEmail = { cleanCode ->
                authViewModel.setPendingInviteCode(cleanCode)
                showInviteDialog = false
                authViewModel.clearMessages()
                if (mode == LoginMode.FORGOT_PASSWORD) {
                    mode = LoginMode.SIGN_IN
                }
            },
            onDismiss = { showInviteDialog = false }
        )
    }

    if (showDemoTypeDialog) {
        DemoShopTypePickerDialog(
            currentLanguage = currentLanguage,
            onSelectType = { shopType ->
                showDemoTypeDialog = false
                if (onEnterDemo != null) {
                    onEnterDemo(shopType)
                } else {
                    authViewModel.launchDemoMode(shopType)
                }
            },
            onDismiss = { showDemoTypeDialog = false }
        )
    }
}
