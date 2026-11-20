package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.model.Product
import com.example.domain.CalculationEngine
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.domain.ShopUnitPreset
import com.example.ui.components.BackupPinDialog
import com.example.ui.components.BackupPinDialogMode
import com.example.ui.components.FatherConfirmActionDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.VividButton
import com.example.ui.components.getCategoryIcon
import kotlin.math.abs
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.PhotoStorageMode
import com.example.util.ProductImageUtil
import com.example.util.ShopLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddProductWizardDialog(
    productToEdit: Product? = null,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    canSeeBuyingPrice: Boolean = true,
    isSimpleMode: Boolean = false,
    isFatherMode: Boolean = false,
    hasOwnerPin: Boolean = false,
    onVerifyOwnerPin: ((String, (Boolean) -> Unit) -> Unit)? = null,
    onSetOwnerPin: ((String, () -> Unit) -> Unit)? = null,
    shopType: ShopType? = null,
    availableCategories: List<String> = emptyList(),
    unitPresets: List<ShopUnitPreset> = emptyList(),
    photoStorageMode: PhotoStorageMode = PhotoStorageMode.SYNC,
    externalError: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        nameBn: String,
        category: String,
        unit: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Double,
        lowStockLimit: Double,
        photoUri: String,
        barcode: String,
        wholesalePrice: Double,
        mrp: Double,
        itemLocation: String,
        partyRate: Double,
        buyUnit: String,
        buyConversion: Double,
        sellUnits: Map<String, Double>
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isEditMode = productToEdit != null
    val showCostAndProfit = canSeeBuyingPrice && (!isSimpleMode || isFatherMode)
    var step by remember { mutableIntStateOf(1) } // 1: Basics, 2: Pricing, 3: Stock

    val categories = remember(availableCategories, shopType, productToEdit) {
        val result = mutableListOf<String>()
        fun addUnique(raw: String) {
            val trimmed = raw.trim()
            if (trimmed.isNotBlank() && result.none { it.equals(trimmed, ignoreCase = true) }) {
                result.add(trimmed)
            }
        }
        availableCategories.forEach(::addUnique)
        if (result.isEmpty() && shopType != null) {
            ShopTypeCatalog.startingCategories(shopType).forEach(::addUnique)
        }
        productToEdit?.category?.let(::addUnique)
        if (result.isEmpty()) {
            ShopTypeCatalog.startingCategories(ShopType.GENERAL).forEach(::addUnique)
        }
        result.toList()
    }
    val resolvedPresets = remember(unitPresets, shopType) {
        when {
            unitPresets.isNotEmpty() -> unitPresets
            shopType != null -> ShopTypeCatalog.unitPresets(shopType)
            else -> emptyList()
        }
    }
    val initialCategory = remember(productToEdit, categories) {
        productToEdit?.category?.ifBlank { categories.firstOrNull() ?: "General" }
            ?: categories.firstOrNull()
            ?: "General"
    }
    val initialPreset = remember(productToEdit, shopType, initialCategory) {
        if (productToEdit == null && shopType != null) {
            ShopTypeCatalog.presetForCategory(shopType, initialCategory)
        } else {
            null
        }
    }

    // Step 1: Basics
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var nameBn by remember { mutableStateOf(productToEdit?.nameBn ?: "") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedUnit by remember {
        mutableStateOf(
            productToEdit?.let { CalculationEngine.effectiveStockUnit(it) }
                ?: initialPreset?.stockUnit
                ?: "pcs"
        )
    }
    var buyUnit by remember {
        mutableStateOf(
            productToEdit?.let { CalculationEngine.effectiveBuyUnit(it) }
                ?: initialPreset?.buyUnit
                ?: selectedUnit
        )
    }
    var buyConversionInput by remember {
        mutableStateOf(
            when {
                productToEdit != null -> ShopLocale.formatNumber(CalculationEngine.effectiveBuyConversion(productToEdit), AppLanguage.EN)
                initialPreset != null -> ShopLocale.formatNumber(initialPreset.buyConversion, AppLanguage.EN)
                else -> "1"
            }
        )
    }
    var sellUnitInputs by remember {
        mutableStateOf<Map<String, String>>(
            when {
                productToEdit != null -> productToEdit.sellUnits.mapValues { (_, factor) ->
                    ShopLocale.formatNumber(factor, AppLanguage.EN)
                }
                initialPreset != null -> initialPreset.sellUnits.mapValues { (_, factor) ->
                    ShopLocale.formatNumber(factor, AppLanguage.EN)
                }
                else -> emptyMap()
            }
        )
    }
    var hasCustomizedUnits by remember { mutableStateOf(productToEdit != null) }
    var showAddSellUnitPicker by remember { mutableStateOf(false) }

    val initialSavedPhotoUri = remember(productToEdit) { productToEdit?.photoUri ?: "" }
    var photoUriString by remember { mutableStateOf(initialSavedPhotoUri) }
    var barcodeInput by remember { mutableStateOf(productToEdit?.barcode ?: "") }
    var itemLocationInput by remember { mutableStateOf(productToEdit?.itemLocation ?: "") }
    var showPhotoSourceChoices by remember { mutableStateOf(false) }
    var showInAppCamera by remember { mutableStateOf(false) }

    fun cleanupStagedCameraPhotoIfUnused(oldUri: String) {
        if (oldUri != initialSavedPhotoUri &&
            (oldUri.contains("/product_photos/cam_") || oldUri.contains("/product_photos/cropped_"))
        ) {
            ProductImageUtil.deleteLocalPhotoIfOwned(context, oldUri)
        }
    }

    fun handleWizardDismiss() {
        cleanupStagedCameraPhotoIfUnused(photoUriString)
        onDismiss()
    }

    // Step 2: Pricing
    var buyPriceInput by remember {
        mutableStateOf(if (isEditMode) formatEditablePrice(productToEdit!!.buyPrice) else "")
    }
    var sellPriceInput by remember {
        mutableStateOf(if (isEditMode) formatEditablePrice(productToEdit!!.sellPrice) else "")
    }
    var wholesalePriceInput by remember {
        mutableStateOf(if (isEditMode && productToEdit!!.wholesalePrice > 0.0) formatEditablePrice(productToEdit.wholesalePrice) else "")
    }
    var mrpInput by remember {
        mutableStateOf(if (isEditMode && productToEdit!!.mrp > 0.0) formatEditablePrice(productToEdit.mrp) else "")
    }
    var partyRateInput by remember {
        mutableStateOf(if (isEditMode && productToEdit!!.partyRate > 0.0) formatEditablePrice(productToEdit.partyRate) else "")
    }

    // Step 3: Stock
    var stockInput by remember {
        mutableStateOf(
            if (isEditMode) ShopLocale.formatNumber(productToEdit!!.stock, AppLanguage.EN) else "10"
        )
    }
    var lowStockLimitInput by remember {
        mutableStateOf(
            if (isEditMode) ShopLocale.formatNumber(productToEdit!!.lowStockLimit, AppLanguage.EN) else "5"
        )
    }

    var stepError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFatherPinDialog by remember { mutableStateOf(false) }
    var fatherPinReasonDesc by remember { mutableStateOf("") }
    var showFatherConfirmSave by remember { mutableStateOf(false) }
    var pendingSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSaveSummary by remember { mutableStateOf("") }

    // Photo picker using Android Photo Picker (zero broad storage permission required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cleanupStagedCameraPhotoIfUnused(photoUriString)
            photoUriString = uri.toString()
            showPhotoSourceChoices = false
            stepError = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            stepError = null
            showInAppCamera = true
        } else {
            stepError = "Camera access denied — you can choose a photo from Gallery"
        }
    }

    fun onTakePhotoClick() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            stepError = null
            showInAppCamera = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val units = remember(resolvedPresets, selectedUnit, buyUnit) {
        val extras = buildList {
            add(selectedUnit)
            add(buyUnit)
            resolvedPresets.forEach { preset ->
                add(preset.stockUnit)
                add(preset.buyUnit)
                addAll(preset.sellUnits.keys)
            }
            add("sack")
            add("roll")
        }.map { it.trim() }.filter { it.isNotBlank() }
        (CalculationEngine.SUPPORTED_UNITS + extras).distinctBy { it.lowercase() }
    }

    if (showInAppCamera) {
        ProductCameraScreen(
            maxDimensionPx = if (photoStorageMode == PhotoStorageMode.SYNC) {
                ProductImageUtil.SYNC_MAX_DIMENSION_PX
            } else {
                ProductImageUtil.PHONE_ONLY_MAX_DIMENSION_PX
            },
            maxBytes = if (photoStorageMode == PhotoStorageMode.SYNC) {
                ProductImageUtil.SYNC_MAX_BYTES
            } else {
                ProductImageUtil.PHONE_ONLY_MAX_BYTES
            },
            onPhotoConfirmed = { capturedUri ->
                cleanupStagedCameraPhotoIfUnused(photoUriString)
                photoUriString = capturedUri.toString()
                showPhotoSourceChoices = false
                stepError = null
                showInAppCamera = false
            },
            onPickFromGallery = {
                showInAppCamera = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCancel = {
                showInAppCamera = false
            }
        )
    } else {
    val hostActivitySafeInsets = rememberHostActivitySafeWindowInsets()
    Dialog(
        onDismissRequest = ::handleWizardDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        val wizardDialogView = LocalView.current
        SideEffect {
            val dialogWindow = generateSequence(wizardDialogView.parent) { (it as? android.view.View)?.parent }
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
                dialogWindow.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                ViewCompat.requestApplyInsets(dialogWindow.decorView)
            }
        }
        val safeWizardInsets = WindowInsets.statusBars
            .union(WindowInsets.navigationBars)
            .union(WindowInsets.displayCutout)
            .union(WindowInsets.ime)
            .union(hostActivitySafeInsets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B08))
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VividGreen.copy(alpha = 0.24f),
                                Color(0xFF0F260B).copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.08f),
                            radius = size.width * 0.92f
                        )
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            if (showDeleteConfirm) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(safeWizardInsets)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val confirmShape = RoundedCornerShape(28.dp)
                    Surface(
                        shape = confirmShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(listOf(AlertRed, AlertRed.copy(alpha = 0.4f)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .clip(confirmShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xF71A1F1A), Color(0xFA111411))
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFatherMode) {
                                        ShopLocale.get("father_move_to_bin_title", currentLanguage)
                                    } else if (currentLanguage == AppLanguage.BN) {
                                        "পণ্য মুছে ফেলবেন?"
                                    } else {
                                        "Delete Product?"
                                    },
                                    color = AlertRed,
                                    fontSize = if (isFatherMode) 20.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                IconButton(
                                    onClick = { showDeleteConfirm = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = ShopLocale.get("close", currentLanguage), tint = TextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isFatherMode) {
                                    ShopLocale.format("father_move_to_bin_desc", currentLanguage, productToEdit?.name ?: "")
                                } else if (currentLanguage == AppLanguage.BN) {
                                    "'${productToEdit?.name}' মুছে ফেলতে চান? বর্তমান স্টক ও তথ্য মুছে যাবে।"
                                } else {
                                    "Are you sure you want to delete '${productToEdit?.name}'? All current inventory and details will be removed. This action cannot be undone."
                                },
                                color = TextSecondary,
                                fontSize = if (isFatherMode) 14.sp else 13.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassOutlinedButton(
                                    text = ShopLocale.get("cancel", currentLanguage),
                                    onClick = { showDeleteConfirm = false },
                                    modifier = Modifier.weight(1f)
                                )
                                VividButton(
                                    text = if (isFatherMode) {
                                        ShopLocale.get("father_move_to_bin_btn", currentLanguage)
                                    } else if (currentLanguage == AppLanguage.BN) {
                                        "মুছুন"
                                    } else {
                                        "Delete"
                                    },
                                    onClick = {
                                        showDeleteConfirm = false
                                        onDelete?.invoke()
                                    },
                                    containerColor = AlertRed,
                                    contentColor = Color.White,
                                    isLoading = isBusy,
                                    icon = Icons.Default.Delete,
                                    modifier = Modifier.weight(1f),
                                    testTag = "confirm_delete_btn"
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(safeWizardInsets)
                        .widthIn(max = 520.dp)
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp)
                ) {
                    // Top Bar: Circular Frosted Close Button + 3-Step Horizontal Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = ::handleWizardDismiss,
                            shape = CircleShape,
                            color = Color(0x24FFFFFF),
                            border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        WizardStepperHeader(
                            currentStep = step,
                            currentLanguage = currentLanguage,
                            modifier = Modifier.weight(1f)
                        )

                        if (isEditMode && onDelete != null && !isSimpleMode) {
                            Surface(
                                onClick = { showDeleteConfirm = true },
                                shape = CircleShape,
                                color = Color(0x28E53935),
                                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("wizard_delete_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = AlertRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step Contents (scrollable so photo preview + inputs never clip on small phones)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                    when (step) {
                        1 -> {
                            // STEP 1: BASICS
                            Column {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = when {
                                            isEditMode && currentLanguage == AppLanguage.BN -> "পণ্য ঠিক করুন"
                                            isEditMode -> "Edit Product"
                                            currentLanguage == AppLanguage.BN -> "মূল তথ্য"
                                            else -> "Basics"
                                        },
                                        color = TextPrimary,
                                        fontSize = if (isFatherMode) 26.sp else 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "ধাপ ১ / ৩: মূল তথ্য" else "Step 1 of 3: Basics",
                                        color = VividGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassTextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                        stepError = null
                                    },
                                    label = if (currentLanguage == AppLanguage.BN) "পণ্যের নাম (ইংরেজি) *" else "Product Name (English) *",
                                    placeholder = "e.g. Miniket Rice 5kg",
                                    testTag = "wizard_name_en"
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                GlassTextField(
                                    value = nameBn,
                                    onValueChange = { nameBn = it },
                                    label = if (currentLanguage == AppLanguage.BN) "পণ্যের নাম (বাংলা - ঐচ্ছিক)" else "Product Name (বাংলা - Optional)",
                                    placeholder = "যেমনঃ মিনিকেট চাল ৫ কেজি",
                                    testTag = "wizard_name_bn"
                                )

                                if (!isSimpleMode) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "ক্যাটাগরি" else "Category",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(categories) { cat ->
                                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) Color(0x335DD62C) else Color(0x20FFFFFF),
                                                border = BorderStroke(1.dp, if (isSelected) VividGreen else GlassBorder),
                                                modifier = Modifier
                                                    .clickable {
                                                        selectedCategory = cat
                                                        if (!isEditMode && !hasCustomizedUnits && shopType != null) {
                                                            val preset = ShopTypeCatalog.presetForCategory(shopType, cat)
                                                            if (preset != null) {
                                                                selectedUnit = preset.stockUnit
                                                                buyUnit = preset.buyUnit
                                                                buyConversionInput = ShopLocale.formatNumber(preset.buyConversion, AppLanguage.EN)
                                                                sellUnitInputs = preset.sellUnits.mapValues { (_, factor) ->
                                                                    ShopLocale.formatNumber(factor, AppLanguage.EN)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .testTag("wizard_category_$cat")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = getCategoryIcon(cat),
                                                        contentDescription = null,
                                                        tint = if (isSelected) VividGreen else TextMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = ShopLocale.displayCategory(cat, currentLanguage),
                                                        color = if (isSelected) VividGreen else TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isEditMode && resolvedPresets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = ShopLocale.get("shop_type_unit_presets", currentLanguage),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(resolvedPresets, key = { it.id }) { preset ->
                                            val isPresetActive = selectedUnit.equals(preset.stockUnit, ignoreCase = true) &&
                                                buyUnit.equals(preset.buyUnit, ignoreCase = true)
                                            Surface(
                                                onClick = {
                                                    selectedUnit = preset.stockUnit
                                                    buyUnit = preset.buyUnit
                                                    buyConversionInput = ShopLocale.formatNumber(preset.buyConversion, AppLanguage.EN)
                                                    sellUnitInputs = preset.sellUnits.mapValues { (_, factor) ->
                                                        ShopLocale.formatNumber(factor, AppLanguage.EN)
                                                    }
                                                    categories.firstOrNull { it.equals(preset.category, ignoreCase = true) }?.let {
                                                        selectedCategory = it
                                                    }
                                                    hasCustomizedUnits = true
                                                    stepError = null
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isPresetActive) Color(0x305DD62C) else Color(0x1EFFFFFF),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isPresetActive) VividGreen else GlassBorder
                                                ),
                                                modifier = Modifier.testTag("wizard_unit_preset_${preset.id}")
                                            ) {
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.BN) preset.labelBn else preset.labelEn,
                                                    color = if (isPresetActive) VividGreen else TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Stock unit selection
                                Text(
                                    text = ShopLocale.get("stock_unit_label", currentLanguage),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(units) { u ->
                                        val isSelected = selectedUnit.equals(u, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0x335DD62C) else Color(0x20FFFFFF),
                                            border = BorderStroke(1.dp, if (isSelected) VividGreen else GlassBorder),
                                            modifier = Modifier
                                                .clickable {
                                                    val wasDefaultBuy = buyUnit.equals(selectedUnit, ignoreCase = true)
                                                    selectedUnit = u
                                                    if (wasDefaultBuy || buyUnit.equals(u, ignoreCase = true)) {
                                                        buyUnit = u
                                                        buyConversionInput = "1"
                                                    }
                                                    sellUnitInputs = sellUnitInputs.filterKeys { !it.equals(u, ignoreCase = true) }
                                                    hasCustomizedUnits = true
                                                    stepError = null
                                                }
                                                .testTag("wizard_stock_unit_$u")
                                        ) {
                                            Text(
                                                text = u,
                                                color = if (isSelected) VividGreen else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Buy unit & Conversion
                                Text(
                                    text = ShopLocale.get("buy_unit_label", currentLanguage),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("wizard_buy_unit_row")
                                ) {
                                    items(units) { u ->
                                        val isSelected = buyUnit.equals(u, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0x335DD62C) else Color(0x20FFFFFF),
                                            border = BorderStroke(1.dp, if (isSelected) VividGreen else GlassBorder),
                                            modifier = Modifier
                                                .clickable {
                                                    buyUnit = u
                                                    if (u.equals(selectedUnit, ignoreCase = true)) {
                                                        buyConversionInput = "1"
                                                    } else if (buyConversionInput == "1") {
                                                        val suggested = CalculationEngine.defaultSubUnitConversion(u, selectedUnit)
                                                        if (suggested != null) {
                                                            buyConversionInput = ShopLocale.formatNumber(suggested, AppLanguage.EN)
                                                        }
                                                    }
                                                    hasCustomizedUnits = true
                                                    stepError = null
                                                }
                                                .testTag("wizard_buy_unit_$u")
                                        ) {
                                            Text(
                                                text = u,
                                                color = if (isSelected) VividGreen else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                if (!buyUnit.equals(selectedUnit, ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0x1EFFFFFF),
                                        border = BorderStroke(1.dp, GlassBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "1 $buyUnit =",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            GlassTextField(
                                                value = buyConversionInput,
                                                onValueChange = {
                                                    buyConversionInput = it
                                                    stepError = null
                                                },
                                                label = ShopLocale.get("conversion_label", currentLanguage),
                                                placeholder = "25",
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                testTag = "wizard_buy_conversion",
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = selectedUnit,
                                                color = VividGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Selling units (Stock unit + optional smaller units)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ShopLocale.get("sell_units_label", currentLanguage),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val availableSubUnits = units.filter { cand ->
                                        !cand.equals(selectedUnit, ignoreCase = true) &&
                                            sellUnitInputs.keys.none { it.equals(cand, ignoreCase = true) }
                                    }
                                    if (availableSubUnits.isNotEmpty()) {
                                        Surface(
                                            onClick = { showAddSellUnitPicker = !showAddSellUnitPicker },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0x285DD62C),
                                            border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
                                            modifier = Modifier.testTag("wizard_add_sell_unit_btn")
                                        ) {
                                            Text(
                                                text = ShopLocale.get("add_sell_unit", currentLanguage),
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

                                Spacer(modifier = Modifier.height(6.dp))

                                // Base selling unit chip + configured sub-units
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x18FFFFFF),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "1 $selectedUnit (${ShopLocale.get("stock_unit_label", currentLanguage)})",
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }

                                if (showAddSellUnitPicker) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val candidateUnits = units.filter { cand ->
                                        !cand.equals(selectedUnit, ignoreCase = true) &&
                                            sellUnitInputs.keys.none { it.equals(cand, ignoreCase = true) }
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(candidateUnits) { cand ->
                                            Surface(
                                                onClick = {
                                                    val suggested = CalculationEngine.defaultSubUnitConversion(selectedUnit, cand)
                                                    val initialFactor = suggested?.let { ShopLocale.formatNumber(it, AppLanguage.EN) } ?: ""
                                                    sellUnitInputs = sellUnitInputs + (cand to initialFactor)
                                                    showAddSellUnitPicker = false
                                                    stepError = null
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0x24FFFFFF),
                                                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.5f)),
                                                modifier = Modifier.testTag("wizard_pick_sell_unit_$cand")
                                            ) {
                                                Text(
                                                    text = "+ $cand",
                                                    color = VividGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                sellUnitInputs.forEach { (subUnit, factorText) ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0x1EFFFFFF),
                                        border = BorderStroke(1.dp, GlassBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "1 $selectedUnit =",
                                                color = TextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            GlassTextField(
                                                value = factorText,
                                                onValueChange = { updated ->
                                                    sellUnitInputs = sellUnitInputs.toMutableMap().apply { put(subUnit, updated) }
                                                    stepError = null
                                                },
                                                label = subUnit,
                                                placeholder = "1000",
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                testTag = "wizard_sell_unit_factor_$subUnit",
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = subUnit,
                                                color = VividGreen,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            IconButton(
                                                onClick = {
                                                    sellUnitInputs = sellUnitInputs - subUnit
                                                    stepError = null
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("wizard_remove_sell_unit_$subUnit")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove $subUnit",
                                                    tint = AlertRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Add Optional Photo (Take Photo / Choose from Gallery / Preview with Change & Remove)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x20FFFFFF))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (photoUriString.isNotBlank()) {
                                            if (currentLanguage == AppLanguage.BN) "পণ্যের ছবি" else "Product Photo"
                                        } else {
                                            if (currentLanguage == AppLanguage.BN) "ঐচ্ছিক ছবি যোগ করুন" else "Add Optional Photo"
                                        },
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (photoUriString.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            ProductThumbnail(
                                                photoUri = photoUriString,
                                                category = selectedCategory,
                                                contentDescription = name.ifBlank { "Product photo preview" },
                                                storeId = productToEdit?.storeId ?: "",
                                                productId = productToEdit?.id ?: "",
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .testTag("wizard_photo_preview"),
                                                iconSize = 28.dp,
                                                cornerRadius = 12.dp
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val photoStatusLine = when {
                                                    ProductImageUtil.isSyncDataUri(photoUriString) ->
                                                        "Synced • max ${ProductImageUtil.SYNC_MAX_DIMENSION_PX}px JPEG"
                                                    ProductImageUtil.isLocalOnlyFlag(photoUriString) ->
                                                        "Saved on this phone only"
                                                    ProductImageUtil.isLocalPhotoUri(photoUriString) -> {
                                                        if (photoStorageMode == PhotoStorageMode.SYNC) {
                                                            "Ready • syncs as ${ProductImageUtil.SYNC_MAX_DIMENSION_PX}px JPEG"
                                                        } else {
                                                            "Ready • saves on phone only"
                                                        }
                                                    }
                                                    else -> "Saved product photo"
                                                }
                                                Text(
                                                    text = photoStatusLine,
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        onClick = { showPhotoSourceChoices = !showPhotoSourceChoices },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0x265DD62C),
                                                        border = BorderStroke(1.dp, VividGreen),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .defaultMinSize(minHeight = 44.dp)
                                                            .testTag("wizard_photo_change_btn")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = null,
                                                                tint = VividGreen,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Change",
                                                                color = VividGreen,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        onClick = {
                                                            cleanupStagedCameraPhotoIfUnused(photoUriString)
                                                            photoUriString = ""
                                                            showPhotoSourceChoices = false
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0x26FF5252),
                                                        border = BorderStroke(1.dp, AlertRed),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .defaultMinSize(minHeight = 44.dp)
                                                            .testTag("wizard_photo_remove_btn")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = null,
                                                                tint = AlertRed,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Remove",
                                                                color = AlertRed,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "ছবি না দিলে ক্যাটাগরি আইকন দেখাবে" else "Uses category icon if omitted",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (photoUriString.isBlank() || showPhotoSourceChoices) {
                                        Surface(
                                            onClick = { onTakePhotoClick() },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0x1FFFFFFF),
                                            border = BorderStroke(1.dp, GlassBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("wizard_take_photo_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = VividGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.BN) "ছবি তুলুন" else "Take Photo",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0x1FFFFFFF),
                                            border = BorderStroke(1.dp, GlassBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("wizard_choose_gallery_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = VividGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.BN) "গ্যালারি থেকে বাছাই করুন" else "Choose from Gallery",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isSimpleMode) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    GlassTextField(
                                        value = barcodeInput,
                                        onValueChange = { barcodeInput = it },
                                        label = if (currentLanguage == AppLanguage.BN) "বারকোড (ঐচ্ছিক)" else "Barcode (Optional)",
                                        placeholder = "e.g. 890123456789",
                                        testTag = "wizard_barcode"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    GlassTextField(
                                        value = itemLocationInput,
                                        onValueChange = { itemLocationInput = it },
                                        label = if (currentLanguage == AppLanguage.BN) "তাকের অবস্থান (ঐচ্ছিক)" else "Item Location (Optional)",
                                        placeholder = "e.g. Shelf A-2",
                                        testTag = "wizard_item_location"
                                    )
                                }
                            }
                        }

                        2 -> {
                            // STEP 2: PRICING (BUY/SELL TILES + LIVE PROFIT/MARGIN + DETAILED WHOLESALE/MRP + LOW STOCK ALERT PILL)
                            val buy = ShopLocale.toAsciiDigits(buyPriceInput).toDoubleOrNull() ?: 0.0
                            val sell = ShopLocale.toAsciiDigits(sellPriceInput).toDoubleOrNull() ?: 0.0
                            val parsedConvForPricing = if (buyUnit.equals(selectedUnit, ignoreCase = true)) {
                                1.0
                            } else {
                                (ShopLocale.toAsciiDigits(buyConversionInput).toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0001)
                            }
                            val profitPerPiece = CalculationEngine.calculateProfitPerPiece(sell, buy)
                            val margin = CalculationEngine.calculateMarginPercent(sell, buy)
                            val isNegativeProfit = profitPerPiece < 0.0
                            val accentColor = if (isNegativeProfit) AlertRed else VividGreen
                            val unitDisplayName = ShopLocale.displayUnit(selectedUnit, currentLanguage)

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Centered Frosted Glass Product Photo / Camera Orb with Edit Badge (tap to jump to Basics to change photo)
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clickable { step = 1 }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Transparent,
                                            border = BorderStroke(
                                                1.2.dp,
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0x58FFFFFF), Color(0x18FFFFFF))
                                                )
                                            ),
                                            modifier = Modifier
                                                .size(92.dp)
                                                .align(Alignment.Center)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(Color(0x543D433D), Color(0x3C181B18))
                                                    )
                                                )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (photoUriString.isNotBlank()) {
                                                    ProductThumbnail(
                                                        photoUri = photoUriString,
                                                        category = selectedCategory,
                                                        contentDescription = name.ifBlank { "Product photo" },
                                                        storeId = productToEdit?.storeId ?: "",
                                                        productId = productToEdit?.id ?: "",
                                                        modifier = Modifier.fillMaxSize(),
                                                        iconSize = 32.dp,
                                                        cornerRadius = 46.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PhotoCamera,
                                                        contentDescription = "Edit product photo",
                                                        tint = Color(0xB8FFFFFF),
                                                        modifier = Modifier.size(38.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Bottom-right circular edit badge matching reference
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF232623),
                                            border = BorderStroke(1.dp, Color(0x45FFFFFF)),
                                            modifier = Modifier
                                                .size(30.dp)
                                                .align(Alignment.BottomEnd)
                                                .shadow(6.dp, CircleShape)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD8B982),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pricing Heading
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "মূল্য নির্ধারণ" else "Pricing",
                                    color = TextPrimary,
                                    fontSize = if (isFatherMode) 32.sp else 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Buy Price & Sell Price Big Glass Input Tiles
                                if (showCostAndProfit) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        PricingBigGlassInputTile(
                                            label = if (currentLanguage == AppLanguage.BN) "ক্রয় মূল্য ($unitDisplayName)" else "BUY PRICE ($unitDisplayName)",
                                            value = buyPriceInput,
                                            onValueChange = {
                                                buyPriceInput = it
                                                stepError = null
                                            },
                                            testTag = "wizard_buy_price",
                                            modifier = Modifier.weight(1f)
                                        )

                                        PricingBigGlassInputTile(
                                            label = if (currentLanguage == AppLanguage.BN) "বিক্রয় মূল্য ($unitDisplayName)" else "SELL PRICE ($unitDisplayName)",
                                            value = sellPriceInput,
                                            onValueChange = {
                                                sellPriceInput = it
                                                stepError = null
                                            },
                                            testTag = "wizard_sell_price",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    PricingBigGlassInputTile(
                                        label = if (currentLanguage == AppLanguage.BN) "বিক্রয় মূল্য ($unitDisplayName)" else "SELL PRICE ($unitDisplayName)",
                                        value = sellPriceInput,
                                        onValueChange = {
                                            sellPriceInput = it
                                            stepError = null
                                        },
                                        testTag = "wizard_sell_price",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Glowing Live Profit & Margin Card with Progress Bar
                                if (showCostAndProfit) {
                                    val heroShape = RoundedCornerShape(22.dp)
                                    val animatedMarginProgress by animateFloatAsState(
                                        targetValue = (margin.toFloat() / 100f).coerceIn(0f, 1f),
                                        label = "wizardMarginBar"
                                    )
                                    val profitFormatted = formatHomeMoney(profitPerPiece, currentLanguage, false)
                                    val profitFontSize = when {
                                        profitFormatted.length <= 6 -> 27.sp
                                        profitFormatted.length <= 9 -> 23.sp
                                        else -> 19.sp
                                    }
                                    val marginFormatted = ShopLocale.formatPercent(margin, 1, currentLanguage)

                                    Surface(
                                        shape = heroShape,
                                        color = Color.Transparent,
                                        border = BorderStroke(1.4.dp, accentColor.copy(alpha = 0.88f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = 20.dp,
                                                shape = heroShape,
                                                spotColor = accentColor.copy(alpha = 0.45f),
                                                ambientColor = accentColor.copy(alpha = 0.25f)
                                            )
                                            .clip(heroShape)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = if (isNegativeProfit) {
                                                        listOf(Color(0xFF3E1818), Color(0xFF170B0B))
                                                    } else {
                                                        listOf(Color(0xFF284F18), Color(0xFF10190D))
                                                    }
                                                )
                                            )
                                            .testTag("wizard_profit_hero_card")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1.1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (currentLanguage == AppLanguage.BN) "প্রতি $unitDisplayName লাভ" else "Profit per $unitDisplayName",
                                                        color = Color(0xFFD6E2D2),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = profitFormatted,
                                                        color = accentColor,
                                                        fontSize = profitFontSize,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.testTag("wizard_live_profit_value")
                                                    )
                                                }

                                                Column(
                                                    modifier = Modifier.weight(0.9f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (currentLanguage == AppLanguage.BN) "মার্জিন" else "Margin",
                                                            color = Color(0xFFD6E2D2),
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = Color(0xB8FFFFFF),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = marginFormatted,
                                                        color = TextPrimary,
                                                        fontSize = 25.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.testTag("wizard_live_margin_value")
                                                    )
                                                }
                                            }
                                            if (!buyUnit.equals(selectedUnit, ignoreCase = true) && buy > 0.0) {
                                                val boxCost = CalculationEngine.buyPriceInBuyUnit(buy, parsedConvForPricing)
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.BN) {
                                                        "প্রতি ${ShopLocale.displayUnit(buyUnit, currentLanguage)} খরচ: ${ShopLocale.formatMoney(boxCost, currentLanguage)} • প্রতি $unitDisplayName খরচ: ${ShopLocale.formatMoney(buy, currentLanguage)}"
                                                    } else {
                                                        "Cost per $buyUnit: ${ShopLocale.formatMoney(boxCost, currentLanguage)} • Cost per $unitDisplayName: ${ShopLocale.formatMoney(buy, currentLanguage)}"
                                                    },
                                                    color = Color(0xFFB5C7B0),
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Live Margin Progress Bar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x28FFFFFF))
                                            ) {
                                                if (animatedMarginProgress > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(fraction = animatedMarginProgress)
                                                            .clip(CircleShape)
                                                            .background(
                                                                brush = Brush.horizontalGradient(
                                                                    colors = listOf(
                                                                        accentColor,
                                                                        if (isNegativeProfit) Color(0xFFFF7979) else Color(0xFF93FA69)
                                                                    )
                                                                )
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Wholesale, MRP & Party Rate — ONLY in Detailed Mode
                                if (!isSimpleMode) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("wizard_detailed_pricing_card"),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            PricingBigGlassInputTile(
                                                label = if (currentLanguage == AppLanguage.BN) "পাইকারি" else "WHOLESALE",
                                                value = wholesalePriceInput,
                                                onValueChange = {
                                                    wholesalePriceInput = it
                                                    stepError = null
                                                },
                                                testTag = "wizard_wholesale_price",
                                                modifier = Modifier.weight(1f)
                                            )

                                            PricingBigGlassInputTile(
                                                label = if (currentLanguage == AppLanguage.BN) "সর্বোচ্চ খুচরা" else "MRP",
                                                value = mrpInput,
                                                onValueChange = {
                                                    mrpInput = it
                                                    stepError = null
                                                },
                                                testTag = "wizard_mrp",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        PricingBigGlassInputTile(
                                            label = if (currentLanguage == AppLanguage.BN) "পার্টি রেট (ঐচ্ছিক)" else "PARTY RATE (OPTIONAL)",
                                            value = partyRateInput,
                                            onValueChange = {
                                                partyRateInput = it
                                                stepError = null
                                            },
                                            testTag = "wizard_party_rate",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Low Stock Alert Stepper Pill
                                val currentLowLimit = (ShopLocale.toAsciiDigits(lowStockLimitInput).toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0)
                                val alertPillShape = RoundedCornerShape(22.dp)
                                Surface(
                                    shape = alertPillShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0x36FFFFFF), Color(0x14FFFFFF))
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(alertPillShape)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xEB222522), Color(0xEC141614))
                                            )
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "কম স্টক সতর্কতা" else "Low stock alert at",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0x382D322D),
                                            border = BorderStroke(1.dp, Color(0x28FFFFFF))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        val next = CalculationEngine.roundStock((currentLowLimit - 1.0).coerceAtLeast(0.0))
                                                        lowStockLimitInput = ShopLocale.formatNumber(next, AppLanguage.EN)
                                                    },
                                                    shape = CircleShape,
                                                    color = Color(0x24FFFFFF),
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("wizard_low_limit_minus")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Decrease alert limit",
                                                            tint = TextPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = ShopLocale.formatNumber(currentLowLimit, currentLanguage),
                                                        color = TextPrimary,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Text(
                                                        text = selectedUnit,
                                                        color = TextSecondary,
                                                        fontSize = 12.5.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                Surface(
                                                    onClick = {
                                                        val next = CalculationEngine.roundStock(currentLowLimit + 1.0)
                                                        lowStockLimitInput = ShopLocale.formatNumber(next, AppLanguage.EN)
                                                    },
                                                    shape = CircleShape,
                                                    color = Color(0x24FFFFFF),
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("wizard_low_limit_plus")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Increase alert limit",
                                                            tint = TextPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // STEP 3: STOCK & REVIEW
                            val buy = ShopLocale.toAsciiDigits(buyPriceInput).toDoubleOrNull() ?: 0.0
                            val sell = ShopLocale.toAsciiDigits(sellPriceInput).toDoubleOrNull() ?: 0.0
                            val qty = CalculationEngine.roundStock((ShopLocale.toAsciiDigits(stockInput).toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
                            val lowLimit = CalculationEngine.roundStock((ShopLocale.toAsciiDigits(lowStockLimitInput).toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0))
                            val parsedBuyConv = if (buyUnit.equals(selectedUnit, ignoreCase = true)) {
                                1.0
                            } else {
                                CalculationEngine.roundStock((ShopLocale.toAsciiDigits(buyConversionInput).toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0001))
                            }

                            Column {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "স্টক ও পর্যালোচনা" else "Stock & Review",
                                        color = TextPrimary,
                                        fontSize = if (isFatherMode) 26.sp else 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "ধাপ ৩ / ৩: স্টক ও পর্যালোচনা" else "Step 3 of 3: Stock & Review",
                                        color = VividGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GlassTextField(
                                    value = stockInput,
                                    onValueChange = {
                                        stockInput = it
                                        stepError = null
                                    },
                                    label = if (currentLanguage == AppLanguage.BN) "স্টকের পরিমাণ (${ShopLocale.displayUnit(selectedUnit, currentLanguage)})" else "Stock Quantity ($selectedUnit)",
                                    placeholder = "10",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    testTag = "wizard_stock_qty"
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                GlassTextField(
                                    value = lowStockLimitInput,
                                    onValueChange = {
                                        lowStockLimitInput = it
                                        stepError = null
                                    },
                                    label = if (currentLanguage == AppLanguage.BN) "কম স্টক সতর্কতা সীমা (${ShopLocale.displayUnit(selectedUnit, currentLanguage)})" else "Low-Stock Alert Limit ($selectedUnit)",
                                    placeholder = "5",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    testTag = "wizard_low_limit"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Review Card
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "সারসংক্ষেপ" else "Summary Review",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "$name (${ShopLocale.displayCategory(selectedCategory, currentLanguage)} · ${ShopLocale.displayUnit(selectedUnit, currentLanguage)})",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!buyUnit.equals(selectedUnit, ignoreCase = true)) {
                                            Text(
                                                text = "${if (currentLanguage == AppLanguage.BN) "ক্রয়" else "Buy"}: 1 ${ShopLocale.displayUnit(buyUnit, currentLanguage)} = ${ShopLocale.formatNumber(parsedBuyConv, currentLanguage)} ${ShopLocale.displayUnit(selectedUnit, currentLanguage)}",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (sellUnitInputs.isNotEmpty()) {
                                            val sellSummary = sellUnitInputs.entries.joinToString(", ") { (su, f) ->
                                                "1 ${ShopLocale.displayUnit(selectedUnit, currentLanguage)} = ${f.ifBlank { "?" }} ${ShopLocale.displayUnit(su, currentLanguage)}"
                                            }
                                            Text(
                                                text = "${if (currentLanguage == AppLanguage.BN) "বিক্রয় একক" else "Sell"}: $sellSummary",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val priceReviewText = if (showCostAndProfit) {
                                            if (currentLanguage == AppLanguage.BN) {
                                                "ক্রয়: ${ShopLocale.formatMoney(buy, currentLanguage)} | বিক্রয়: ${ShopLocale.formatMoney(sell, currentLanguage)}"
                                            } else {
                                                "Buy: ${ShopLocale.formatMoney(buy, currentLanguage)} | Sell: ${ShopLocale.formatMoney(sell, currentLanguage)}"
                                            }
                                        } else {
                                            "${if (currentLanguage == AppLanguage.BN) "বিক্রয় মূল্য" else "Sale Price"}: ${ShopLocale.formatMoney(sell, currentLanguage)}"
                                        }
                                        Text(
                                            text = priceReviewText,
                                            color = VividGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) {
                                                "স্টক: ${ShopLocale.formatNumber(qty, currentLanguage)} ${ShopLocale.displayUnit(selectedUnit, currentLanguage)} (সতর্কতা: ${ShopLocale.formatNumber(lowLimit, currentLanguage)})"
                                            } else {
                                                "Stock: ${ShopLocale.formatNumber(qty, currentLanguage)} $selectedUnit (Low: ${ShopLocale.formatNumber(lowLimit, currentLanguage)})"
                                            },
                                            color = TextMuted,
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
                    Spacer(modifier = Modifier.height(12.dp))
                    }

                    val displayedError = stepError ?: externalError
                    if (!displayedError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayedError,
                            color = AlertRed,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Pill Action Buttons (matching Image 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (step > 1) {
                            Surface(
                                onClick = {
                                    step -= 1
                                    stepError = null
                                },
                                shape = RoundedCornerShape(28.dp),
                                color = Color(0xEB1D201D),
                                border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ShopLocale.get("back", currentLanguage),
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (step < 3) {
                            val nextShape = RoundedCornerShape(28.dp)
                            Surface(
                                onClick = {
                                    if (step == 1) {
                                        if (name.isBlank()) {
                                            stepError = if (currentLanguage == AppLanguage.BN) "পণ্যের নাম লিখুন" else "Please enter product name"
                                            return@Surface
                                        }
                                        if (!buyUnit.equals(selectedUnit, ignoreCase = true)) {
                                            val convVal = ShopLocale.toAsciiDigits(buyConversionInput).toDoubleOrNull()
                                            if (convVal == null || convVal <= 0.0) {
                                                stepError = if (currentLanguage == AppLanguage.BN) "রূপান্তর মান ০-এর বেশি হতে হবে" else "Conversion must be greater than 0"
                                                return@Surface
                                            }
                                        }
                                        val invalidSell = sellUnitInputs.entries.any { (_, v) ->
                                            val f = ShopLocale.toAsciiDigits(v).toDoubleOrNull()
                                            f == null || f <= 0.0
                                        }
                                        if (invalidSell) {
                                            stepError = if (currentLanguage == AppLanguage.BN) "বিক্রয় এককের মান ০-এর বেশি হতে হবে" else "Selling unit conversion must be > 0"
                                            return@Surface
                                        }
                                    }
                                    if (step == 2) {
                                        val buy = if (showCostAndProfit) {
                                            ShopLocale.toAsciiDigits(buyPriceInput).toDoubleOrNull()
                                        } else {
                                            productToEdit?.buyPrice ?: 0.0
                                        }
                                        val sell = ShopLocale.toAsciiDigits(sellPriceInput).toDoubleOrNull()
                                        if (buy == null || sell == null || buy < 0.0 || sell < 0.0) {
                                            stepError = if (showCostAndProfit) {
                                                if (currentLanguage == AppLanguage.BN) "সঠিক ক্রয় ও বিক্রয় মূল্য লিখুন" else "Enter valid cost and selling prices"
                                            } else {
                                                if (currentLanguage == AppLanguage.BN) "সঠিক বিক্রয় মূল্য লিখুন" else "Enter a valid selling price"
                                            }
                                            return@Surface
                                        }
                                        if (!isSimpleMode) {
                                            val wholesale = if (wholesalePriceInput.isBlank()) 0.0 else ShopLocale.toAsciiDigits(wholesalePriceInput).toDoubleOrNull()
                                            val mrpVal = if (mrpInput.isBlank()) 0.0 else ShopLocale.toAsciiDigits(mrpInput).toDoubleOrNull()
                                            val partyVal = if (partyRateInput.isBlank()) 0.0 else ShopLocale.toAsciiDigits(partyRateInput).toDoubleOrNull()
                                            if (wholesale == null || wholesale < 0.0 || mrpVal == null || mrpVal < 0.0 || partyVal == null || partyVal < 0.0) {
                                                stepError = if (currentLanguage == AppLanguage.BN) "ঐচ্ছিক মূল্য সঠিকভাবে লিখুন" else "Optional prices must be valid numbers"
                                                return@Surface
                                            }
                                        }
                                    }
                                    step += 1
                                    stepError = null
                                },
                                shape = nextShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0x88FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = nextShape,
                                        spotColor = VividGreen,
                                        ambientColor = VividGreen
                                    )
                                    .clip(nextShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF7CEF4F), VividGreen)
                                        )
                                    )
                                    .testTag("wizard_next_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "পরবর্তী" else "Next",
                                        color = Color(0xFF0A1406),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            val saveShape = RoundedCornerShape(28.dp)
                            Surface(
                                onClick = {
                                    if (!isBusy) {
                                        val qtyParsed = ShopLocale.toAsciiDigits(stockInput).toDoubleOrNull()
                                        val lowParsed = if (lowStockLimitInput.isBlank()) 5.0 else ShopLocale.toAsciiDigits(lowStockLimitInput).toDoubleOrNull()
                                        if (qtyParsed == null || qtyParsed < 0.0) {
                                            stepError = if (currentLanguage == AppLanguage.BN) "স্টক নেতিবাচক হতে পারবে না" else "Stock cannot be negative"
                                            return@Surface
                                        }
                                        if (lowParsed == null || lowParsed < 0.0) {
                                            stepError = if (currentLanguage == AppLanguage.BN) "কম স্টক সীমা নেতিবাচক হতে পারবে না" else "Low stock limit cannot be negative"
                                            return@Surface
                                        }
                                        val buy = if (showCostAndProfit) {
                                            ShopLocale.toAsciiDigits(buyPriceInput).toDoubleOrNull() ?: 0.0
                                        } else {
                                            productToEdit?.buyPrice ?: 0.0
                                        }
                                        val sell = ShopLocale.toAsciiDigits(sellPriceInput).toDoubleOrNull() ?: 0.0
                                        val qty = CalculationEngine.roundStock(qtyParsed)
                                        val lowLimit = CalculationEngine.roundStock(lowParsed)
                                        val wholesale = ShopLocale.toAsciiDigits(wholesalePriceInput).toDoubleOrNull() ?: 0.0
                                        val mrpVal = ShopLocale.toAsciiDigits(mrpInput).toDoubleOrNull() ?: 0.0
                                        val partyVal = ShopLocale.toAsciiDigits(partyRateInput).toDoubleOrNull() ?: 0.0
                                        val resolvedBuyUnit = buyUnit.trim().ifBlank { selectedUnit }
                                        val resolvedBuyConv = if (resolvedBuyUnit.equals(selectedUnit, ignoreCase = true)) {
                                            1.0
                                        } else {
                                            CalculationEngine.roundStock((ShopLocale.toAsciiDigits(buyConversionInput).toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0001))
                                        }
                                        val resolvedSellUnits = sellUnitInputs.mapNotNull { (su, factorStr) ->
                                            val f = ShopLocale.toAsciiDigits(factorStr).toDoubleOrNull()
                                            if (su.isNotBlank() && !su.equals(selectedUnit, ignoreCase = true) && f != null && f > 0.0) {
                                                su.trim() to CalculationEngine.roundStock(f)
                                            } else null
                                        }.toMap()
                                        val submitAction: () -> Unit = {
                                            onConfirm(
                                                name,
                                                nameBn,
                                                selectedCategory,
                                                selectedUnit,
                                                buy,
                                                sell,
                                                qty,
                                                lowLimit,
                                                photoUriString,
                                                barcodeInput.trim(),
                                                wholesale,
                                                mrpVal,
                                                itemLocationInput.trim(),
                                                partyVal,
                                                resolvedBuyUnit,
                                                resolvedBuyConv,
                                                resolvedSellUnits
                                            )
                                        }
                                        if (isFatherMode) {
                                            val orig = productToEdit
                                            val priceChanged = orig != null && (
                                                abs(buy - orig.buyPrice) > 0.001 ||
                                                    abs(sell - orig.sellPrice) > 0.001 ||
                                                    abs(wholesale - orig.wholesalePrice) > 0.001 ||
                                                    abs(mrpVal - orig.mrp) > 0.001 ||
                                                    abs(partyVal - orig.partyRate) > 0.001
                                                )
                                            val stockLowered = orig != null && qty < (orig.stock - 0.0001)
                                            pendingSaveAction = submitAction
                                            pendingSaveSummary = "$name · ${ShopLocale.formatNumber(qty, currentLanguage)} ${ShopLocale.displayUnit(selectedUnit, currentLanguage)} · ${ShopLocale.formatMoney(sell, currentLanguage)}"
                                            if (priceChanged || stockLowered) {
                                                fatherPinReasonDesc = when {
                                                    priceChanged && stockLowered -> ShopLocale.get("father_mode_pin_price_and_stock_desc", currentLanguage)
                                                    priceChanged -> ShopLocale.get("father_mode_pin_price_desc", currentLanguage)
                                                    else -> ShopLocale.get("father_mode_pin_stock_desc", currentLanguage)
                                                }
                                                showFatherPinDialog = true
                                            } else {
                                                showFatherConfirmSave = true
                                            }
                                        } else {
                                            submitAction()
                                        }
                                    }
                                },
                                shape = saveShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0x88FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isFatherMode) 58.dp else 52.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = saveShape,
                                        spotColor = VividGreen,
                                        ambientColor = VividGreen
                                    )
                                    .clip(saveShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF7CEF4F), VividGreen)
                                        )
                                    )
                                    .testTag("wizard_save_product_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF0A1406),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF0A1406),
                                                modifier = Modifier.size(if (isFatherMode) 20.dp else 18.dp)
                                            )
                                            Text(
                                                text = when {
                                                    isEditMode && currentLanguage == AppLanguage.BN -> "পরিবর্তন সংরক্ষণ"
                                                    isEditMode -> "Save Changes"
                                                    currentLanguage == AppLanguage.BN -> "পণ্য সংরক্ষণ"
                                                    else -> "Save Product"
                                                },
                                                color = Color(0xFF0A1406),
                                                fontSize = if (isFatherMode) 16.5.sp else 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFatherPinDialog) {
        BackupPinDialog(
            mode = if (hasOwnerPin) BackupPinDialogMode.ENTER_PIN else BackupPinDialogMode.SET_PIN,
            currentLanguage = currentLanguage,
            customDescription = fatherPinReasonDesc,
            onSetPin = { pin, _ ->
                if (onSetOwnerPin != null) {
                    onSetOwnerPin.invoke(pin) {
                        showFatherPinDialog = false
                        showFatherConfirmSave = true
                    }
                } else {
                    showFatherPinDialog = false
                    showFatherConfirmSave = true
                }
            },
            onVerifyPin = { pin, onResult ->
                if (onVerifyOwnerPin != null) {
                    onVerifyOwnerPin.invoke(pin) { ok ->
                        onResult(ok)
                        if (ok) {
                            showFatherPinDialog = false
                            showFatherConfirmSave = true
                        }
                    }
                } else {
                    onResult(false)
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = {
                showFatherPinDialog = false
                pendingSaveAction = null
            }
        )
    }

    if (showFatherConfirmSave) {
        FatherConfirmActionDialog(
            title = if (isEditMode) {
                ShopLocale.get("father_confirm_edit_title", currentLanguage)
            } else {
                ShopLocale.get("father_confirm_save_title", currentLanguage)
            },
            body = pendingSaveSummary,
            confirmLabel = ShopLocale.get("confirm", currentLanguage),
            currentLanguage = currentLanguage,
            onConfirm = {
                showFatherConfirmSave = false
                val action = pendingSaveAction
                pendingSaveAction = null
                action?.invoke()
            },
            onDismiss = {
                showFatherConfirmSave = false
                pendingSaveAction = null
            }
        )
    }
    }
}

@Composable
private fun WizardStepperHeader(
    currentStep: Int,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val steps = if (currentLanguage == AppLanguage.BN) {
        listOf(1 to "মূল তথ্য", 2 to "মূল্য", 3 to "স্টক")
    } else {
        listOf(1 to "Basics", 2 to "Pricing", 3 to "Stock")
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (stepIndex, label) ->
                val isCompleted = stepIndex < currentStep
                val isActive = stepIndex == currentStep
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = CircleShape,
                                        spotColor = VividGreen,
                                        ambientColor = VividGreen
                                    )
                                    .clip(CircleShape)
                                    .background(VividGreen.copy(alpha = 0.34f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> TextPrimary
                                        isActive -> VividGreen
                                        else -> Color(0x2AFFFFFF)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isCompleted -> TextPrimary
                                        isActive -> Color(0xCCFFFFFF)
                                        else -> Color(0x38FFFFFF)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCompleted -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0A1406),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                isActive -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x66FFFFFF))
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = label,
                        color = if (isActive) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 18.dp, start = 4.dp, end = 4.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                if (stepIndex < currentStep) TextPrimary else Color(0x28FFFFFF)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingBigGlassInputTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val tileShape = RoundedCornerShape(20.dp)
    val valueFontSize = when {
        value.length <= 6 -> 26.sp
        value.length <= 8 -> 21.sp
        else -> 17.sp
    }

    Surface(
        onClick = { focusRequester.requestFocus() },
        shape = tileShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isFocused) 1.4.dp else 1.dp,
            brush = if (isFocused) {
                Brush.verticalGradient(listOf(VividGreen.copy(alpha = 0.85f), VividGreen.copy(alpha = 0.35f)))
            } else {
                Brush.verticalGradient(listOf(Color(0x4EFFFFFF), Color(0x16FFFFFF)))
            }
        ),
        modifier = modifier
            .clip(tileShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB252925), Color(0xEE131513))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = ShopLocale.CURRENCY,
                    color = if (value.isBlank()) TextMuted else TextPrimary,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .defaultMinSize(minWidth = 22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            color = TextMuted,
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(VividGreen),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier
                            .widthIn(min = 22.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag(testTag)
                    )
                }
            }
        }
    }
}

private fun formatEditablePrice(amount: Double): String {
    if (amount == 0.0) return "0"
    val roundedLong = amount.toLong()
    return if (kotlin.math.abs(amount - roundedLong.toDouble()) < 0.005) {
        roundedLong.toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", amount)
            .trimEnd('0')
            .trimEnd('.')
    }
}

@Composable
private fun rememberHostActivitySafeWindowInsets(): WindowInsets {
    val context = LocalContext.current
    val hostDecorView = remember(context) {
        tailrec fun findActivity(ctx: android.content.Context): android.app.Activity? = when (ctx) {
            is android.app.Activity -> ctx
            is android.content.ContextWrapper -> findActivity(ctx.baseContext)
            else -> null
        }
        findActivity(context)?.window?.decorView
    }
    var rawInsets by remember(hostDecorView) {
        mutableStateOf(hostDecorView?.let { ViewCompat.getRootWindowInsets(it) })
    }
    DisposableEffect(hostDecorView) {
        if (hostDecorView == null) {
            onDispose {}
        } else {
            val refresh = {
                rawInsets = ViewCompat.getRootWindowInsets(hostDecorView)
            }
            refresh()
            val layoutListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                refresh()
            }
            hostDecorView.addOnLayoutChangeListener(layoutListener)
            onDispose {
                hostDecorView.removeOnLayoutChangeListener(layoutListener)
            }
        }
    }
    val compat = rawInsets ?: return WindowInsets(0, 0, 0, 0)
    val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    val visibleInsets = compat.getInsets(typeMask)
    val imeInsets = compat.getInsets(WindowInsetsCompat.Type.ime())
    return WindowInsets(
        left = maxOf(visibleInsets.left, imeInsets.left),
        top = visibleInsets.top,
        right = maxOf(visibleInsets.right, imeInsets.right),
        bottom = maxOf(visibleInsets.bottom, imeInsets.bottom)
    )
}
