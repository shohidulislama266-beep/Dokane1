package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.StrategyActionTarget
import com.example.domain.BakiOverview
import com.example.domain.BusinessStrategyEngine
import com.example.domain.CustomerBalanceSummary
import com.example.domain.DailyCashflowPoint
import com.example.domain.HomeInsights
import com.example.domain.MoneyNowSummary
import com.example.domain.SevenDayCashflowSummary
import com.example.domain.StockSummary
import com.example.domain.StoreFinancials
import com.example.ui.components.BusinessStrategyCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatHomeMoney(amount: Double, lang: AppLanguage, isHidden: Boolean): String {
    if (isHidden) return "${ShopLocale.CURRENCY}••••"
    if (lang == AppLanguage.BN) {
        return ShopLocale.formatMoney(amount, lang)
    }
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        val hasCents = abs(amount - amount.roundToInt()) >= 0.01
        minimumFractionDigits = if (hasCents) 2 else 0
        maximumFractionDigits = if (hasCents) 2 else 0
    }
    return "${ShopLocale.CURRENCY}${nf.format(amount)}"
}

@Composable
private fun BentoGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var mod = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xEB222622), Color(0xEC141614))
            )
        )
    if (testTag != null) mod = mod.testTag(testTag)
    if (onClick != null) mod = mod.clickable(onClick = onClick)

    Surface(
        modifier = mod,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x36FFFFFF), Color(0x10FFFFFF))
            )
        )
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

private fun formatTodayStrategyKey(ms: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(java.util.Date(ms))
}

internal fun isStrategyFromToday(
    strategy: SavedBusinessStrategy?,
    nowMs: Long = System.currentTimeMillis()
): Boolean {
    if (strategy == null) return false
    val todayKey = formatTodayStrategyKey(nowMs)
    if (strategy.dateKey.isNotBlank()) {
        return strategy.dateKey.trim() == todayKey
    }
    if (strategy.generatedAtMs <= 0L) return false
    return formatTodayStrategyKey(strategy.generatedAtMs) == todayKey
}

private fun isFatherStrategyFromToday(strategy: SavedBusinessStrategy?): Boolean =
    isStrategyFromToday(strategy)

internal fun extractSimpleTipLine(
    strategy: SavedBusinessStrategy?,
    maxWords: Int = 25,
    nowMs: Long = System.currentTimeMillis()
): String? {
    if (!isStrategyFromToday(strategy, nowMs)) return null
    val raw = strategy?.focusLine?.trim()
        ?.ifBlank { strategy.actions.firstOrNull()?.text?.trim().orEmpty() }
        ?.ifBlank { strategy.diagnosis.trim() }
        .orEmpty()
    if (raw.isBlank()) return null
    val words = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return null
    return words.take(maxWords.coerceAtLeast(1)).joinToString(" ")
}

private fun extractFatherTipLine(strategy: SavedBusinessStrategy?): String? =
    extractSimpleTipLine(strategy, maxWords = 15)

