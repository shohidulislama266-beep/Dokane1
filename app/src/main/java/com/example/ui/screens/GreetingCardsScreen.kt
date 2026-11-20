package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCardDraft
import com.example.data.model.BusinessCardFontPair
import com.example.data.model.BusinessCardFontSize
import com.example.data.model.BusinessCardLayout
import com.example.data.model.BusinessCardPalette
import com.example.data.model.BusinessCardPalettes
import com.example.data.model.BusinessCardSide
import com.example.data.model.BusinessCardTextAlign
import com.example.data.model.SavedBusinessCard
import com.example.domain.BusinessCardPaletteEngine
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.BusinessCardLocalStore
import com.example.util.BusinessCardRenderer
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun showToastOnMain(context: Context, message: String, duration: Int) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(context, message, duration).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, duration).show()
        }
    }
}

private enum class GreetingCardsTab {
    BUSINESS_CARD,
    MY_CARDS
}

@Composable
fun GreetingCardsScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val profileSeed by storeViewModel.businessCardProfileSeed.collectAsState()

    var activeTab by rememberSaveable { mutableStateOf(GreetingCardsTab.BUSINESS_CARD) }
    var draft by remember {
        mutableStateOf(
            BusinessCardDraft().appliedProfile(profileSeed)
        )
    }
    var hasUserEditedFields by rememberSaveable { mutableStateOf(false) }
    var savedCards by remember {
        mutableStateOf(BusinessCardLocalStore.loadSavedCards(context))
    }
    var showShareFormatDialog by remember { mutableStateOf(false) }
    var shareTargetDraft by remember { mutableStateOf<BusinessCardDraft?>(null) }

    // Keep auto-fill synchronized with async Store load until the user edits any field or loads a card
    LaunchedEffect(profileSeed) {
        if (!hasUserEditedFields) {
            draft = draft.appliedProfile(profileSeed)
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val localUri = withContext(Dispatchers.IO) {
                    runCatching { BusinessCardLocalStore.importLogoImage(context, uri) }.getOrNull()
                }
                if (!localUri.isNullOrBlank()) {
                    hasUserEditedFields = true
                    draft = draft.copy(logoUri = localUri)
                } else {
                    showToastOnMain(
                        context,
                        ShopLocale.get("err_open_image", currentLanguage),
                        Toast.LENGTH_SHORT
                    )
                }
            }
        }
    }

    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val localUri = withContext(Dispatchers.IO) {
                    runCatching { BusinessCardLocalStore.importAndCropBackground(context, uri) }.getOrNull()
                }
                if (!localUri.isNullOrBlank()) {
                    hasUserEditedFields = true
                    draft = draft.copy(backgroundImageUri = localUri)
                } else {
                    showToastOnMain(
                        context,
                        ShopLocale.get("err_open_image", currentLanguage),
                        Toast.LENGTH_SHORT
                    )
                }
            }
        }
    }

    fun exportPngAndNotify(target: BusinessCardDraft) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BusinessCardRenderer.exportPng(context, target) }.getOrNull()
            }
            val msg = if (result != null) {
                ShopLocale.format("bc_png_saved_msg", currentLanguage, result.file.name)
            } else {
                ShopLocale.get("bc_export_err", currentLanguage)
            }
            showToastOnMain(context, msg, Toast.LENGTH_LONG)
        }
    }

    fun exportPdfAndNotify(target: BusinessCardDraft) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BusinessCardRenderer.exportPdf(context, target) }.getOrNull()
            }
            val msg = if (result != null) {
                ShopLocale.format("bc_pdf_saved_msg", currentLanguage, result.file.name)
            } else {
                ShopLocale.get("bc_export_err", currentLanguage)
            }
            showToastOnMain(context, msg, Toast.LENGTH_LONG)
            if (result != null) {
                BusinessCardRenderer.openPdfWithChooser(context, result.file, currentLanguage)
            }
        }
    }

    fun shareCard(target: BusinessCardDraft, asPdf: Boolean) {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    if (asPdf) {
                        BusinessCardRenderer.exportPdf(context, target).file
                    } else {
                        BusinessCardRenderer.exportPng(context, target).file
                    }
                }.getOrNull()
            }
            if (file != null) {
                BusinessCardRenderer.shareCardFile(
                    context = context,
                    file = file,
                    mimeType = if (asPdf) "application/pdf" else "image/png",
                    subject = target.shopName.ifBlank { ShopLocale.get("bc_tab_business_card", currentLanguage) },
                    lang = currentLanguage
                )
            } else {
                showToastOnMain(
                    context,
                    ShopLocale.get("bc_export_err", currentLanguage),
                    Toast.LENGTH_SHORT
                )
            }
        }
    }

    val topHeaderAndTabs: LazyListScope.() -> Unit = {
        item(key = "greeting_cards_top_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header (scrolls away with list)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("greeting_cards_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ShopLocale.get("back", currentLanguage),
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = ShopLocale.get("greeting_cards_title", currentLanguage),
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tab Strip: Business Card | My Cards (single-line labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioTabButton(
                        label = ShopLocale.get("bc_tab_business_card", currentLanguage),
                        selected = activeTab == GreetingCardsTab.BUSINESS_CARD,
                        onClick = { activeTab = GreetingCardsTab.BUSINESS_CARD },
                        testTag = "greeting_tab_business_card",
                        modifier = Modifier.weight(1f)
                    )
                    StudioTabButton(
                        label = if (savedCards.isNotEmpty()) {
                            ShopLocale.format("bc_tab_my_cards_count", currentLanguage, savedCards.size)
                        } else {
                            ShopLocale.get("bc_tab_my_cards", currentLanguage)
                        },
                        selected = activeTab == GreetingCardsTab.MY_CARDS,
                        onClick = {
                            savedCards = BusinessCardLocalStore.loadSavedCards(context)
                            activeTab = GreetingCardsTab.MY_CARDS
                        },
                        testTag = "greeting_tab_my_cards",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(VividGreen.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.10f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        when (activeTab) {
            GreetingCardsTab.BUSINESS_CARD -> {
                BusinessCardStudioContent(
                    headerSlot = topHeaderAndTabs,
                    draft = draft,
                    currentLanguage = currentLanguage,
                    onUpdateDraft = { updated, markEdited ->
                        if (markEdited) hasUserEditedFields = true
                        draft = updated
                    },
                    onResetToProfile = {
                        hasUserEditedFields = false
                        draft = draft.appliedProfile(profileSeed)
                        showToastOnMain(
                            context,
                            ShopLocale.get("bc_profile_reset_msg", currentLanguage),
                            Toast.LENGTH_SHORT
                        )
                    },
                    onUploadLogo = {
                        logoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUploadBackground = {
                        bgPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSaveToMyCards = {
                        val saved = BusinessCardLocalStore.saveCard(context, draft)
                        draft = saved.draft
                        savedCards = BusinessCardLocalStore.loadSavedCards(context)
                        showToastOnMain(
                            context,
                            ShopLocale.get("bc_saved_msg", currentLanguage),
                            Toast.LENGTH_SHORT
                        )
                    },
                    onDownloadPng = { exportPngAndNotify(draft) },
                    onDownloadPdf = { exportPdfAndNotify(draft) },
                    onShare = {
                        shareTargetDraft = draft
                        showShareFormatDialog = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            GreetingCardsTab.MY_CARDS -> {
                MySavedCardsContent(
                    headerSlot = topHeaderAndTabs,
                    cards = savedCards,
                    currentLanguage = currentLanguage,
                    onLoadCard = { card ->
                        hasUserEditedFields = true
                        draft = card.draft
                        activeTab = GreetingCardsTab.BUSINESS_CARD
                        showToastOnMain(
                            context,
                            ShopLocale.get("bc_loaded_msg", currentLanguage),
                            Toast.LENGTH_SHORT
                        )
                    },
                    onDeleteCard = { card ->
                        savedCards = BusinessCardLocalStore.deleteCard(context, card.id)
                        if (draft.cardId == card.id) {
                            draft = draft.copy(cardId = "")
                        }
                        showToastOnMain(
                            context,
                            ShopLocale.get("bc_deleted_msg", currentLanguage),
                            Toast.LENGTH_SHORT
                        )
                    },
                    onShareCard = { card ->
                        shareTargetDraft = card.draft
                        showShareFormatDialog = true
                    },
                    onDownloadPng = { card -> exportPngAndNotify(card.draft) },
                    onDownloadPdf = { card -> exportPdfAndNotify(card.draft) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showShareFormatDialog) {
        val target = shareTargetDraft ?: draft
        AlertDialog(
            onDismissRequest = { showShareFormatDialog = false },
            properties = edgeToEdgeDialogProperties(),
            modifier = Modifier.safeDialogWindowPadding(),
            containerColor = Color(0xFF1A1E1A),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = ShopLocale.get("bc_share_chooser", currentLanguage),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showShareFormatDialog = false
                            shareCard(target, asPdf = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VividGreen, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_share_png_option")
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PNG (1050×600)",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            showShareFormatDialog = false
                            shareCard(target, asPdf = true)
                        },
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_share_pdf_option")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PDF (3.5×2 in)",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareFormatDialog = false }) {
                    Text(
                        text = ShopLocale.get("cancel", currentLanguage),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        )
    }
}

@Composable
private fun BusinessCardStudioContent(
    headerSlot: LazyListScope.() -> Unit,
    draft: BusinessCardDraft,
    currentLanguage: AppLanguage,
    onUpdateDraft: (BusinessCardDraft, Boolean) -> Unit,
    onResetToProfile: () -> Unit,
    onUploadLogo: () -> Unit,
    onUploadBackground: () -> Unit,
    onSaveToMyCards: () -> Unit,
    onDownloadPng: () -> Unit,
    onDownloadPdf: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bc_studio_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        headerSlot()
        // 1a. Preview Header + Front/Back Switch
        item(key = "bc_preview_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ShopLocale.get("bc_section_preview", currentLanguage),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactChip(
                        label = ShopLocale.get("bc_side_front", currentLanguage),
                        selected = draft.side == BusinessCardSide.FRONT,
                        onClick = { onUpdateDraft(draft.copy(side = BusinessCardSide.FRONT), false) },
                        testTag = "bc_side_front"
                    )
                    CompactChip(
                        label = ShopLocale.get("bc_side_back", currentLanguage),
                        selected = draft.side == BusinessCardSide.BACK,
                        onClick = { onUpdateDraft(draft.copy(side = BusinessCardSide.BACK), false) },
                        testTag = "bc_side_back"
                    )
                }
            }
        }

        // 1b. Live 3.5 x 2 Card Preview
        item(key = "bc_preview_card") {
            BusinessCardCanvasPreview(
                draft = draft,
                currentLanguage = currentLanguage
            )
        }

        // 1c. 3 Layouts (Left, Centered, Split)
        item(key = "bc_layout_selector") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactChip(
                    label = ShopLocale.get("bc_layout_left", currentLanguage),
                    selected = draft.layout == BusinessCardLayout.LEFT_ALIGNED,
                    onClick = { onUpdateDraft(draft.copy(layout = BusinessCardLayout.LEFT_ALIGNED), false) },
                    testTag = "bc_layout_left",
                    modifier = Modifier.weight(1f)
                )
                CompactChip(
                    label = ShopLocale.get("bc_layout_center", currentLanguage),
                    selected = draft.layout == BusinessCardLayout.CENTERED,
                    onClick = { onUpdateDraft(draft.copy(layout = BusinessCardLayout.CENTERED), false) },
                    testTag = "bc_layout_center",
                    modifier = Modifier.weight(1f)
                )
                CompactChip(
                    label = ShopLocale.get("bc_layout_split", currentLanguage),
                    selected = draft.layout == BusinessCardLayout.SPLIT_PANEL,
                    onClick = { onUpdateDraft(draft.copy(layout = BusinessCardLayout.SPLIT_PANEL), false) },
                    testTag = "bc_layout_split",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. 12-Palette Picker + Light Card Switch
        item(key = "bc_palettes") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShopLocale.get("bc_section_palettes", currentLanguage),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ShopLocale.get("bc_light_card_label", currentLanguage),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Switch(
                                checked = draft.isLightCard,
                                onCheckedChange = { onUpdateDraft(draft.copy(isLightCard = it), false) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = VividGreen,
                                    uncheckedThumbColor = TextPrimary,
                                    uncheckedTrackColor = Color(0xFF2B2F2B)
                                ),
                                modifier = Modifier.testTag("bc_light_card_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val cols = when {
                            maxWidth >= 560.dp -> 4
                            maxWidth >= 400.dp -> 3
                            else -> 2
                        }
                        val rows = BusinessCardPalettes.ALL.chunked(cols)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { rowPalettes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPalettes.forEach { palette ->
                                        PaletteGridTile(
                                            palette = palette,
                                            selected = draft.paletteId == palette.id,
                                            currentLanguage = currentLanguage,
                                            onClick = { onUpdateDraft(draft.copy(paletteId = palette.id), false) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(cols - rowPalettes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Editable Card Details + Reset to Profile
        item(key = "bc_details_form") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ShopLocale.get("bc_section_details", currentLanguage),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onResetToProfile,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_reset_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ShopLocale.get("bc_reset_profile_btn", currentLanguage),
                            color = VividGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardTextField(
                        value = draft.shopName,
                        onValueChange = { onUpdateDraft(draft.copy(shopName = it), true) },
                        label = ShopLocale.get("bc_field_shop_name", currentLanguage),
                        hint = ShopLocale.get("bc_hint_shop_name", currentLanguage),
                        testTag = "bc_input_shop_name"
                    )
                    CardTextField(
                        value = draft.ownerName,
                        onValueChange = { onUpdateDraft(draft.copy(ownerName = it), true) },
                        label = ShopLocale.get("bc_field_owner_name", currentLanguage),
                        hint = ShopLocale.get("bc_hint_owner_name", currentLanguage),
                        testTag = "bc_input_owner_name"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardTextField(
                            value = draft.title,
                            onValueChange = { onUpdateDraft(draft.copy(title = it), true) },
                            label = ShopLocale.get("bc_field_title", currentLanguage),
                            hint = ShopLocale.get("bc_hint_title", currentLanguage),
                            testTag = "bc_input_title",
                            modifier = Modifier.weight(1f)
                        )
                        CardTextField(
                            value = draft.tagline,
                            onValueChange = { onUpdateDraft(draft.copy(tagline = it), true) },
                            label = ShopLocale.get("bc_field_tagline", currentLanguage),
                            hint = ShopLocale.get("bc_hint_tagline", currentLanguage),
                            testTag = "bc_input_tagline",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardTextField(
                            value = draft.phone,
                            onValueChange = { onUpdateDraft(draft.copy(phone = it), true) },
                            label = ShopLocale.get("bc_field_phone", currentLanguage),
                            hint = ShopLocale.get("bc_hint_phone", currentLanguage),
                            testTag = "bc_input_phone",
                            modifier = Modifier.weight(1f)
                        )
                        CardTextField(
                            value = draft.email,
                            onValueChange = { onUpdateDraft(draft.copy(email = it), true) },
                            label = ShopLocale.get("bc_field_email", currentLanguage),
                            hint = ShopLocale.get("bc_hint_email", currentLanguage),
                            testTag = "bc_input_email",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CardTextField(
                        value = draft.address,
                        onValueChange = { onUpdateDraft(draft.copy(address = it), true) },
                        label = ShopLocale.get("bc_field_address", currentLanguage),
                        hint = ShopLocale.get("bc_hint_address", currentLanguage),
                        testTag = "bc_input_address"
                    )
                }
                }
            }
        }

        // 4. Optional Logo & Background Upload + Dark Overlay Slider
        item(key = "bc_media") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = ShopLocale.get("bc_section_media", currentLanguage),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUploadLogo,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_upload_logo_btn")
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = VividGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (draft.logoUri.isBlank()) {
                                ShopLocale.get("bc_upload_logo_btn", currentLanguage)
                            } else {
                                ShopLocale.get("bc_change_logo_btn", currentLanguage)
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (draft.logoUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onUpdateDraft(draft.copy(logoUri = ""), true) },
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("bc_remove_logo_btn")
                        ) {
                            Text(
                                text = ShopLocale.get("bc_remove_btn", currentLanguage),
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUploadBackground,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_upload_bg_btn")
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = VividGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (draft.backgroundImageUri.isBlank()) {
                                ShopLocale.get("bc_upload_bg_btn", currentLanguage)
                            } else {
                                ShopLocale.get("bc_change_bg_btn", currentLanguage)
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (draft.backgroundImageUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onUpdateDraft(draft.copy(backgroundImageUri = ""), true) },
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("bc_remove_bg_btn")
                        ) {
                            Text(
                                text = ShopLocale.get("bc_remove_btn", currentLanguage),
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (draft.backgroundImageUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShopLocale.get("bc_bg_overlay_label", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(draft.backgroundOverlayAlpha * 100).toInt()}%",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Slider(
                        value = draft.backgroundOverlayAlpha,
                        onValueChange = { onUpdateDraft(draft.copy(backgroundOverlayAlpha = it), true) },
                        valueRange = 0f..0.85f,
                        colors = SliderDefaults.colors(
                            thumbColor = VividGreen,
                            activeTrackColor = VividGreen,
                            inactiveTrackColor = Color(0xFF333833)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bc_bg_overlay_slider")
                    )
                }
                }
            }
        }

        // 5. Fonts (Bangla + Latin pairs), Size & Alignment Controls
        item(key = "bc_typography") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = ShopLocale.get("bc_section_typography", currentLanguage),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = ShopLocale.get("bc_font_pair_label", currentLanguage),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BusinessCardFontPair.entries.chunked(2).forEach { pairRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pairRow.forEach { pair ->
                                CompactChip(
                                    label = if (currentLanguage == AppLanguage.BN) pair.labelBn else pair.labelEn,
                                    selected = draft.fontPair == pair,
                                    onClick = { onUpdateDraft(draft.copy(fontPair = pair), false) },
                                    testTag = "bc_font_${pair.id}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = ShopLocale.get("bc_font_size_label", currentLanguage),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BusinessCardFontSize.entries.forEach { sz ->
                        CompactChip(
                            label = if (currentLanguage == AppLanguage.BN) sz.labelBn else sz.labelEn,
                            selected = draft.fontSize == sz,
                            onClick = { onUpdateDraft(draft.copy(fontSize = sz), false) },
                            testTag = "bc_size_${sz.id}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = ShopLocale.get("bc_text_align_label", currentLanguage),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessCardTextAlign.entries.forEach { align ->
                        CompactChip(
                            label = if (currentLanguage == AppLanguage.BN) align.labelBn else align.labelEn,
                            selected = draft.textAlign == align,
                            onClick = { onUpdateDraft(draft.copy(textAlign = align), false) },
                            testTag = "bc_align_${align.id}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                }
            }
        }

        // 6. Output Actions: Save to My Cards, Download PNG, Download PDF, Share
        item(key = "bc_actions") {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveToMyCards,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VividGreen, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1.35f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_save_card_btn")
                    ) {
                        Icon(Icons.Outlined.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ShopLocale.get("bc_save_card_btn", currentLanguage),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onShare,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(0.85f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_share_btn")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = VividGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ShopLocale.get("bc_share_btn", currentLanguage),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadPng,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_download_png_btn")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = VividGreen)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ShopLocale.get("bc_download_png_btn", currentLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onDownloadPdf,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("bc_download_pdf_btn")
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = VividGreen)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ShopLocale.get("bc_download_pdf_btn", currentLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ShopLocale.get("bc_phone_only_note", currentLanguage),
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                }
            }
        }
    }
}

@Composable
private fun MySavedCardsContent(
    headerSlot: LazyListScope.() -> Unit,
    cards: List<SavedBusinessCard>,
    currentLanguage: AppLanguage,
    onLoadCard: (SavedBusinessCard) -> Unit,
    onDeleteCard: (SavedBusinessCard) -> Unit,
    onShareCard: (SavedBusinessCard) -> Unit,
    onDownloadPng: (SavedBusinessCard) -> Unit,
    onDownloadPdf: (SavedBusinessCard) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bc_my_cards_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        headerSlot()
        if (cards.isEmpty()) {
            item(key = "bc_my_cards_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlainEmptyState(
                        message = ShopLocale.get("bc_my_cards_empty", currentLanguage)
                    )
                }
            }
        } else {
            items(cards, key = { it.id }) { saved ->
            val palette = BusinessCardPalettes.byId(saved.draft.paletteId)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bc_saved_card_${saved.id}")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = saved.draft.shopName.ifBlank { ShopLocale.get("bc_sample_shop", currentLanguage) },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (currentLanguage == AppLanguage.BN) palette.nameBn else palette.nameEn} · ${ShopLocale.formatDateShort(saved.savedAtMs, currentLanguage)}",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onLoadCard(saved) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("bc_saved_edit_${saved.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = ShopLocale.get("bc_load_card_btn", currentLanguage),
                                tint = VividGreen
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCard(saved) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("bc_saved_delete_${saved.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = ShopLocale.get("bc_delete_card_btn", currentLanguage),
                                tint = AlertRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BusinessCardCanvasPreview(
                    draft = saved.draft,
                    currentLanguage = currentLanguage
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactActionChip(
                        label = ShopLocale.get("bc_download_png_btn", currentLanguage),
                        icon = Icons.Outlined.Download,
                        onClick = { onDownloadPng(saved) },
                        testTag = "bc_saved_png_${saved.id}"
                    )
                    CompactActionChip(
                        label = ShopLocale.get("bc_download_pdf_btn", currentLanguage),
                        icon = Icons.Outlined.PictureAsPdf,
                        onClick = { onDownloadPdf(saved) },
                        testTag = "bc_saved_pdf_${saved.id}"
                    )
                    CompactActionChip(
                        label = ShopLocale.get("bc_share_btn", currentLanguage),
                        icon = Icons.Outlined.Share,
                        onClick = { onShareCard(saved) },
                        testTag = "bc_saved_share_${saved.id}"
                    )
                }
                }
            }
            }
        }
    }
}

@Composable
private fun PaletteGridTile(
    palette: BusinessCardPalette,
    selected: Boolean,
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("bc_palette_${palette.id}"),
        shape = shape,
        color = if (selected) Color(0xFF1F2D1B) else Color(0xFF171A17),
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) VividGreen else GlassBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                palette.hexColors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(BusinessCardPaletteEngine.parseHexColor(hex)))
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                    )
                }
            }
            Text(
                text = if (currentLanguage == AppLanguage.BN) palette.nameBn else palette.nameEn,
                color = if (selected) VividGreen else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = if (selected) VividGreen else Color(0xFF1A1E1A),
        border = BorderStroke(1.dp, if (selected) VividGreen else GlassBorder)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.Black else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = if (selected) VividGreen.copy(alpha = 0.20f) else Color(0xFF161916),
        border = BorderStroke(1.dp, if (selected) VividGreen else GlassBorder)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) VividGreen else TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = Color(0xFF181C18),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = VividGreen, modifier = Modifier.size(15.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = {
            Text(
                text = hint,
                color = TextMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VividGreen,
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = VividGreen,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = VividGreen
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
