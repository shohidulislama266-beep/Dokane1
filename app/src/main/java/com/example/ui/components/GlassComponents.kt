package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage

@Stable
class FullScreenOverlayHost {
    internal val activeOrder = mutableStateListOf<Any>()
    internal val entries = mutableStateMapOf<Any, @Composable () -> Unit>()

    fun attach(id: Any, content: @Composable () -> Unit) {
        entries[id] = content
        if (!activeOrder.contains(id)) {
            activeOrder.add(id)
        }
    }

    fun update(id: Any, content: @Composable () -> Unit) {
        entries[id] = content
        if (!activeOrder.contains(id)) {
            activeOrder.add(id)
        }
    }

    fun detach(id: Any) {
        activeOrder.remove(id)
        entries.remove(id)
    }
}

val LocalFullScreenOverlayHost = staticCompositionLocalOf<FullScreenOverlayHost?> { null }

@Composable
fun ProvideFullScreenOverlayHost(
    content: @Composable () -> Unit
) {
    val existing = LocalFullScreenOverlayHost.current
    if (existing != null) {
        content()
        return
    }
    val host = remember { FullScreenOverlayHost() }
    CompositionLocalProvider(LocalFullScreenOverlayHost provides host) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            host.activeOrder.forEach { id ->
                val overlayContent = host.entries[id]
                if (overlayContent != null) {
                    key(id) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(id) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent().changes.forEach { it.consume() }
                                        }
                                    }
                                }
                        ) {
                            overlayContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenOverlayEntry(
    id: Any,
    content: @Composable () -> Unit
) {
    val host = LocalFullScreenOverlayHost.current ?: return
    SideEffect {
        host.update(id, content)
    }
    DisposableEffect(host, id) {
        host.attach(id, content)
        onDispose {
            host.detach(id)
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Composable
fun rememberHostActivitySafeWindowInsets(): WindowInsets {
    val context = LocalContext.current
    val hostDecorView = remember(context) {
        context.findHostActivity()?.window?.decorView
    }
    var rawInsets by remember(hostDecorView) {
        mutableStateOf(hostDecorView?.let { ViewCompat.getRootWindowInsets(it) })
    }
    SideEffect {
        if (hostDecorView != null) {
            val latest = ViewCompat.getRootWindowInsets(hostDecorView)
            if (latest != rawInsets) {
                rawInsets = latest
            }
        }
    }
    DisposableEffect(hostDecorView) {
        if (hostDecorView == null) {
            onDispose {}
        } else {
            val refresh = {
                rawInsets = ViewCompat.getRootWindowInsets(hostDecorView)
            }
            refresh()
            val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                refresh()
            }
            val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                refresh()
            }
            hostDecorView.addOnLayoutChangeListener(layoutListener)
            hostDecorView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
            onDispose {
                hostDecorView.removeOnLayoutChangeListener(layoutListener)
                if (hostDecorView.viewTreeObserver.isAlive) {
                    hostDecorView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
                }
            }
        }
    }
    val compat = rawInsets ?: return WindowInsets(0, 0, 0, 0)
    val typeMask = WindowInsetsCompat.Type.systemBars() or
        WindowInsetsCompat.Type.displayCutout() or
        WindowInsetsCompat.Type.tappableElement()
    val visibleInsets = compat.getInsets(typeMask)
    val imeInsets = compat.getInsets(WindowInsetsCompat.Type.ime())
    return WindowInsets(
        left = maxOf(visibleInsets.left, imeInsets.left),
        top = visibleInsets.top,
        right = maxOf(visibleInsets.right, imeInsets.right),
        bottom = maxOf(visibleInsets.bottom, imeInsets.bottom)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun safeFullScreenWindowInsets(): WindowInsets {
    val hostActivitySafeInsets = rememberHostActivitySafeWindowInsets()
    return WindowInsets.statusBars
        .union(WindowInsets.navigationBars)
        .union(WindowInsets.tappableElement)
        .union(WindowInsets.displayCutout)
        .union(WindowInsets.ime)
        .union(hostActivitySafeInsets)
}

fun edgeToEdgeDialogProperties(usePlatformDefaultWidth: Boolean = true): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        decorFitsSystemWindows = false
    )

@Composable
fun Modifier.safeScreenWindowPadding(): Modifier =
    this.windowInsetsPadding(safeFullScreenWindowInsets())

@Composable
fun Modifier.safeDialogWindowPadding(): Modifier =
    this.windowInsetsPadding(safeFullScreenWindowInsets())

@Composable
fun ConfigureEdgeToEdgeDialogWindow(fullScreen: Boolean = false) {
    val dialogView = LocalView.current
    SideEffect {
        val dialogWindow = generateSequence(dialogView.parent) { (it as? View)?.parent }
            .filterIsInstance<DialogWindowProvider>()
            .firstOrNull()
            ?.window
        if (dialogWindow != null) {
            @Suppress("DEPRECATION")
            dialogWindow.clearFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
            dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            @Suppress("DEPRECATION")
            dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dialogWindow.isStatusBarContrastEnforced = false
                dialogWindow.isNavigationBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            if (fullScreen) {
                dialogWindow.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            ViewCompat.requestApplyInsets(dialogWindow.decorView)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var boxModifier = modifier
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = Color(0x77000000),
            spotColor = Color(0x44000000)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xF0232723),
                    Color(0xF2151815)
                )
            )
        )

    if (testTag != null) {
        boxModifier = boxModifier.testTag(testTag)
    }

    if (onClick != null) {
        boxModifier = boxModifier.clickable(onClick = onClick)
    }

    val border = if (borderColor != null) {
        BorderStroke(1.1.dp, borderColor)
    } else {
        BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x3BFFFFFF), Color(0x10FFFFFF))
            )
        )
    }

    Surface(
        modifier = boxModifier,
        shape = shape,
        color = Color.Transparent,
        border = border
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun HeroGlowGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    glowColor: Color = VividGreen,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var cardMod = modifier
        .shadow(
            elevation = 16.dp,
            shape = shape,
            ambientColor = glowColor.copy(alpha = 0.25f),
            spotColor = glowColor.copy(alpha = 0.35f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xF51A2A15),
                    Color(0xF70E140C)
                )
            )
        )
    if (testTag != null) cardMod = cardMod.testTag(testTag)
    if (onClick != null) cardMod = cardMod.clickable(onClick = onClick)

    Surface(
        modifier = cardMod,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.90f),
                    glowColor.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun VividButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = VividGreen,
    contentColor: Color = DarkBackground,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    testTag: String = "vivid_button"
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(20.dp),
        contentPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
            PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        } else {
            contentPadding
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.35f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = DarkBackground,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    maxLines = 2,
                    softWrap = true,
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize.value + 3f).sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize
                )
            }
        }
    }
}

