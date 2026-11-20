package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.BackupImportMode
import com.example.data.model.BackupImportPreview
import com.example.data.model.BackupImportSummary
import com.example.domain.ShopTypeCatalog
import com.example.ui.components.BackupImportConfirmDialog
import com.example.ui.components.BackupImportPreviewDialog
import com.example.ui.components.BackupImportSummaryDialog
import com.example.ui.components.BackupPinDialog
import com.example.ui.components.BackupPinDialogMode
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.CsvExportTargetDialog
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.SettingsShopTypeDialog
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.PhotoStorageMode
import com.example.util.ReceiptImageStyle
import com.example.util.ShopLocale

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateDestination: (BusinessNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val shopType by storeViewModel.shopType.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val currentMember by storeViewModel.currentMember.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isDemoMode = storeViewModel.isDemoMode
    val showProfitInSimpleMode by storeViewModel.showProfitInSimpleMode.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
    val receiptImageStyle by storeViewModel.receiptImageStyle.collectAsState()
    val hasOwnerPin by storeViewModel.hasOwnerPin.collectAsState()
    val aiKeySummary by storeViewModel.aiKeySummary.collectAsState()
    val isBusy by storeViewModel.isBusy.collectAsState()
    val errorMessage by storeViewModel.errorMessage.collectAsState()
    val statusMessage by storeViewModel.statusMessage.collectAsState()

    LaunchedEffect(isSimpleMode, isFatherMode) {
        if (isSimpleMode && !isFatherMode) {
            onBack()
        }
    }

    BackHandler { onBack() }

    var showShopTypeDialog by remember { mutableStateOf(false) }
    var showPinDialogMode by remember { mutableStateOf<BackupPinDialogMode?>(null) }
    var showFatherModePinDialog by remember { mutableStateOf(false) }
    var pendingFatherModeEnabled by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var showAiKeyDialog by remember { mutableStateOf(false) }

    var importRawJson by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<BackupImportPreview?>(null) }
    var pendingImportMode by remember { mutableStateOf<BackupImportMode?>(null) }
    var importSummary by remember { mutableStateOf<BackupImportSummary?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            storeViewModel.exportStoreBackupToUri(uri)
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            storeViewModel.previewBackupImportFromUri(uri) { raw, preview ->
                importRawJson = raw
                importPreview = preview
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.22f),
                            VividGreen.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.14f, size.height * 0.08f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.22f),
                        radius = size.width * 0.55f
                    )
                )
            }
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header (scrolls naturally with content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ShopLocale.get("back", currentLanguage),
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = ShopLocale.get("settings_title", currentLanguage),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    softWrap = true
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VividGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        color = VividGreen,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AlertRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = AlertRed,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. APP MODE (Simple | Detail + Father Mode)
            SettingsSectionCard(
                title = if (currentLanguage == AppLanguage.BN) "অ্যাপ মোড" else "APP MODE",
                highlighted = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF101511))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val simpleSelected = isSimpleMode
                    val detailSelected = !isSimpleMode
                    SettingsSegmentedOption(
                        label = if (currentLanguage == AppLanguage.BN) "সহজ" else "Simple",
                        selected = simpleSelected,
                        onClick = { storeViewModel.setSimpleMode(true) },
                        modifier = Modifier.weight(1f),
                        testTag = "settings_mode_simple_btn"
                    )
                    SettingsSegmentedOption(
                        label = if (currentLanguage == AppLanguage.BN) "বিস্তারিত" else "Detail",
                        selected = detailSelected,
                        onClick = {
                            if (isFatherMode) {
                                Toast.makeText(
                                    context,
                                    ShopLocale.get("father_mode_blocked_detailed", currentLanguage),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                storeViewModel.setSimpleMode(false)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "settings_mode_detail_btn"
                    )
                }

                HorizontalDivider(color = GlassBorder)

                // Father Mode row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = ShopLocale.get("father_mode_title", currentLanguage),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isFatherMode) {
                                    ShopLocale.get("father_mode_lock_info", currentLanguage)
                                } else {
                                    ShopLocale.get("father_mode_subtitle", currentLanguage)
                                },
                                color = if (isFatherMode) VividGreen else TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    if (isOwner) {
                        Switch(
                            checked = isFatherMode,
                            onCheckedChange = { target ->
                                pendingFatherModeEnabled = target
                                showFatherModePinDialog = true
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = VividGreen
                            ),
                            modifier = Modifier.testTag("settings_father_mode_switch")
                        )
                    } else if (isFatherMode) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = VividGreen.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "চালু" else "Active",
                                color = VividGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 2. LANGUAGE & DISPLAY
            SettingsSectionCard(
                title = if (currentLanguage == AppLanguage.BN) "ভাষা ও ডিসপ্লে" else "LANGUAGE & DISPLAY",
                highlighted = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "ভাষা" else "Language",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF101511))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(AppLanguage.EN to "English", AppLanguage.BN to "বাংলা").forEach { (lang, label) ->
                            val active = currentLanguage == lang
                            Surface(
                                onClick = { storeViewModel.setLanguage(lang) },
                                shape = RoundedCornerShape(9.dp),
                                color = if (active) Color(0x28FFFFFF) else Color.Transparent,
                                border = if (active) BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)) else null,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .testTag("settings_lang_${lang.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) TextPrimary else TextSecondary,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // Hide Balance Numbers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "টাকার হিসাব লুকিয়ে রাখুন" else "Hide Balance Numbers",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = isPrivacyHidden,
                        onCheckedChange = { storeViewModel.togglePrivacyHidden() },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = VividGreen)
                    )
                }

                HorizontalDivider(color = GlassBorder)

                // Show profit in Simple Mode (Default OFF)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = ShopLocale.get("show_profit_simple_mode_title", currentLanguage),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = ShopLocale.get("show_profit_simple_mode_desc", currentLanguage),
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Switch(
                        checked = showProfitInSimpleMode,
                        onCheckedChange = { storeViewModel.setShowProfitInSimpleMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = VividGreen),
                        modifier = Modifier.testTag("settings_simple_mode_profit_switch")
                    )
                }

                HorizontalDivider(color = GlassBorder)

                // Receipt image style: Light / Dark (MUST PRESERVE)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (receiptImageStyle == ReceiptImageStyle.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ShopLocale.get("receipt_image_style_setting", currentLanguage),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = ShopLocale.get("receipt_image_style_desc", currentLanguage),
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReceiptStyleChoicePill(
                            label = ShopLocale.get("receipt_style_light", currentLanguage),
                            swatchBg = Color(0xFFFFFFFF),
                            swatchAccent = Color(0xFF337418),
                            selected = receiptImageStyle == ReceiptImageStyle.LIGHT,
                            onClick = { storeViewModel.setReceiptImageStyle(ReceiptImageStyle.LIGHT) },
                            modifier = Modifier.weight(1f),
                            testTag = "receipt_style_light_btn"
                        )
                        ReceiptStyleChoicePill(
                            label = ShopLocale.get("receipt_style_dark", currentLanguage),
                            swatchBg = Color(0xFF0F0F0F),
                            swatchAccent = Color(0xFF5DD62C),
                            selected = receiptImageStyle == ReceiptImageStyle.DARK,
                            onClick = { storeViewModel.setReceiptImageStyle(ReceiptImageStyle.DARK) },
                            modifier = Modifier.weight(1f),
                            testTag = "receipt_style_dark_btn"
                        )
                    }
                }
            }

            // 3. SHOP PROFILE & TYPE
            SettingsSectionCard(
                title = if (currentLanguage == AppLanguage.BN) "দোকানের তথ্য ও ধরন" else "SHOP PROFILE & TYPE"
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = store?.name.orEmpty().ifBlank { ShopLocale.get("receipt_header_store_default", currentLanguage) },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!store?.phone.isNullOrBlank()) {
                        Text(store!!.phone, color = TextSecondary, fontSize = 12.5.sp)
                    }
                    if (!store?.address.isNullOrBlank()) {
                        Text(store!!.address, color = TextMuted, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = GlassBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isOwner) { showShopTypeDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "দোকানের ধরন" else "Shop Type",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = shopType?.let { ShopTypeCatalog.displayName(it, currentLanguage) }
                                    ?: if (currentLanguage == AppLanguage.BN) "সাধারণ দোকান" else "General Store",
                                color = VividGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (isOwner) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "বদলান" else "Change",
                                color = VividGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (!isSimpleMode) {
                    HorizontalDivider(color = GlassBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateDestination(BusinessNavDestination.GREETING_CARDS_SCREEN) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "দোকানের কার্ড ও লোগো" else "Business Card & Logo",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "রশিদের উপরে দেখানোর জন্য লোগো যোগ করুন" else "Set store logo used on receipt images",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4. PHOTO STORAGE
            SettingsSectionCard(
                title = if (currentLanguage == AppLanguage.BN) "ছবি সংরক্ষণ" else "PHOTO STORAGE"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = if (isDemoMode) {
                                    ShopLocale.get("demo_photo_local_only", currentLanguage)
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "পণ্যের ছবি সিঙ্ক" else "Product Photo Storage"
                                },
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isDemoMode) {
                                    ShopLocale.get("demo_photo_local_only_sub", currentLanguage)
                                } else if (photoStorageMode == PhotoStorageMode.SYNC) {
                                    if (currentLanguage == AppLanguage.BN) "ক্লাউড সিঙ্ক চালু" else "Cloud sync across devices"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "শুধু এই ফোনে" else "Saved locally on this phone"
                                },
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    if (!isDemoMode) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF101511))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isCloud = photoStorageMode == PhotoStorageMode.SYNC
                            listOf(
                                true to (if (currentLanguage == AppLanguage.BN) "ক্লাউড" else "Cloud"),
                                false to (if (currentLanguage == AppLanguage.BN) "ফোন" else "Local")
                            ).forEach { (cloudOption, label) ->
                                val active = isCloud == cloudOption
                                Surface(
                                    onClick = {
                                        storeViewModel.setPhotoStorageMode(
                                            if (cloudOption) PhotoStorageMode.SYNC else PhotoStorageMode.PHONE_ONLY
                                        )
                                    },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (active) VividGreen.copy(alpha = 0.22f) else Color.Transparent,
                                    border = if (active) BorderStroke(1.dp, VividGreen.copy(alpha = 0.6f)) else null,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (active) VividGreen else TextSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. SECURITY & STAFF
            SettingsSectionCard(
                title = if (currentLanguage == AppLanguage.BN) "নিরাপত্তা" else "SECURITY"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "মালিকের পিন" else "Security PIN",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (hasOwnerPin) {
                                        if (currentLanguage == AppLanguage.BN) "· চালু" else "· Active"
                                    } else {
                                        if (currentLanguage == AppLanguage.BN) "· সেট নেই" else "· Not Set"
                                    },
                                    color = if (hasOwnerPin) VividGreen else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) {
                                    "পণ্য মুছা, রিস্টোর ও বাবা মোড সুরক্ষিত রাখে"
                                } else {
                                    "Protects deleting, restoring & mode lock"
                                },
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            showPinDialogMode = if (hasOwnerPin) BackupPinDialogMode.CHANGE_PIN else BackupPinDialogMode.SET_PIN
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0x18FFFFFF),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasOwnerPin) {
                                    if (currentLanguage == AppLanguage.BN) "বদলান" else "Change"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "সেট করুন" else "Set PIN"
                                },
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isOwner && !isSimpleMode && !isDemoMode) {
                    HorizontalDivider(color = GlassBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateDestination(BusinessNavDestination.STAFF_MANAGEMENT_SCREEN) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                            Text(
                                text = ShopLocale.get("manage_staff_setting", currentLanguage),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 6. OWNER: AI KEY (owner only, hidden in demo & simple mode) & EXPORT / IMPORT (hidden in Simple Mode)
            if (isOwner && !isSimpleMode && !isDemoMode) {
                SettingsSectionCard(
                    title = if (currentLanguage == AppLanguage.BN) "AI কী (শুধু মালিক)" else "AI KEY (OWNER ONLY)"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAiKeyDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VividGreen, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "প্রাইভেট Gemini AI কী" else "Private Gemini AI Key",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (aiKeySummary.hasSavedKey) aiKeySummary.maskedDisplay
                                    else if (currentLanguage == AppLanguage.BN) "সেট করা নেই (ঐচ্ছিক)" else "Not set (Optional)",
                                    color = if (aiKeySummary.hasSavedKey) VividGreen else TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = if (aiKeySummary.hasSavedKey) {
                                    if (currentLanguage == AppLanguage.BN) "পরিচালনা" else "Manage"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "যোগ করুন" else "Add"
                                },
                                color = VividGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (isOwner && !isSimpleMode) {

                SettingsSectionCard(
                    title = if (currentLanguage == AppLanguage.BN) "এক্সপোর্ট ও ইমপোর্ট" else "EXPORT & IMPORT",
                    highlighted = true
                ) {
                    val isCloudActive = store?.id != "demo_store" && !store?.id.isNullOrBlank()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCloudActive) VividGreen else TextMuted)
                        )
                        Text(
                            text = if (isCloudActive) {
                                if (currentLanguage == AppLanguage.BN) "ফায়ারবেস ক্লাউড যুক্ত" else "Firebase Connected"
                            } else {
                                if (currentLanguage == AppLanguage.BN) "ডেমো লোকাল মোড" else "Demo Local Store"
                            },
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassOutlinedButton(
                            text = if (currentLanguage == AppLanguage.BN) "এক্সপোর্ট JSON" else "Export JSON",
                            icon = Icons.Default.CloudDownload,
                            onClick = {
                                val name = "storeos_backup_${System.currentTimeMillis()}.json"
                                exportBackupLauncher.launch(name)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_export_backup_btn"
                        )
                        GlassOutlinedButton(
                            text = if (currentLanguage == AppLanguage.BN) "ইমপোর্ট JSON" else "Import JSON",
                            icon = Icons.Default.CloudUpload,
                            onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_import_backup_btn"
                        )
                    }
                    GlassOutlinedButton(
                        text = if (currentLanguage == AppLanguage.BN) "CSV শীট এক্সপোর্ট" else "Export CSV Sheet",
                        icon = Icons.Default.TableChart,
                        onClick = { showCsvDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_export_csv_btn"
                    )
                }
            }

            // 7. SIGN OUT (Danger zone glass card)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A0E0E),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.42f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "অ্যাকাউন্ট" else "ACCOUNT",
                        color = AlertRed.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    VividButton(
                        text = if (isDemoMode) {
                            ShopLocale.get("exit_demo_mode", currentLanguage)
                        } else if (currentLanguage == AppLanguage.BN) {
                            "সাইন আউট"
                        } else {
                            "Sign Out"
                        },
                        icon = Icons.AutoMirrored.Filled.Logout,
                        containerColor = AlertRed.copy(alpha = 0.88f),
                        contentColor = Color.White,
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_logout_btn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (showShopTypeDialog) {
        SettingsShopTypeDialog(
            currentShopType = shopType,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            onConfirmChange = { newType, addSuggested ->
                storeViewModel.updateStoreShopType(newType, addSuggested) {
                    showShopTypeDialog = false
                }
            },
            onDismiss = { showShopTypeDialog = false }
        )
    }

    if (showPinDialogMode != null) {
        BackupPinDialog(
            mode = showPinDialogMode!!,
            currentLanguage = currentLanguage,
            onSetPin = { pin, done ->
                storeViewModel.setOwnerPin(pin) {
                    done()
                    showPinDialogMode = null
                }
            },
            onVerifyPin = { pin, res -> storeViewModel.verifyOwnerPin(pin, res) },
            onChangePin = { oldP, newP, res ->
                storeViewModel.changeOwnerPin(oldP, newP) { ok ->
                    res(ok)
                    if (ok) showPinDialogMode = null
                }
            },
            onDismiss = { showPinDialogMode = null }
        )
    }

    if (showFatherModePinDialog && isOwner) {
        val targetMemberId = currentMember?.userId?.ifBlank { null } ?: store?.ownerId ?: ""
        BackupPinDialog(
            mode = if (hasOwnerPin) BackupPinDialogMode.ENTER_PIN else BackupPinDialogMode.SET_PIN,
            currentLanguage = currentLanguage,
            customDescription = ShopLocale.get("father_mode_pin_toggle_desc", currentLanguage),
            onSetPin = { pin, _ ->
                storeViewModel.setOwnerPin(pin) {
                    storeViewModel.setFatherModeForMember(
                        memberId = targetMemberId,
                        enabled = pendingFatherModeEnabled,
                        memberDisplayName = currentMember?.displayName ?: "Owner",
                        memberEmail = currentMember?.email ?: "",
                        onSuccess = { showFatherModePinDialog = false }
                    )
                }
            },
            onVerifyPin = { pin, onResult ->
                storeViewModel.verifyOwnerPin(pin) { ok ->
                    onResult(ok)
                    if (ok) {
                        storeViewModel.setFatherModeForMember(
                            memberId = targetMemberId,
                            enabled = pendingFatherModeEnabled,
                            memberDisplayName = currentMember?.displayName ?: "Owner",
                            memberEmail = currentMember?.email ?: "",
                            onSuccess = { showFatherModePinDialog = false }
                        )
                    }
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = { showFatherModePinDialog = false }
        )
    }

    if (showCsvDialog) {
        CsvExportTargetDialog(
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            onSelectTarget = { target ->
                storeViewModel.exportCsvTarget(target) { file ->
                    showCsvDialog = false
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, file.name))
                }
            },
            onDismiss = { showCsvDialog = false }
        )
    }

    if (importPreview != null && pendingImportMode == null && importSummary == null) {
        BackupImportPreviewDialog(
            preview = importPreview!!,
            currentLanguage = currentLanguage,
            onChooseMode = { mode -> pendingImportMode = mode },
            onDismiss = {
                importPreview = null
                importRawJson = null
            }
        )
    }

    if (importPreview != null && pendingImportMode != null && importSummary == null) {
        BackupImportConfirmDialog(
            preview = importPreview!!,
            mode = pendingImportMode!!,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            onConfirm = {
                val json = importRawJson ?: return@BackupImportConfirmDialog
                storeViewModel.confirmBackupImport(json, pendingImportMode!!) { sum ->
                    importPreview = null
                    importRawJson = null
                    pendingImportMode = null
                    importSummary = sum
                }
            },
            onDismiss = { pendingImportMode = null }
        )
    }

    if (importSummary != null) {
        BackupImportSummaryDialog(
            summary = importSummary!!,
            currentLanguage = currentLanguage,
            onDone = { importSummary = null }
        )
    }

    if (showAiKeyDialog) {
        AiKeyManageDialog(
            hasSavedKey = aiKeySummary.hasSavedKey,
            maskedKey = aiKeySummary.maskedDisplay,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            onSave = { rawKey ->
                storeViewModel.testAndSavePrivateAiKey(rawKey) {
                    showAiKeyDialog = false
                    Toast.makeText(context, ShopLocale.get("ai_key_saved_msg", currentLanguage), Toast.LENGTH_SHORT).show()
                }
            },
            onRemove = {
                storeViewModel.removePrivateAiKey {
                    showAiKeyDialog = false
                }
            },
            onDismiss = { showAiKeyDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (highlighted) Color(0xFF101911) else Color(0xFF111412),
        border = BorderStroke(
            width = 1.dp,
            brush = if (highlighted) {
                Brush.verticalGradient(
                    colors = listOf(
                        VividGreen.copy(alpha = 0.52f),
                        Color(0x28FFFFFF)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x38FFFFFF),
                        Color(0x14FFFFFF)
                    )
                )
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = if (highlighted) VividGreen.copy(alpha = 0.85f) else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

@Composable
private fun SettingsSegmentedOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFF1C2B1E) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)) else null,
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) TextPrimary else TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReceiptStyleChoicePill(
    label: String,
    swatchBg: Color,
    swatchAccent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) VividGreen.copy(alpha = 0.16f) else Color(0x18FFFFFF),
        border = BorderStroke(1.5.dp, if (selected) VividGreen else GlassBorder),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(swatchBg)
                    .border(2.dp, swatchAccent, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (selected) VividGreen else TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiKeyManageDialog(
    hasSavedKey: Boolean,
    maskedKey: String,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkBackground,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "প্রাইভেট Gemini AI কী" else "Private Gemini AI Key",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (hasSavedKey) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "বর্তমান কী: $maskedKey" else "Saved key: $maskedKey",
                        color = VividGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                GlassTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = "AIza...",
                    leadingIcon = Icons.Default.Key,
                    testTag = "ai_key_input"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasSavedKey) {
                        GlassOutlinedButton(
                            text = if (currentLanguage == AppLanguage.BN) "মুছুন" else "Remove",
                            onClick = onRemove,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        GlassOutlinedButton(
                            text = ShopLocale.get("cancel", currentLanguage),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VividButton(
                        text = if (currentLanguage == AppLanguage.BN) "যাচাই ও সেভ" else "Verify & Save",
                        enabled = keyInput.trim().length >= 10,
                        isLoading = isBusy,
                        onClick = { onSave(keyInput.trim()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
