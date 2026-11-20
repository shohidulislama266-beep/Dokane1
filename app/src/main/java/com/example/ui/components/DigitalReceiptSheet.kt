package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.CompletedSaleReceipt
import com.example.domain.CalculationEngine
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.ProductImageUtil
import com.example.util.ReceiptImageRenderer
import com.example.util.ReceiptImageStyle
import com.example.util.ShopLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalReceiptBottomSheet(
    receipt: CompletedSaleReceipt,
    onDismiss: () -> Unit,
    onStartNewSale: (() -> Unit)? = null,
    receiptImageStyle: ReceiptImageStyle = ReceiptImageStyle.LIGHT
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color(0xAA000000),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.testTag("digital_receipt_bottom_sheet")
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        DigitalReceiptContent(
            receipt = receipt,
            onDismiss = onDismiss,
            onStartNewSale = onStartNewSale,
            receiptImageStyle = receiptImageStyle
        )
    }
}

@Composable
fun DigitalReceiptContent(
    receipt: CompletedSaleReceipt,
    onDismiss: () -> Unit,
    onStartNewSale: (() -> Unit)? = null,
    receiptImageStyle: ReceiptImageStyle = ReceiptImageStyle.LIGHT
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val receiptLang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()

    var isGeneratingShareImage by remember { mutableStateOf(false) }
    var isSavingImage by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }

    val isBaki = receipt.paymentType.equals("baki", ignoreCase = true)
    val formattedDate = remember(receipt.timestamp, receiptLang) {
        ShopLocale.formatDateLong(receipt.timestamp, receiptLang)
    }

    val isDarkSlip = receiptImageStyle == ReceiptImageStyle.DARK
    val slipCardBg = if (isDarkSlip) Color(0xFF202020) else Color(0xFFFFFFFF)
    val slipBorder = if (isDarkSlip) Color(0xFF333333) else Color(0xFFE5E7EB)
    val slipTextPrimary = if (isDarkSlip) Color(0xFFF8F8F8) else Color(0xFF111827)
    val slipTextSecondary = if (isDarkSlip) Color(0xFFD1D5DB) else Color(0xFF374151)
    val slipTextMuted = if (isDarkSlip) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val slipAccentGreen = if (isDarkSlip) Color(0xFF5DD62C) else Color(0xFF337418)
    val slipDivider = if (isDarkSlip) Color(0xFF333333) else Color(0xFFE5E7EB)
    val slipPaidBadgeBg = if (isDarkSlip) Color(0xFF1D3814) else Color(0xFFE8F5E2)
    val slipPaidBadgeText = if (isDarkSlip) Color(0xFF5DD62C) else Color(0xFF337418)
    val slipBakiBadgeBg = if (isDarkSlip) Color(0xFF3D2B08) else Color(0xFFFEF3C7)
    val slipBakiBadgeText = if (isDarkSlip) Color(0xFFFBBF24) else Color(0xFFB45309)
    val slipDiscountColor = if (isDarkSlip) Color(0xFFFF6B6B) else Color(0xFFDC2626)

    val logoModel = remember(receipt.storeLogoUri, context) {
        if (receipt.storeLogoUri.isBlank()) null
        else ProductImageUtil.toCoilImageModel(context, receipt.storeLogoUri)
    }

    val rawCustomerName = receipt.customerName.trim()
    val hasAttachedCustomer = rawCustomerName.isNotEmpty() &&
        !rawCustomerName.equals("Cash Sale", ignoreCase = true) &&
        !rawCustomerName.equals("Walk-in", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Header Banner with safe top padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sale_completed_header"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x265DD62C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = ShopLocale.get("sale_completed", receiptLang),
                        tint = VividGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ShopLocale.get("sale_completed", receiptLang),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("sale_completed_title")
                    )
                    Text(
                        text = if (receiptLang == AppLanguage.BN) {
                            "স্টক রিয়েল-টাইমে আপডেট হয়েছে"
                        } else {
                            "Stock updated in real-time"
                        },
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("receipt_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = ShopLocale.get("cancel", receiptLang),
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // On-screen Receipt Slip Card matching the exported PNG design
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, slipBorder), RoundedCornerShape(16.dp))
                .testTag("digital_receipt_slip_card"),
            shape = RoundedCornerShape(16.dp),
            color = slipCardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                if (logoModel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = logoModel),
                            contentDescription = receipt.storeName.ifBlank { "Logo" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("receipt_store_logo")
                        )
                    }
                }

                if (receipt.storeName.isNotBlank()) {
                    Text(
                        text = receipt.storeName.trim(),
                        color = slipTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (receipt.storePhone.isNotBlank()) {
                    Text(
                        text = ShopLocale.format(
                            "phone_prefix",
                            receiptLang,
                            ShopLocale.digits(receipt.storePhone.trim(), receiptLang)
                        ),
                        color = slipTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (receipt.storeAddress.isNotBlank()) {
                    Text(
                        text = ShopLocale.digits(receipt.storeAddress.trim(), receiptLang),
                        color = slipTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (
                    logoModel != null ||
                    receipt.storeName.isNotBlank() ||
                    receipt.storePhone.isNotBlank() ||
                    receipt.storeAddress.isNotBlank()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = slipDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Invoice No (exactly as stored, e.g. INV-0107, single digit style) + Date + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ShopLocale.get("invoice", receiptLang)}: ${ShopLocale.digits(receipt.invoiceNo.trim(), receiptLang)}",
                            color = slipAccentGreen,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("receipt_invoice_number")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            color = slipTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBaki) slipBakiBadgeBg else slipPaidBadgeBg
                    ) {
                        Text(
                            text = ShopLocale.get(
                                if (isBaki) "receipt_status_baki" else "receipt_status_paid",
                                receiptLang
                            ),
                            color = if (isBaki) slipBakiBadgeText else slipPaidBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("receipt_status_badge")
                        )
                    }
                }

                if (hasAttachedCustomer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val customerLine = buildString {
                        append(ShopLocale.format("customer_prefix", receiptLang, rawCustomerName))
                        val phone = receipt.customerPhone.trim()
                        if (phone.isNotEmpty()) {
                            append(" (${ShopLocale.digits(phone, receiptLang)})")
                        }
                    }
                    Text(
                        text = customerLine,
                        color = slipTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("receipt_customer_line")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = slipDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ShopLocale.get("receipt_col_item", receiptLang),
                        color = slipTextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.75f)
                    )
                    Text(
                        text = ShopLocale.get("receipt_col_qty", receiptLang),
                        color = slipTextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = ShopLocale.get("receipt_col_rate", receiptLang),
                        color = slipTextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        text = ShopLocale.get("receipt_col_amount", receiptLang),
                        color = slipTextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.15f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Item Rows: item name wraps fully across lines with NO ellipsis ("...")
                receipt.items.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.name,
                            color = slipTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .weight(1.75f)
                                .padding(end = 6.dp)
                                .testTag("receipt_item_name_$idx")
                        )
                        Text(
                            text = "${ShopLocale.formatNumber(item.qty, receiptLang)} ${ShopLocale.displayUnit(item.unit, receiptLang)}",
                            color = slipTextSecondary,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.weight(0.95f)
                        )
                        Text(
                            text = ReceiptImageRenderer.formatStoredRate(item.sellPrice, receiptLang),
                            color = slipTextSecondary,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            text = ShopLocale.formatMoneyFixed2(item.lineTotal, receiptLang),
                            color = slipTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            modifier = Modifier.weight(1.15f)
                        )
                    }
                    if (idx < receipt.items.lastIndex) {
                        HorizontalDivider(color = slipDivider.copy(alpha = 0.55f), thickness = 0.7.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = slipDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ShopLocale.get("subtotal", receiptLang),
                        color = slipTextSecondary,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = ShopLocale.formatMoneyFixed2(receipt.subtotal, receiptLang),
                        color = slipTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Discount (only when > 0)
                if (receipt.discount > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_discount_row"),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ShopLocale.get("total_discount", receiptLang),
                            color = slipDiscountColor,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "-${ShopLocale.formatMoneyFixed2(receipt.discount, receiptLang)}",
                            color = slipDiscountColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Paid Amount & Due Amount (only for Baki sales)
                if (isBaki) {
                    val paidAmt = CalculationEngine.roundMoney(receipt.paidAmount.coerceAtLeast(0.0))
                    val dueAmt = CalculationEngine.roundMoney(
                        if (receipt.dueAmount > 0.0 || receipt.paidAmount > 0.0) {
                            receipt.dueAmount.coerceAtLeast(0.0)
                        } else {
                            (receipt.grandTotal - paidAmt).coerceAtLeast(0.0)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_paid_row"),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ShopLocale.get("paid_amount_label", receiptLang),
                            color = slipTextSecondary,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = ShopLocale.formatMoneyFixed2(paidAmt, receiptLang),
                            color = slipTextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_due_row"),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ShopLocale.get("due_amount_label", receiptLang),
                            color = slipBakiBadgeText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ShopLocale.formatMoneyFixed2(dueAmt, receiptLang),
                            color = slipBakiBadgeText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grand Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ShopLocale.get("grand_total", receiptLang).uppercase(),
                        color = slipTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = ShopLocale.formatMoneyFixed2(receipt.grandTotal, receiptLang),
                        color = slipAccentGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("receipt_grand_total_value")
                    )
                }

                if (receipt.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ShopLocale.format("note_prefix", receiptLang, ShopLocale.digits(receipt.notes.trim(), receiptLang)),
                        color = slipTextMuted,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ShopLocale.get("receipt_image_thank_you", receiptLang),
                    color = slipTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Loading / Success / Error banner (never falls back to sharing text silently)
        if (isGeneratingShareImage || isSavingImage) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1D281A),
                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_image_loading_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = VividGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = ShopLocale.get("making_receipt_image", receiptLang),
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else if (!feedbackMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (feedbackIsError) Color(0x2EFF5252) else Color(0x265DD62C),
                border = BorderStroke(
                    1.dp,
                    if (feedbackIsError) Color(0xFFFF5252) else VividGreen.copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_image_feedback_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (feedbackIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (feedbackIsError) Color(0xFFFF8A80) else VividGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feedbackMessage.orEmpty(),
                        color = if (feedbackIsError) Color(0xFFFFCDD2) else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 1: Share Receipt (PNG) + Save Image (Gallery PNG)
        val actionsIdle = !isGeneratingShareImage && !isSavingImage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (!actionsIdle) return@Button
                    feedbackMessage = null
                    isGeneratingShareImage = true
                    coroutineScope.launch {
                        val exportResult = withContext(Dispatchers.IO) {
                            runCatching {
                                ReceiptImageRenderer.exportReceiptPng(
                                    context = context,
                                    receipt = receipt,
                                    style = receiptImageStyle,
                                    lang = receiptLang
                                )
                            }
                        }
                        val result = exportResult.mapCatching { file ->
                            ReceiptImageRenderer.shareExistingReceiptPng(
                                context = context,
                                pngFile = file,
                                receipt = receipt,
                                lang = receiptLang
                            ).getOrThrow()
                        }
                        isGeneratingShareImage = false
                        result.onFailure { err ->
                            val reason = err.localizedMessage?.takeIf { it.isNotBlank() } ?: "Unknown error"
                            feedbackIsError = true
                            feedbackMessage = ShopLocale.format("receipt_image_share_failed", receiptLang, reason)
                        }
                    }
                },
                enabled = actionsIdle,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("share_receipt_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGeneratingShareImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = ShopLocale.get("share_receipt", receiptLang),
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isGeneratingShareImage) {
                        ShopLocale.get("making_receipt_image", receiptLang)
                    } else {
                        ShopLocale.get("share_receipt", receiptLang)
                    },
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = {
                    if (!actionsIdle) return@OutlinedButton
                    feedbackMessage = null
                    isSavingImage = true
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            ReceiptImageRenderer.saveReceiptPngToGallery(
                                context = context,
                                receipt = receipt,
                                style = receiptImageStyle,
                                lang = receiptLang
                            )
                        }
                        isSavingImage = false
                        result.onSuccess { savedName ->
                            val msg = ShopLocale.format("receipt_image_saved_gallery", receiptLang, savedName)
                            feedbackIsError = false
                            feedbackMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            val reason = err.localizedMessage?.takeIf { it.isNotBlank() } ?: "Unknown error"
                            feedbackIsError = true
                            feedbackMessage = ShopLocale.format("receipt_image_save_failed", receiptLang, reason)
                        }
                    }
                },
                enabled = actionsIdle,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("save_receipt_image_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VividGreen
                )
            ) {
                if (isSavingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = VividGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = ShopLocale.get("save_image_btn", receiptLang),
                        tint = VividGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSavingImage) {
                        ShopLocale.get("making_receipt_image", receiptLang)
                    } else {
                        ShopLocale.get("save_image_btn", receiptLang)
                    },
                    color = VividGreen,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Copy Slip (plain text preserved)
        OutlinedButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(receipt.formatTextReceipt(receiptLang)))
                Toast.makeText(
                    context,
                    ShopLocale.get("receipt_copied", receiptLang),
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("copy_receipt_button"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF444444)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = ShopLocale.get("copy_slip", receiptLang),
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ShopLocale.get("copy_slip", receiptLang),
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Start Next Sale / Done Button (kept safely above bottom navigation)
        Button(
            onClick = {
                if (onStartNewSale != null) {
                    onStartNewSale()
                } else {
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("next_sale_button"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VividGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (onStartNewSale != null) {
                    ShopLocale.get("new_sale_next", receiptLang)
                } else {
                    ShopLocale.get("done", receiptLang)
                },
                color = Color.Black,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
