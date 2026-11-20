package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InviteCodeConfig
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkGreenContainer
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage

@Composable
internal fun LoginLanguageSwitch(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1F1A),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.testTag("login_language_toggle")
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isEn = currentLanguage == AppLanguage.EN
            LanguageSegmentButton(
                label = "EN",
                selected = isEn,
                testTag = "lang_option_en",
                onClick = { onSelectLanguage(AppLanguage.EN) }
            )
            Text(
                text = "|",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            LanguageSegmentButton(
                label = "বাংলা",
                selected = !isEn,
                testTag = "lang_option_bn",
                onClick = { onSelectLanguage(AppLanguage.BN) }
            )
        }
    }
}

@Composable
private fun LanguageSegmentButton(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) VividGreen else Color.Transparent,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { this.selected = selected }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) DarkBackground else TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun GoogleHeroSignInButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VividGreen,
            contentColor = DarkBackground,
            disabledContainerColor = VividGreen.copy(alpha = 0.45f),
            disabledContentColor = DarkBackground.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .testTag("google_sign_in_btn")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = DarkBackground,
                strokeWidth = 2.8.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                GoogleGMark(modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoogleGMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokeWidth = size.minDimension * 0.2f
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -142f,
                sweepAngle = 102f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 142f,
                sweepAngle = 78f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 42f,
                sweepAngle = 102f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -6f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.5f, size.height * 0.5f),
                end = Offset(size.width - strokeWidth * 0.1f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
internal fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isBn: Boolean,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    onFocused: () -> Unit,
    fieldTestTag: String,
    eyeTestTag: String
) {
    LoginInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = "••••••",
        leadingIcon = Icons.Outlined.Lock,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag(eyeTestTag)
            ) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) {
                        if (isBn) "পাসওয়ার্ড লুকান" else "Hide password"
                    } else {
                        if (isBn) "পাসওয়ার্ড দেখুন" else "Show password"
                    },
                    tint = TextSecondary
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        onFocused = onFocused,
        testTag = fieldTestTag
    )
}

@Composable
internal fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    enabled: Boolean,
    testTag: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocused: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextMuted,
                    fontSize = 14.5.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VividGreen,
                unfocusedBorderColor = GlassBorderLight,
                focusedContainerColor = Color(0xFF141814),
                unfocusedContainerColor = Color(0xFF141814),
                disabledContainerColor = Color(0xFF121512),
                disabledBorderColor = GlassBorderLight,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextMuted,
                cursorColor = VividGreen
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent { state ->
                    if (state.isFocused) onFocused()
                }
                .testTag(testTag)
        )
    }
}

@Composable
internal fun FullLineLoginLink(
    text: String,
    color: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) color else TextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun PendingInviteBanner(
    code: String,
    isBn: Boolean,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkGreenContainer,
        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("pending_invite_chip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.VpnKey,
                contentDescription = null,
                tint = VividGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBn) "আমন্ত্রণ কোড: $code" else "Invite code: $code",
                color = VividGreen,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("clear_pending_invite_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isBn) "আমন্ত্রণ কোড সরান" else "Remove invite code",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun JoinWithInviteDialog(
    initialCode: String,
    isBn: Boolean,
    onContinueWithGoogle: (String) -> Unit,
    onContinueWithEmail: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawCode by rememberSaveable { mutableStateOf(initialCode) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val normalized = InviteCodeConfig.normalize(rawCode)

    fun validateAndRun(onValid: (String) -> Unit) {
        if (normalized.length < InviteCodeConfig.CODE_LENGTH) {
            validationError = if (isBn) {
                "৮ অক্ষরের আমন্ত্রণ কোড লিখুন।"
            } else {
                "Enter the 8-character invite code."
            }
            return
        }
        validationError = null
        onValid(normalized)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                testTag = "login_invite_dialog",
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isBn) "আমন্ত্রণ কোড দিয়ে যোগ দিন" else "Join with invite code",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBn) {
                            "দোকানের মালিকের দেওয়া ৮ অক্ষরের কোডটি লিখুন, এরপর সাইন ইন করে যোগ দিন।"
                        } else {
                            "Enter the 8-character code from your store owner, then sign in to join."
                        },
                        color = TextSecondary,
                        fontSize = 13.5.sp
                    )
                    LoginInputField(
                        value = rawCode,
                        onValueChange = { input ->
                            rawCode = input.uppercase()
                                .filter { it.isLetterOrDigit() }
                                .take(InviteCodeConfig.CODE_LENGTH)
                            validationError = null
                        },
                        label = if (isBn) "আমন্ত্রণ কোড" else "Invite code",
                        placeholder = InviteCodeConfig.EXAMPLE_CODE,
                        leadingIcon = Icons.Outlined.VpnKey,
                        enabled = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { validateAndRun(onContinueWithEmail) }
                        ),
                        testTag = "login_invite_code_input"
                    )
                    if (validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            color = Color(0xFFFFB4AB),
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("invite_dialog_error")
                        )
                    }
                    Button(
                        onClick = { validateAndRun(onContinueWithGoogle) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VividGreen,
                            contentColor = DarkBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("invite_continue_google_btn")
                    ) {
                        Text(
                            text = if (isBn) "গুগল দিয়ে চালিয়ে যান" else "Continue with Google",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    OutlinedButton(
                        onClick = { validateAndRun(onContinueWithEmail) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("invite_continue_email_btn")
                    ) {
                        Text(
                            text = if (isBn) "ইমেইল দিয়ে চালিয়ে যান" else "Continue with email",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("invite_cancel_btn")
                        ) {
                            Text(
                                text = if (isBn) "বাতিল" else "Cancel",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