@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    testTag: String = "glass_outlined_button"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = if (contentPadding == ButtonDefaults.ContentPadding) {
            PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        } else {
            contentPadding
        },
        border = BorderStroke(1.dp, GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary,
            disabledContentColor = TextMuted
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VividGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                maxLines = 2,
                softWrap = true,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize.value + 3f).sp,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    testTag: String = "glass_text_field"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text(placeholder, color = TextMuted, maxLines = 1) },
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = TextSecondary) }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x33202020),
                unfocusedContainerColor = Color(0x22202020),
                focusedBorderColor = VividGreen,
                unfocusedBorderColor = GlassBorder,
                focusedLabelColor = VividGreen,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = VividGreen
            )
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = com.example.ui.theme.AlertRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LanguageToggle(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("language_toggle"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x40202020),
        border = BorderStroke(1.dp, GlassBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EN",
                fontSize = 12.sp,
                fontWeight = if (currentLanguage == AppLanguage.EN) FontWeight.Bold else FontWeight.Normal,
                color = if (currentLanguage == AppLanguage.EN) VividGreen else TextMuted
            )
            Text(
                text = " | ",
                fontSize = 12.sp,
                color = GlassBorderLight
            )
            Text(
                text = "বাং",
                fontSize = 12.sp,
                fontWeight = if (currentLanguage == AppLanguage.BN) FontWeight.Bold else FontWeight.Normal,
                color = if (currentLanguage == AppLanguage.BN) VividGreen else TextMuted
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.defaultMinSize(minHeight = 110.dp),
        onClick = onClick,
        testTag = "metric_card_$title"
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isHighlight) Color(0x265DD62C) else Color(0x20FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isHighlight) VividGreen else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = if (isHighlight) VividGreen else TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchGlassField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector = Icons.Default.Search,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = "search_glass_field"
) {
    GlassTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        testTag = testTag
    )
}