@Composable
fun FatherModeHomeLayout(
    financials: StoreFinancials,
    cashInHand: Double,
    bakiOverview: BakiOverview,
    lowStockProducts: List<Product>,
    isPrivacyHidden: Boolean,
    onNavigateToSell: () -> Unit,
    onOpenBaki: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenMonthlyReport: () -> Unit = {},
    businessStrategy: SavedBusinessStrategy? = null
) {
    val context = LocalContext.current
    val amberAccent = Color(0xFFFFC107)
    val highContrastCardBg = Color(0xFF141814)
    val highContrastBorder = Color(0x52FFFFFF)

    val topDebtors = remember(bakiOverview) {
        bakiOverview.debtors
            .filter { it.currentBalance > 0.001 }
            .sortedByDescending { it.currentBalance }
            .take(3)
    }
    val topLowStock = remember(lowStockProducts) {
        lowStockProducts.take(3)
    }
    val todayTip = remember(businessStrategy) {
        extractFatherTipLine(businessStrategy)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp)
            .testTag("father_home_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. VERY LARGE GREEN BUTTON: "বিক্রি করুন"
        Surface(
            onClick = onNavigateToSell,
            shape = RoundedCornerShape(22.dp),
            color = VividGreen,
            border = BorderStroke(1.5.dp, Color(0xFFA5F27F)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .testTag("home_new_sale_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFF061204),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "বিক্রি করুন",
                    color = Color(0xFF061204),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. CARD: "আজকের হিসাব" (today's sales, cash in hand, number of customers — nothing else)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = highContrastCardBg,
            border = BorderStroke(1.2.dp, highContrastBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_todays_summary_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "আজকের হিসাব",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                HorizontalDivider(color = Color(0x28FFFFFF))
                FatherSummaryStatRow(
                    label = "আজকের বিক্রি",
                    value = formatHomeMoney(financials.todaySales, AppLanguage.BN, isPrivacyHidden),
                    valueColor = VividGreen
                )
                FatherSummaryStatRow(
                    label = "হাতে নগদ",
                    value = formatHomeMoney(cashInHand, AppLanguage.BN, isPrivacyHidden),
                    valueColor = Color.White
                )
                FatherSummaryStatRow(
                    label = "কাস্টমার",
                    value = "${ShopLocale.formatNumber(financials.todayOrdersCount, AppLanguage.BN)} জন",
                    valueColor = Color.White
                )
                Surface(
                    onClick = onOpenMonthlyReport,
                    shape = RoundedCornerShape(14.dp),
                    color = VividGreen.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VividGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("father_monthly_report_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "মাসের হিসাব",
                            tint = VividGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মাসের হিসাব",
                            color = VividGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 3. CARD: "কার কাছে পাব" (top 3 debtors + one-tap call + "সব দেখুন")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = highContrastCardBg,
            border = BorderStroke(1.2.dp, highContrastBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("father_home_baki_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "কার কাছে পাব",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Surface(
                        onClick = onOpenBaki,
                        shape = RoundedCornerShape(14.dp),
                        color = VividGreen.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, VividGreen),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("father_baki_see_all_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "সব দেখুন",
                                color = VividGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (topDebtors.isEmpty()) {
                    Text(
                        text = "আজ এখনো কোনো বাকি নেই",
                        color = Color(0xFFD5DCD5),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    topDebtors.forEachIndexed { idx, summary ->
                        if (idx > 0) {
                            HorizontalDivider(color = Color(0x1EFFFFFF))
                        }
                        val phone = summary.customer.mobile.trim()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = summary.customer.name.ifBlank { "কাস্টমার" },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formatHomeMoney(summary.currentBalance, AppLanguage.BN, isPrivacyHidden),
                                color = amberAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (phone.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        runCatching { context.startActivity(dialIntent) }
                                    },
                                    shape = CircleShape,
                                    color = VividGreen,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("father_baki_call_${summary.customer.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "${summary.customer.name} কল করুন",
                                            tint = Color(0xFF061204),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CARD: "কী মাল কমে গেছে" (up to 3 low-stock items + "সব দেখুন")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = highContrastCardBg,
            border = BorderStroke(1.2.dp, highContrastBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_low_stock_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "কী মাল কমে গেছে",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Surface(
                        onClick = onOpenLowStock,
                        shape = RoundedCornerShape(14.dp),
                        color = amberAccent.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, amberAccent),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("father_low_stock_see_all_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "সব দেখুন",
                                color = amberAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (topLowStock.isEmpty()) {
                    Text(
                        text = "সব মাল পর্যাপ্ত আছে",
                        color = VividGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    topLowStock.forEachIndexed { idx, product ->
                        if (idx > 0) {
                            HorizontalDivider(color = Color(0x1EFFFFFF))
                        }
                        val displayName = product.nameBn.ifBlank { product.name }
                        val unitLabel = ShopLocale.displayUnit(product.unit, AppLanguage.BN)
                        val qtyText = "${ShopLocale.formatNumber(product.stock, AppLanguage.BN)} $unitLabel"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            )
                            Text(
                                text = qtyText,
                                color = if (product.stock <= 0.0) Color(0xFFFF5252) else amberAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 5. SHORT AI TIP LINE (at most 15 words, plain Bengali, hidden if older than today)
        if (!todayTip.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A2218),
                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("father_home_tip_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = amberAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = todayTip,
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FatherSummaryStatRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFFDCE2DC),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SimpleHomeLayout(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    cashInHand: Double,
    bakiOverview: BakiOverview,
    lowStockProducts: List<Product>,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSell: Boolean,
    canViewStock: Boolean,
    canAddEditProducts: Boolean,
    canAccessBaki: Boolean,
    canReceivePayment: Boolean,
    canSeeProfit: Boolean,
    canSeeSalesSummary: Boolean,
    onNavigateToSell: () -> Unit,
    onQuickPaymentIn: () -> Unit,
    onAddProduct: () -> Unit,
    onNavigateToBaki: () -> Unit,
    onOpenLowStock: () -> Unit,
    onNavigateToSalesSummary: () -> Unit = {},
    onOpenMonthlyReport: () -> Unit = {},
    businessStrategy: SavedBusinessStrategy? = null,
    canViewStrategy: Boolean = false,
    showProfitInSimpleMode: Boolean = false
) {
    val amberAccent = Color(0xFFFFB300)

    val topDebtors = remember(bakiOverview) {
        bakiOverview.debtors
            .filter { it.currentBalance > 0.001 }
            .sortedByDescending { it.currentBalance }
            .take(3)
    }
    val topLowStock = remember(lowStockProducts) {
        lowStockProducts.take(3)
    }
    val todayTip = remember(businessStrategy) {
        extractSimpleTipLine(businessStrategy, maxWords = 25)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Existing glowing sales ring with today's sales and order count
        if (canSeeSalesSummary) {
            item(key = "simple_sales_ring") {
                val ringProgress = remember(financials.todaySales, homeInsights.yesterdaySales) {
                    when {
                        homeInsights.yesterdaySales > 0.0 ->
                            (financials.todaySales / homeInsights.yesterdaySales).toFloat().coerceIn(0.08f, 1f)
                        financials.todaySales > 0.0 -> 0.72f
                        else -> 0f
                    }
                }
                SimpleSalesRingHero(
                    todaySales = financials.todaySales,
                    ordersCount = financials.todayOrdersCount,
                    progress = ringProgress,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    onClick = onNavigateToSalesSummary
                )
            }
        }

        // 2. Three big quick buttons: "বিক্রি", "বাকি আদায়", "মাল যোগ"
        if (canSell || canReceivePayment || canAccessBaki || canAddEditProducts) {
            item(key = "simple_quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canSell) {
                        SimpleQuickActionButton(
                            label = ShopLocale.get("simple_btn_sell", currentLanguage),
                            icon = Icons.Default.ShoppingBag,
                            isPrimary = true,
                            onClick = onNavigateToSell,
                            modifier = Modifier.weight(1f),
                            testTag = "home_new_sale_btn"
                        )
                    }
                    if (canReceivePayment || canAccessBaki) {
                        SimpleQuickActionButton(
                            label = ShopLocale.get("simple_btn_collect_baki", currentLanguage),
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            isPrimary = false,
                            onClick = onQuickPaymentIn,
                            modifier = Modifier.weight(1f),
                            testTag = "home_quick_payment_in_btn"
                        )
                    }
                    if (canAddEditProducts) {
                        SimpleQuickActionButton(
                            label = ShopLocale.get("simple_btn_add_stock", currentLanguage),
                            icon = Icons.Default.AddBox,
                            isPrimary = false,
                            onClick = onAddProduct,
                            modifier = Modifier.weight(1f),
                            testTag = "home_quick_add_product_btn"
                        )
                    }
                }
            }
        }

        // 3. Card "কার কাছে পাব" with top 3 customers owing money + "সব দেখুন"
        if (canAccessBaki) {
            item(key = "simple_who_owes_card") {
                BentoGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_baki_card",
                    padding = PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ShopLocale.get("simple_card_who_owes", currentLanguage),
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            SimpleSeeAllButton(
                                text = ShopLocale.get("simple_see_all", currentLanguage),
                                accentColor = VividGreen,
                                onClick = onNavigateToBaki,
                                testTag = "simple_baki_see_all_btn"
                            )
                        }

                        if (topDebtors.isEmpty()) {
                            Text(
                                text = ShopLocale.get("simple_empty_baki", currentLanguage),
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            topDebtors.forEachIndexed { idx, summary ->
                                if (idx > 0) {
                                    HorizontalDivider(color = Color(0x1AFFFFFF))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = summary.customer.name.ifBlank {
                                            if (currentLanguage == AppLanguage.BN) "কাস্টমার" else "Customer"
                                        },
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp)
                                    )
                                    Text(
                                        text = formatHomeMoney(summary.currentBalance, currentLanguage, isPrivacyHidden),
                                        color = amberAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
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

        // 4. Card "কী মাল কমে গেছে" with up to 3 low-stock items + "সব দেখুন"
        if (canViewStock) {
            item(key = "simple_low_stock_card") {
                BentoGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_low_stock_card",
                    padding = PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ShopLocale.get("simple_card_low_stock", currentLanguage),
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            SimpleSeeAllButton(
                                text = ShopLocale.get("simple_see_all", currentLanguage),
                                accentColor = amberAccent,
                                onClick = onOpenLowStock,
                                testTag = "simple_low_stock_see_all_btn"
                            )
                        }

                        if (topLowStock.isEmpty()) {
                            Text(
                                text = ShopLocale.get("simple_empty_low_stock", currentLanguage),
                                color = VividGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            topLowStock.forEachIndexed { idx, product ->
                                if (idx > 0) {
                                    HorizontalDivider(color = Color(0x1AFFFFFF))
                                }
                                val displayName = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) {
                                    product.nameBn
                                } else {
                                    product.name
                                }
                                val unitLabel = ShopLocale.displayUnit(product.unit, currentLanguage)
                                val qtyText = "${ShopLocale.formatNumber(product.stock, currentLanguage)} $unitLabel"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayName,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp)
                                    )
                                    Text(
                                        text = qtyText,
                                        color = if (product.stock <= 0.0) AlertRed else amberAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
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

        // 5. Short card "আজকের হিসাব": cash in hand and today's profit only
        item(key = "simple_todays_accounts_card") {
            BentoGlassCard(
                modifier = Modifier.fillMaxWidth(),
                testTag = "simple_todays_accounts_card",
                padding = PaddingValues(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShopLocale.get("simple_card_todays_accounts", currentLanguage),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        SimpleSeeAllButton(
                            text = "মাসের হিসাব",
                            accentColor = VividGreen,
                            onClick = onOpenMonthlyReport,
                            testTag = "simple_monthly_report_btn"
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = ShopLocale.get("simple_cash_in_hand", currentLanguage),
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatHomeMoney(cashInHand, currentLanguage, isPrivacyHidden),
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("simple_cash_in_hand_value")
                            )
                        }
                        if (showProfitInSimpleMode && canSeeProfit) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(GlassBorder)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = ShopLocale.get("simple_todays_profit", currentLanguage),
                                    color = TextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatHomeMoney(financials.todayProfit, currentLanguage, isPrivacyHidden),
                                    color = if (financials.todayProfit < 0.0) AlertRed else VividGreen,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("simple_todays_profit_value")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Short AI tip card at bottom (hidden if older than today, at most 25 words)
        if (canViewStrategy && !todayTip.isNullOrBlank()) {
            item(key = "simple_business_strategy") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1A2218),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simple_business_strategy")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = amberAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = todayTip,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleQuickActionButton(
    label: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPrimary) Color(0x667EEB4B) else Color(0x30FFFFFF)
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .clip(shape)
            .background(
                brush = if (isPrimary) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF234219), Color(0xFF152910))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xEB222622), Color(0xEC141614))
                    )
                }
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) VividGreen else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SimpleSeeAllButton(
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = Modifier
            .defaultMinSize(minHeight = 52.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SimpleSalesRingHero(
    todaySales: Double,
    ordersCount: Long,
    progress: Float,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    onClick: () -> Unit
) {
    val numberPart = remember(todaySales, currentLanguage, isPrivacyHidden) {
        if (isPrivacyHidden) {
            "••••"
        } else if (currentLanguage == AppLanguage.BN) {
            ShopLocale.formatMoney(todaySales, currentLanguage).replace(ShopLocale.CURRENCY, "").trim()
        } else {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                val hasCents = abs(todaySales - todaySales.roundToInt()) >= 0.01
                minimumFractionDigits = if (hasCents) 2 else 0
                maximumFractionDigits = if (hasCents) 2 else 0
            }
            nf.format(todaySales)
        }
    }

    Box(
        modifier = Modifier
            .size(214.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.36f),
                            VividGreen.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.56f, size.height * 0.68f),
                        radius = size.width * 0.72f
                    )
                )
            }
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .testTag("home_todays_summary_card")
        )
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f

            // Inner dark glass disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF282D28), Color(0xFF131613)),
                    center = Offset(size.width * 0.42f, size.height * 0.34f),
                    radius = size.width * 0.65f
                ),
                radius = (size.minDimension / 2f) - strokePx * 0.45f
            )

            // Outer glass bezel
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x55FFFFFF), Color(0x14FFFFFF))
                ),
                radius = (size.minDimension / 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Background circular track
            drawArc(
                color = Color(0x28FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Vivid green progress arc
            if (progress > 0f) {
                val sweep = 360f * progress
                drawArc(
                    color = VividGreen.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx + 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF7BFF48),
                            VividGreen,
                            Color(0xFF3E9C1A),
                            Color(0xFF7BFF48)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = ShopLocale.get("simple_todays_sales", currentLanguage),
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VividGreen, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)) {
                        append(ShopLocale.CURRENCY)
                    }
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)) {
                        append(numberPart)
                    }
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("home_tile_today_sales")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (currentLanguage == AppLanguage.BN) {
                    "${ShopLocale.formatNumber(ordersCount, currentLanguage)}টি অর্ডার"
                } else {
                    if (ordersCount == 1L) "1 order" else "$ordersCount orders"
                },
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun DetailedHomeLayout(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    bakiOverview: BakiOverview,
    totalBalance: Double,
    moneyNow: MoneyNowSummary,
    lowStockProducts: List<Product>,
    sevenDayCashflow: SevenDayCashflowSummary,
    recentSales: List<Sale>,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSeeSalesSummary: Boolean,
    canSeeProfit: Boolean,
    canAccessBaki: Boolean,
    canReceivePayment: Boolean,
    canAccessCashAndBank: Boolean,
    canAccessSuppliers: Boolean,
    canPaySupplier: Boolean,
    canViewStock: Boolean,
    canAddEditProducts: Boolean,
    canViewReports: Boolean,
    canAccessExpenses: Boolean,
    canSeeHistory: Boolean,
    canSell: Boolean = true,
    onNavigateToSell: () -> Unit = {},
    onAddProduct: () -> Unit = {},
    onOpenSalesSummary: () -> Unit,
    onOpenBaki: () -> Unit,
    onOpenCashAndBank: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenLowStock: () -> Unit,
    onRestockProduct: (Product) -> Unit,
    onOpenCashflowDetail: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSaleDetail: (Sale) -> Unit,
    onQuickPaymentIn: () -> Unit,
    onQuickPaymentOut: () -> Unit,
    onQuickPurchase: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickAddParty: () -> Unit,
    businessStrategy: SavedBusinessStrategy? = null,
    canViewStrategy: Boolean = false,
    canRefreshStrategy: Boolean = false,
    isStrategyRefreshing: Boolean = false,
    strategyRefreshesRemaining: Int = 0,
    onRefreshStrategy: () -> Unit = {},
    onStrategyAction: (StrategyActionTarget) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Top Quick Action Strip: Big Sell Button + Smaller Add Product Button
        if (canSell || canAddEditProducts) {
            item(key = "detailed_top_sell_add_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_top_actions_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSell) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.85f)),
                            modifier = Modifier
                                .weight(1.45f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF77EE42), VividGreen, Color(0xFF3E9C1A))
                                    )
                                )
                                .clickable(onClick = onNavigateToSell)
                                .testTag("home_new_sale_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color(0xFF0A1406),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "নতুন বিক্রি" else "Sell",
                                    color = Color(0xFF0A1406),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (canAddEditProducts) {
                        BentoGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            onClick = onAddProduct,
                            testTag = "home_add_product_btn",
                            padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBox,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ShopLocale.get("add_product", currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 13.5.sp,
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
        }
        // 1. Hero TODAY glowing card (NO Profile 60%, NO % vs expecting; real % vs yesterday only when yesterdaySales > 0)
        if (canSeeSalesSummary) {
            item(key = "detailed_hero_today") {
                DetailedTodayHeroCard(
                    financials = financials,
                    homeInsights = homeInsights,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    canSeeProfit = canSeeProfit,
                    onClick = onOpenSalesSummary
                )
            }
        }

        // 1B. Business Strategy card directly below Today's Summary (hidden for staff without Reports permission)
        if (canViewStrategy && businessStrategy != null) {
            item(key = "detailed_business_strategy") {
                BusinessStrategyCard(
                    strategy = businessStrategy,
                    isCompactSingleActionMode = false,
                    showRefreshButton = canRefreshStrategy,
                    isRefreshing = isStrategyRefreshing,
                    refreshesRemaining = strategyRefreshesRemaining,
                    onRefresh = onRefreshStrategy,
                    onActionClick = onStrategyAction
                )
            }
        }

        // 2. Asymmetric 2-Column Bento: Left = TO RECEIVE, Right = CASH & BANK + TO GIVE
        val showLeftBaki = canAccessBaki || canReceivePayment
        val showRightTopCash = canAccessCashAndBank
        val showRightBottomGive = canAccessSuppliers || canPaySupplier

        if (showLeftBaki || showRightTopCash || showRightBottomGive) {
            item(key = "detailed_bento_money") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showLeftBaki) {
                        BentoGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = onOpenBaki,
                            testTag = "home_to_receive_card",
                            padding = PaddingValues(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "পাব" else "TO RECEIVE",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatHomeMoney(bakiOverview.totalBakiOwed, currentLanguage, isPrivacyHidden),
                                        color = TextPrimary,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "বাকি • ${ShopLocale.formatNumber(bakiOverview.debtorCount, currentLanguage)} জন"
                                        } else {
                                            "Baki • ${bakiOverview.debtorCount} customers"
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val topDebtors = bakiOverview.debtors.take(3)
                                    if (topDebtors.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0x185DD62C)
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "All settled",
                                                color = VividGreen,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        topDebtors.forEach { debtor ->
                                            val initial = debtor.customer.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF282C28),
                                                border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = initial,
                                                        color = TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRightTopCash || showRightBottomGive) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showRightTopCash) {
                                BentoGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClick = onOpenCashAndBank,
                                    testTag = "home_cash_bank_card",
                                    padding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "ক্যাশ ও ব্যাংক" else "CASH & BANK",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = formatHomeMoney(totalBalance, currentLanguage, isPrivacyHidden),
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (showRightBottomGive) {
                                BentoGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onClick = onOpenSuppliers,
                                    testTag = "home_to_give_card",
                                    padding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.BN) "দেব" else "TO GIVE",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatHomeMoney(moneyNow.debtToSuppliers, currentLanguage, isPrivacyHidden),
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) {
                                                "${ShopLocale.formatNumber(moneyNow.suppliersCount, currentLanguage)} মহাজন"
                                            } else {
                                                "${moneyNow.suppliersCount} suppliers"
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.5.sp,
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
        }

        // 3. LOW STOCK Split Card
        if (canViewStock) {
            item(key = "detailed_low_stock") {
                DetailedLowStockCard(
                    lowStockProducts = lowStockProducts,
                    currentLanguage = currentLanguage,
                    canAddEdit = canAddEditProducts,
                    onOpenLowStock = onOpenLowStock,
                    onRestockProduct = onRestockProduct
                )
            }
        }

        // 4. 7-DAY CASHFLOW Card
        if (canViewReports || canAccessCashAndBank) {
            item(key = "detailed_cashflow") {
                DetailedSevenDayCashflowCard(
                    summary = sevenDayCashflow,
                    currentLanguage = currentLanguage,
                    isPrivacyHidden = isPrivacyHidden,
                    onClick = onOpenCashflowDetail
                )
            }
        }

        // 5. 5 Circular Quick Actions Row (permission-filtered, localized EN & BN, real actions only)
        val quickActions = buildList {
            if (canReceivePayment || canAccessBaki) {
                add(
                    Triple(
                        if (currentLanguage == AppLanguage.BN) "বাকি আদায়" else "Payment In",
                        Icons.Default.CurrencyExchange,
                        onQuickPaymentIn
                    ) to "payment_in"
                )
            }
            if (canPaySupplier || canAccessSuppliers) {
                add(
                    Triple(
                        if (currentLanguage == AppLanguage.BN) "পরিশোধ" else "Payment Out",
                        Icons.Default.SwapHoriz,
                        onQuickPaymentOut
                    ) to "payment_out"
                )
            }
            if (canAccessSuppliers) {
                add(
                    Triple(
                        if (currentLanguage == AppLanguage.BN) "ক্রয়" else "Purchase",
                        Icons.Default.ShoppingCart,
                        onQuickPurchase
                    ) to "purchase"
                )
            }
            if (canAccessExpenses) {
                add(
                    Triple(
                        if (currentLanguage == AppLanguage.BN) "খরচ" else "Expense",
                        Icons.Default.AttachMoney,
                        onQuickExpense
                    ) to "expense"
                )
            }
            if (canAccessBaki || canAccessSuppliers) {
                add(
                    Triple(
                        if (currentLanguage == AppLanguage.BN) "পার্টি যোগ" else "Add Party",
                        Icons.Default.PersonAdd,
                        onQuickAddParty
                    ) to "add_party"
                )
            }
        }

        if (quickActions.isNotEmpty()) {
            item(key = "detailed_quick_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickActions.forEach { (triple, tagKey) ->
                        val (label, icon, action) = triple
                        QuickActionCircleButton(
                            label = label,
                            icon = icon,
                            onClick = action,
                            modifier = Modifier.weight(1f),
                            testTag = "home_qa_$tagKey"
                        )
                    }
                }
            }
        }

        // 6. RECENT SALES Card
        if (canSeeSalesSummary) {
            item(key = "detailed_recent_sales") {
                BentoGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_recent_sales_card",
                    padding = PaddingValues(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = canSeeHistory, onClick = onOpenHistory)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সাম্প্রতিক বিক্রি" else "RECENT SALES",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "History",
                                tint = TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (recentSales.isEmpty()) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "এখনো কোনো বিক্রি হয়নি" else "No sales recorded yet. Tap SELL below.",
                                color = TextMuted,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            recentSales.take(4).forEachIndexed { index, sale ->
                                val rawName = sale.customerName.trim()
                                val displayName = if (rawName.isBlank() || rawName.equals("Cash Sale", ignoreCase = true)) {
                                    if (currentLanguage == AppLanguage.BN) "নগদ বিক্রি" else "Cash Sale"
                                } else {
                                    rawName
                                }
                                val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = canSeeHistory) { onOpenSaleDetail(sale) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF282D28),
                                            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = initial,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = displayName,
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = formatHomeMoney(sale.grandTotal, currentLanguage, isPrivacyHidden),
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (index < recentSales.take(4).lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0x10FFFFFF))
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

@Composable
private fun DetailedTodayHeroCard(
    financials: StoreFinancials,
    homeInsights: HomeInsights,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    canSeeProfit: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.55f),
                        radius = size.width * 0.65f
                    )
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.3.dp, Color(0xFF53DA24))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xF5152012), Color(0xF70C120A))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                        .testTag("home_todays_summary_card")
                )
                // Real sparkline wave rendered behind hero sales figure
                if (homeInsights.todaySparklinePoints.size >= 2) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 34.dp, bottom = 36.dp)
                    ) {
                        val pts = homeInsights.todaySparklinePoints
                        val stepX = size.width / (pts.size - 1).coerceAtLeast(1)
                        val coords = pts.mapIndexed { i, v ->
                            val y = size.height - (v.coerceIn(0f, 1f) * size.height * 0.82f) - size.height * 0.08f
                            Offset(i * stepX, y)
                        }
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 0 until coords.lastIndex) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(VividGreen.copy(alpha = 0.22f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = VividGreen.copy(alpha = 0.28f),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = linePath,
                            color = VividGreen.copy(alpha = 0.85f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top chip: TODAY (no "% vs yesterday" fake comparison chip)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x305DD62C),
                            border = BorderStroke(1.dp, Color(0x455DD62C))
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "আজ" else "TODAY",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big VividGreen Sales Figure
                    Text(
                        text = formatHomeMoney(financials.todaySales, currentLanguage, isPrivacyHidden),
                        color = VividGreen,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("home_tile_today_sales")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom 3-stat row (PROFIT | ORDERS | ITEMS SOLD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canSeeProfit) {
                            HeroSubStat(
                                label = if (currentLanguage == AppLanguage.BN) "লাভ" else "PROFIT",
                                value = formatHomeMoney(financials.todayProfit, currentLanguage, isPrivacyHidden),
                                modifier = Modifier.weight(1f),
                                testTag = "home_tile_net_profit"
                            )
                        }
                        HeroSubStat(
                            label = if (currentLanguage == AppLanguage.BN) "অর্ডার" else "ORDERS",
                            value = ShopLocale.formatNumber(financials.todayOrdersCount, currentLanguage),
                            modifier = Modifier.weight(1f),
                            testTag = "home_tile_orders_completed"
                        )
                        HeroSubStat(
                            label = if (currentLanguage == AppLanguage.BN) "বিক্রিত পণ্য" else "ITEMS SOLD",
                            value = ShopLocale.formatNumber(financials.todayItemsSold, currentLanguage),
                            modifier = Modifier.weight(1f),
                            testTag = "home_tile_items_sold"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSubStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )
    }
}

@Composable
private fun DetailedLowStockCard(
    lowStockProducts: List<Product>,
    currentLanguage: AppLanguage,
    canAddEdit: Boolean,
    onOpenLowStock: () -> Unit,
    onRestockProduct: (Product) -> Unit
) {
    val amber = Color(0xFFFFA726)

    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenLowStock,
        testTag = "home_low_stock_card",
        padding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left count pane
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "স্বল্প মজুদ" else "LOW STOCK",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = ShopLocale.formatNumber(lowStockProducts.size.toLong(), currentLanguage),
                    color = if (lowStockProducts.isNotEmpty()) amber else VividGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(GlassBorder)
            )

            // Right top-2 items pane
            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lowStockProducts.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "সব পণ্যের মজুদ পর্যাপ্ত" else "All stock levels healthy",
                        color = VividGreen,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    lowStockProducts.take(2).forEach { prod ->
                        val isOut = prod.stock <= 0.0
                        val badgeColor = if (isOut) AlertRed else amber
                        val maxRef = (prod.lowStockLimit * 2.0).coerceAtLeast(5.0).toFloat()
                        val fraction = (prod.stock.toFloat() / maxRef).coerceIn(0.08f, 1f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = canAddEdit) { onRestockProduct(prod) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prod.name,
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${ShopLocale.formatNumber(prod.stock, currentLanguage)} ${ShopLocale.displayUnit(prod.unit, currentLanguage)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = when {
                                            isOut && currentLanguage == AppLanguage.BN -> "শেষ"
                                            isOut -> "Out"
                                            currentLanguage == AppLanguage.BN -> "কম"
                                            else -> "Low"
                                        },
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x22FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = if (isOut) 0.1f else fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(badgeColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedSevenDayCashflowCard(
    summary: SevenDayCashflowSummary,
    currentLanguage: AppLanguage,
    isPrivacyHidden: Boolean,
    onClick: () -> Unit
) {
    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        testTag = "home_cashflow_card",
        padding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "৭ দিনের ক্যাশফ্লো" else "7-DAY CASHFLOW",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.85f)) {
                    CashflowDualLineCanvas(
                        points = summary.points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        summary.points.forEach { pt ->
                            Text(
                                text = pt.dayLabel,
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1.15f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(VividGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "আয়" else "In",
                            color = VividGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatHomeMoney(summary.totalIn, currentLanguage, isPrivacyHidden),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "ব্যয়" else "Out",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatHomeMoney(summary.totalOut, currentLanguage, isPrivacyHidden),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
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

@Composable
private fun CashflowDualLineCanvas(
    points: List<DailyCashflowPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val maxVal = points.maxOf { maxOf(it.cashIn, it.cashOut) }.coerceAtLeast(1.0)
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)

        fun buildSmoothPath(values: List<Double>): Path {
            val coords = values.mapIndexed { i, v ->
                val ratio = (v / maxVal).toFloat().coerceIn(0f, 1f)
                val y = size.height - (ratio * size.height * 0.80f) - size.height * 0.08f
                Offset(i * stepX, y)
            }
            return Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.lastIndex) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
        }

        val outPath = buildSmoothPath(points.map { it.cashOut })
        val inPath = buildSmoothPath(points.map { it.cashIn })
        val inFill = Path().apply {
            addPath(inPath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        // Subtle area glow under In line
        drawPath(
            path = inFill,
            brush = Brush.verticalGradient(
                colors = listOf(VividGreen.copy(alpha = 0.22f), Color.Transparent)
            )
        )

        // Out line (soft gray)
        drawPath(
            path = outPath,
            color = TextSecondary.copy(alpha = 0.55f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // In line glow + crisp stroke
        drawPath(
            path = inPath,
            color = VividGreen.copy(alpha = 0.30f),
            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = inPath,
            color = VividGreen,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun QuickActionCircleButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x45FFFFFF), Color(0x14FFFFFF))
                )
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF282D28), Color(0xFF151815))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.1).sp,
            maxLines = 2,
            softWrap = true,
            textAlign = TextAlign.Center
        )
    }
}
