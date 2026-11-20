package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.SaleItem
import com.example.domain.CalculationEngine
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale

/**
 * Father Mode Sell Flow — STEP 3 confirmation screen.
 * Default screen stays a clean single "ঠিক আছে?" question with two optional secondary buttons
 * ("ছাড় দিন" and "কিছু টাকা দিয়েছে") that remain hidden fields until tapped.
 */
@Composable
fun FatherSellStep3Content(
    builtSaleItems: List<SaleItem>,
    subtotal: Double,
    totalDiscount: Double,
    grandTotal: Double,
    paymentType: String,
    attachedCustomer: Customer?,
    partialPaidAmount: Double?,
    clampNoticeTick: Int = 0,
    displayError: String?,
    isBusy: Boolean,
    onApplyDiscount: (Double) -> Unit,
    onClearDiscount: () -> Unit,
    onApplyPartialPaid: (Double) -> Unit,
    onClearPartialPaid: () -> Unit,
    onPaymentTypeChange: (String) -> Unit,
    onOpenCustomerPicker: (requireCustomerForBaki: Boolean) -> Unit,
    onConfirmSale: (paidNow: Double) -> Unit,
    onClose: () -> Unit,
    canSeeProfit: Boolean = true,
    modifier: Modifier = Modifier
) {
    val amberAccent = Color(0xFFFFC107)
    val highContrastCardBg = Color(0xFF141814)
    val highContrastBorder = Color(0x52FFFFFF)

    var showDiscountPad by remember { mutableStateOf(false) }
    var showPartialPayPad by remember { mutableStateOf(false) }
    var showHighDiscountConfirm by remember { mutableStateOf(false) }
    var showClampNotice by remember { mutableStateOf(false) }

    LaunchedEffect(clampNoticeTick) {
        if (clampNoticeTick > 0) {
            showClampNotice = true
            delay(3500L)
            showClampNotice = false
        }
    }

    val hasDiscount = totalDiscount > 0.001
    val hasPartialPayment = partialPaidAmount != null

    val maxAdditionalDiscount = remember(builtSaleItems) {
        CalculationEngine.calculateMaxInvoiceDiscount(builtSaleItems)
    }
    val maxProfitDiscount = remember(builtSaleItems) {
        CalculationEngine.calculateSaleMaxProfitDiscount(builtSaleItems)
    }
    val isDiscountOverProfit = remember(totalDiscount, maxProfitDiscount) {
        totalDiscount > 0.001 && totalDiscount > maxProfitDiscount + 0.001
    }
    val discountLimitMessage = remember(maxProfitDiscount, canSeeProfit) {
        if (canSeeProfit) {
            "লাভের (${ShopLocale.formatMoney(maxProfitDiscount, AppLanguage.BN)}) চেয়ে বেশি ছাড় দেওয়া যাবে না"
        } else {
            "অনুমোদিত সীমার চেয়ে বেশি ছাড় দেওয়া যাবে না"
        }
    }

    val paidNow = remember(partialPaidAmount, paymentType, grandTotal) {
        when {
            partialPaidAmount != null -> CalculationEngine.roundMoney(partialPaidAmount.coerceIn(0.0, grandTotal))
            paymentType == "baki" -> 0.0
            else -> grandTotal
        }
    }
    val remainingBaki = remember(grandTotal, paidNow) {
        CalculationEngine.roundMoney((grandTotal - paidNow).coerceAtLeast(0.0))
    }
    val needsCustomer = remainingBaki > 0.001

    fun attemptConfirm() {
        if (isBusy || isDiscountOverProfit) return
        if (needsCustomer && attachedCustomer == null) {
            onOpenCustomerPicker(true)
            return
        }
        if (subtotal > 0.0 && totalDiscount > subtotal * 0.20 + 0.0001) {
            showHighDiscountConfirm = true
            return
        }
        onConfirmSale(paidNow)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)
            .testTag("father_sell_step3_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: "ঠিক আছে?" + close button to go back to item selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ঠিক আছে?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Surface(
                onClick = onClose,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E221E),
                border = BorderStroke(1.dp, highContrastBorder),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("sell_checkout_close_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ফিরুন",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "বদলান",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Payment mode selector: নগদ বিক্রি / বাকি বিক্রি
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isCashMode = paymentType != "baki" && !hasPartialPayment
            val isFullBakiMode = paymentType == "baki" && !hasPartialPayment
            Surface(
                onClick = {
                    onClearPartialPaid()
                    onPaymentTypeChange("cash")
                },
                shape = RoundedCornerShape(16.dp),
                color = if (isCashMode) VividGreen else highContrastCardBg,
                border = BorderStroke(1.4.dp, if (isCashMode) VividGreen else highContrastBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("sell_payment_cash")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(
                        text = "নগদ বিক্রি",
                        color = if (isCashMode) Color(0xFF061204) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
            Surface(
                onClick = {
                    onClearPartialPaid()
                    onPaymentTypeChange("baki")
                    if (attachedCustomer == null) {
                        onOpenCustomerPicker(true)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = if (isFullBakiMode) amberAccent else highContrastCardBg,
                border = BorderStroke(1.4.dp, if (isFullBakiMode) amberAccent else highContrastBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("sell_payment_baki")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Text(
                        text = "বাকি বিক্রি",
                        color = if (isFullBakiMode) Color(0xFF061204) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }

        // Main Summary Card: always shows total, discount (if any), paid now, remaining baki (if any) at >= 20sp
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = highContrastCardBg,
            border = BorderStroke(1.4.dp, highContrastBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("father_sell_summary_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item count line
                val itemCountBn = ShopLocale.formatNumber(builtSaleItems.size.toLong(), AppLanguage.BN)
                Text(
                    text = "$itemCountBn টি জিনিস",
                    color = Color(0xFFD5DCD5),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Total (before discount if discount applied, else big total)
                if (!hasDiscount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ShopLocale.formatMoney(grandTotal, AppLanguage.BN),
                            color = VividGreen,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    // Original Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট",
                            color = Color(0xFFD5DCD5),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ShopLocale.formatMoney(subtotal, AppLanguage.BN),
                            color = Color(0xFFD5DCD5),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Discount row: "ছাড় ৳X" + small "বাদ দিন" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("father_discount_applied_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ছাড় ${ShopLocale.formatMoney(totalDiscount, AppLanguage.BN)}",
                            color = amberAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        FatherUndoChip(
                            onClick = onClearDiscount,
                            testTag = "father_clear_discount_btn"
                        )
                    }

                    // New Total in very large text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "নতুন মোট",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = ShopLocale.formatMoney(grandTotal, AppLanguage.BN),
                            color = VividGreen,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                HorizontalDivider(color = Color(0x28FFFFFF))

                // Paid Now row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("father_paid_now_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "এখন দিয়েছেন",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ShopLocale.formatMoney(paidNow, AppLanguage.BN),
                            color = VividGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (hasPartialPayment) {
                            FatherUndoChip(
                                onClick = onClearPartialPaid,
                                testTag = "father_clear_partial_btn"
                            )
                        }
                    }
                }

                if (showClampNotice) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33FFC107),
                        border = BorderStroke(1.dp, amberAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("father_clamp_notice")
                    ) {
                        Text(
                            text = "দেওয়া টাকা কমে গেছে",
                            color = amberAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Remaining Baki row (if any)
                if (needsCustomer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("father_remaining_baki_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "বাকি থাকবে ${ShopLocale.formatMoney(remainingBaki, AppLanguage.BN)}",
                            color = amberAccent,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2
                        )
                    }
                }

                // Customer selector / required prompt when remainingBaki > 0
                if (needsCustomer || attachedCustomer != null) {
                    HorizontalDivider(color = Color(0x28FFFFFF))
                    if (attachedCustomer != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = attachedCustomer.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                onClick = { onOpenCustomerPicker(needsCustomer) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x24FFFFFF),
                                border = BorderStroke(1.dp, highContrastBorder),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "বদলান",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = { onOpenCustomerPicker(true) },
                            shape = RoundedCornerShape(16.dp),
                            color = amberAccent.copy(alpha = 0.20f),
                            border = BorderStroke(1.4.dp, amberAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp)
                                .testTag("father_baki_select_customer_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = amberAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "বাকি রাখতে কার নাম?",
                                    color = amberAccent,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Two big secondary buttons under the summary (hidden fields until tapped)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { showDiscountPad = true },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1A201A),
                border = BorderStroke(1.4.dp, if (hasDiscount) amberAccent else highContrastBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .testTag("father_btn_give_discount")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "ছাড় দিন",
                        color = if (hasDiscount) amberAccent else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            Surface(
                onClick = { showPartialPayPad = true },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1A201A),
                border = BorderStroke(1.4.dp, if (hasPartialPayment) VividGreen else highContrastBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .testTag("father_btn_partial_paid")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "কিছু টাকা দিয়েছে",
                        color = if (hasPartialPayment) VividGreen else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        val effectiveError = if (isDiscountOverProfit) discountLimitMessage else displayError
        if (!effectiveError.isNullOrBlank()) {
            Text(
                text = effectiveError,
                color = AlertRed,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("father_step3_error_text")
            )
        }

        // Big Primary Confirm Button
        VividButton(
            text = "হ্যাঁ, বিক্রি ঠিক আছে",
            enabled = !isBusy && !isDiscountOverProfit,
            isLoading = isBusy,
            onClick = ::attemptConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            fontSize = 22.sp,
            testTag = "complete_sale_button"
        )
    }

    // Big Number Pad: "ছাড় দিন"
    if (showDiscountPad) {
        FatherAmountNumpadDialog(
            title = "কত টাকা ছাড়?",
            initialAmount = if (hasDiscount) totalDiscount else 0.0,
            maxAmount = maxAdditionalDiscount,
            maxErrorText = if (canSeeProfit) {
                "লাভের (${ShopLocale.formatMoney(maxAdditionalDiscount, AppLanguage.BN)}) চেয়ে বেশি ছাড় দেওয়া যাবে না"
            } else {
                "অনুমোদিত সীমার চেয়ে বেশি ছাড় দেওয়া যাবে না"
            },
            showMaxAmount = canSeeProfit,
            onDismiss = { showDiscountPad = false },
            onConfirm = { amount ->
                showDiscountPad = false
                if (amount <= 0.0) {
                    onClearDiscount()
                } else {
                    onApplyDiscount(amount)
                }
            }
        )
    }

    // Big Number Pad: "কিছু টাকা দিয়েছে" -> "এখন কত দিলেন?"
    if (showPartialPayPad) {
        FatherAmountNumpadDialog(
            title = "এখন কত দিলেন?",
            initialAmount = if (hasPartialPayment) paidNow else 0.0,
            maxAmount = grandTotal,
            maxErrorText = "মোট টাকার বেশি লেখা যাবে না",
            onDismiss = { showPartialPayPad = false },
            onConfirm = { amount ->
                showPartialPayPad = false
                onApplyPartialPaid(amount)
                val rem = CalculationEngine.roundMoney((grandTotal - amount).coerceAtLeast(0.0))
                if (rem > 0.001 && attachedCustomer == null) {
                    onOpenCustomerPicker(true)
                }
            }
        )
    }

    // Extra check before saving if discount > 20% of total
    if (showHighDiscountConfirm) {
        AlertDialog(
            onDismissRequest = { showHighDiscountConfirm = false },
            properties = edgeToEdgeDialogProperties(),
            modifier = Modifier.safeDialogWindowPadding(),
            containerColor = Color(0xFF141814),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = "ছাড় অনেক বেশি, ঠিক আছে?",
                    color = amberAccent,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "ছাড় ${ShopLocale.formatMoney(totalDiscount, AppLanguage.BN)} (মোট ${ShopLocale.formatMoney(subtotal, AppLanguage.BN)})",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        showHighDiscountConfirm = false
                        onConfirmSale(paidNow)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = VividGreen,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 96.dp, minHeight = 52.dp)
                        .testTag("father_high_discount_yes_btn")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "হ্যাঁ",
                            color = Color(0xFF061204),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            },
            dismissButton = {
                Surface(
                    onClick = { showHighDiscountConfirm = false },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF262A26),
                    border = BorderStroke(1.dp, highContrastBorder),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 96.dp, minHeight = 52.dp)
                        .testTag("father_high_discount_no_btn")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "না",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FatherUndoChip(
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = AlertRed.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.75f)),
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "বাদ দিন",
                color = Color(0xFFFF8A80),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Big high-contrast Bengali number pad dialog for Father Mode STEP 3
 * ("ছাড় দিন" and "এখন কত দিলেন?").
 */
@Composable
private fun FatherAmountNumpadDialog(
    title: String,
    initialAmount: Double,
    maxAmount: Double,
    maxErrorText: String,
    showMaxAmount: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var digits by remember {
        mutableStateOf(
            if (initialAmount > 0.0) {
                val rounded = CalculationEngine.roundMoney(initialAmount)
                if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
            } else {
                ""
            }
        )
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedAmount = remember(digits) {
        digits.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141814),
            border = BorderStroke(1.4.dp, Color(0x52FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("father_numpad_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2
                )

                // Display box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0A0D0A),
                    border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${ShopLocale.CURRENCY}${ShopLocale.digits(digits.ifBlank { "0" }, AppLanguage.BN)}",
                            color = VividGreen,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (showMaxAmount) {
                            Text(
                                text = "সর্বোচ্চ: ${ShopLocale.formatMoney(maxAmount, AppLanguage.BN)}",
                                color = TextSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText!!,
                        color = AlertRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("father_numpad_error_text")
                    )
                }

                // 3x4 Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    onClick = {
                                        errorText = null
                                        when (key) {
                                            "C" -> digits = ""
                                            "DEL" -> if (digits.isNotEmpty()) digits = digits.dropLast(1)
                                            else -> {
                                                if (digits.length < 8) {
                                                    val candidate = if (digits == "0") key else digits + key
                                                    val valNum = candidate.toDoubleOrNull() ?: 0.0
                                                    if (valNum > maxAmount + 0.001) {
                                                        errorText = maxErrorText
                                                    } else {
                                                        digits = candidate
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = when (key) {
                                        "C" -> AlertRed.copy(alpha = 0.22f)
                                        "DEL" -> Color(0xFF282D28)
                                        else -> Color(0xFF1E241E)
                                    },
                                    border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .testTag("numpad_key_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when (key) {
                                            "DEL" -> Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "মুছুন",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            "C" -> Text(
                                                text = "মুছুন",
                                                color = Color(0xFFFF8A80),
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            else -> Text(
                                                text = ShopLocale.digits(key, AppLanguage.BN),
                                                color = Color.White,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom actions: বাতিল / ঠিক আছে
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF242824),
                        border = BorderStroke(1.dp, Color(0x42FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("father_numpad_cancel_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "বাতিল",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            if (parsedAmount > maxAmount + 0.001) {
                                errorText = maxErrorText
                            } else {
                                onConfirm(CalculationEngine.roundMoney(parsedAmount))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = VividGreen,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("father_numpad_confirm_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "ঠিক আছে",
                                color = Color(0xFF061204),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
