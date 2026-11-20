package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.util.LocalAppLanguage
import com.example.util.ShopLocale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import kotlin.math.roundToInt

enum class ShopNavTab {
    HOME,
    STOCK,
    BAKI,
    SELL,
    HISTORY,
    BUSINESS,
    MORE
}

@Composable
fun FloatingGlassPillBottomBar(
    currentTab: ShopNavTab,
    onTabSelected: (ShopNavTab) -> Unit,
    modifier: Modifier = Modifier,
    canSell: Boolean = true,
    canViewStock: Boolean = true,
    canSeeHistory: Boolean = true,
    canViewReports: Boolean = true,
    canAccessBaki: Boolean = true,
    isSimpleMode: Boolean = false,
    isFatherMode: Boolean = false
) {
    val currentLanguage = LocalAppLanguage.current
    val useSimpleTabs = isSimpleMode && !isFatherMode
    val leftCount = if (useSimpleTabs) 1 else 1 + (if (canViewStock) 1 else 0)
    val rightCount = if (useSimpleTabs) {
        (if (canAccessBaki) 1 else 0) + (if (canViewStock) 1 else 0)
    } else {
        (if (canSeeHistory) 1 else 0) + (if (canViewReports) 1 else 0)
    }
    val barHeight = when {
        isFatherMode -> 76.dp
        useSimpleTabs -> 70.dp
        else -> 66.dp
    }
    val orbSize = if (isFatherMode) 68.dp else 64.dp
    val orbFontSize = when {
        isFatherMode -> 15.sp
        useSimpleTabs -> 14.5.sp
        else -> 13.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating pill container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = Color(0xBB000000),
                    spotColor = Color(0x88000000)
                ),
            shape = RoundedCornerShape(36.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x3DFFFFFF), Color(0x14FFFFFF))
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = barHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF51D201D),
                                Color(0xF7131513)
                            )
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(if (useSimpleTabs) 1f else leftCount.toFloat().coerceAtLeast(1f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PillNavItem(
                        label = ShopLocale.get("nav_home", currentLanguage),
                        icon = Icons.Default.Home,
                        isSelected = currentTab == ShopNavTab.HOME,
                        onClick = { onTabSelected(ShopNavTab.HOME) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_home",
                        isFatherMode = isFatherMode,
                        isSimpleMode = useSimpleTabs
                    )

                    if (!useSimpleTabs && canViewStock) {
                        PillNavItem(
                            label = ShopLocale.get("nav_stock", currentLanguage),
                            icon = Icons.Default.Inventory2,
                            isSelected = currentTab == ShopNavTab.STOCK,
                            onClick = { onTabSelected(ShopNavTab.STOCK) },
                            modifier = Modifier.weight(1f),
                            testTag = "nav_stock",
                            isFatherMode = isFatherMode
                        )
                    }
                }

                // Placeholder space for the raised center SELL orb
                if (canSell) {
                    Spacer(modifier = Modifier.width(if (isFatherMode) 70.dp else 64.dp))
                }

                if (useSimpleTabs) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (canAccessBaki) {
                            PillNavItem(
                                label = ShopLocale.get("nav_baki", currentLanguage),
                                icon = Icons.Default.People,
                                isSelected = currentTab == ShopNavTab.BAKI,
                                onClick = { onTabSelected(ShopNavTab.BAKI) },
                                modifier = Modifier.weight(1f),
                                testTag = "nav_baki",
                                isFatherMode = false,
                                isSimpleMode = true
                            )
                        }
                        if (canViewStock) {
                            PillNavItem(
                                label = ShopLocale.get("nav_stock", currentLanguage),
                                icon = Icons.Default.Inventory2,
                                isSelected = currentTab == ShopNavTab.STOCK,
                                onClick = { onTabSelected(ShopNavTab.STOCK) },
                                modifier = Modifier.weight(1f),
                                testTag = "nav_stock",
                                isFatherMode = false,
                                isSimpleMode = true
                            )
                        }
                    }
                } else if (rightCount > 0) {
                    Row(
                        modifier = Modifier.weight(rightCount.toFloat()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (canSeeHistory) {
                            PillNavItem(
                                label = ShopLocale.get("nav_history", currentLanguage),
                                icon = Icons.Default.History,
                                isSelected = currentTab == ShopNavTab.HISTORY,
                                onClick = { onTabSelected(ShopNavTab.HISTORY) },
                                modifier = Modifier.weight(1f),
                                testTag = "nav_history",
                                isFatherMode = isFatherMode
                            )
                        }

                        if (canViewReports) {
                            PillNavItem(
                                label = ShopLocale.get("nav_business", currentLanguage),
                                icon = Icons.Default.Business,
                                isSelected = currentTab == ShopNavTab.BUSINESS,
                                onClick = { onTabSelected(ShopNavTab.BUSINESS) },
                                modifier = Modifier.weight(1f),
                                testTag = "nav_business",
                                isFatherMode = isFatherMode
                            )
                        }
                    }
                }
            }
        }

        // Big Raised Vivid Green SELL Orb in Center (if canSell)
        if (canSell) {
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .size(if (isFatherMode) 88.dp else 84.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ambient green halo behind orb
                Box(
                    modifier = Modifier
                        .size(if (isFatherMode) 88.dp else 84.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VividGreen.copy(alpha = 0.48f),
                                    VividGreen.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .size(orbSize)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = VividGreen,
                            ambientColor = VividGreen
                        )
                        .clip(CircleShape)
                        .clickable { onTabSelected(ShopNavTab.SELL) }
                        .testTag("nav_sell_fab"),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.2.dp, Color(0x66FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF9BFF68),
                                        VividGreen,
                                        Color(0xFF398C1A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ShopLocale.get("nav_sell", currentLanguage),
                            color = Color(0xFF0A1406),
                            fontSize = orbFontSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeftSideGlassMenu(
    storeName: String,
    currentTab: ShopNavTab,
    onTabSelected: (ShopNavTab) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    canSell: Boolean = true,
    canViewStock: Boolean = true,
    canSeeHistory: Boolean = true,
    canViewReports: Boolean = true,
    canAccessBaki: Boolean = true,
    isSimpleMode: Boolean = false,
    isFatherMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val useSimpleTabs = isSimpleMode && !isFatherMode
    val currentLanguage = LocalAppLanguage.current
    Surface(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
            )
            .width(216.dp)
            .fillMaxHeight()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x38FFFFFF), Color(0x14FFFFFF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xF51B1F1B), Color(0xF7111311))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1D3218),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = storeName,
                            tint = VividGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = storeName.ifBlank { "StoreOS" },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canSell) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = VividGreen,
                            ambientColor = VividGreen
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onTabSelected(ShopNavTab.SELL) }
                        .testTag("nav_sell_fab"),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF79F244), VividGreen, Color(0xFF42A81D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color(0xFF0A1406),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = ShopLocale.get("nav_sell", currentLanguage),
                                color = Color(0xFF0A1406),
                                fontSize = if (isFatherMode) 16.sp else 14.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SideMenuNavRow(
                    label = ShopLocale.get("nav_home", currentLanguage),
                    icon = Icons.Default.Home,
                    isSelected = currentTab == ShopNavTab.HOME,
                    onClick = { onTabSelected(ShopNavTab.HOME) },
                    testTag = "nav_home"
                )
                if (useSimpleTabs) {
                    if (canAccessBaki) {
                        SideMenuNavRow(
                            label = ShopLocale.get("nav_baki", currentLanguage),
                            icon = Icons.Default.People,
                            isSelected = currentTab == ShopNavTab.BAKI,
                            onClick = { onTabSelected(ShopNavTab.BAKI) },
                            testTag = "nav_baki"
                        )
                    }
                    if (canViewStock) {
                        SideMenuNavRow(
                            label = ShopLocale.get("nav_stock", currentLanguage),
                            icon = Icons.Default.Inventory2,
                            isSelected = currentTab == ShopNavTab.STOCK,
                            onClick = { onTabSelected(ShopNavTab.STOCK) },
                            testTag = "nav_stock"
                        )
                    }
                } else {
                    if (canViewStock) {
                        SideMenuNavRow(
                            label = ShopLocale.get("nav_stock", currentLanguage),
                            icon = Icons.Default.Inventory2,
                            isSelected = currentTab == ShopNavTab.STOCK,
                            onClick = { onTabSelected(ShopNavTab.STOCK) },
                            testTag = "nav_stock"
                        )
                    }
                    if (canSeeHistory) {
                        SideMenuNavRow(
                            label = ShopLocale.get("nav_history", currentLanguage),
                            icon = Icons.Default.History,
                            isSelected = currentTab == ShopNavTab.HISTORY,
                            onClick = { onTabSelected(ShopNavTab.HISTORY) },
                            testTag = "nav_history"
                        )
                    }
                    if (canViewReports) {
                        SideMenuNavRow(
                            label = ShopLocale.get("nav_business", currentLanguage),
                            icon = Icons.Default.Business,
                            isSelected = currentTab == ShopNavTab.BUSINESS,
                            onClick = { onTabSelected(ShopNavTab.BUSINESS) },
                            testTag = "nav_business"
                        )
                    }
                    SideMenuNavRow(
                        label = ShopLocale.get("nav_more", currentLanguage),
                        icon = Icons.Default.MoreHoriz,
                        isSelected = currentTab == ShopNavTab.MORE,
                        onClick = { onTabSelected(ShopNavTab.MORE) },
                        testTag = "nav_more"
                    )
                }
            }

            if (!useSimpleTabs && onOpenSettings != null) {
                SideMenuNavRow(
                    label = ShopLocale.get("settings_title", currentLanguage),
                    icon = Icons.Default.Settings,
                    isSelected = false,
                    onClick = onOpenSettings,
                    testTag = "side_nav_settings_btn"
                )
            }
        }
    }
}

@Composable
private fun SideMenuNavRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = shape,
        color = if (isSelected) Color(0x285DD62C) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) VividGreen else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VividGreen)
                )
            }
        }
    }
}

@Composable
fun PillNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    isFatherMode: Boolean = false,
    isSimpleMode: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) TextPrimary else TextMuted,
            modifier = Modifier.size(if (isFatherMode) 24.dp else if (isSimpleMode) 22.dp else 20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = if (isFatherMode || isSimpleMode) 12.sp else 9.5.sp,
            lineHeight = if (isFatherMode || isSimpleMode) 14.sp else 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(if (isFatherMode) 5.dp else 4.dp)
                .clip(CircleShape)
                .background(if (isSelected) VividGreen else Color.Transparent)
        )
    }
}

/**
 * Circular progress ring for stock quantity.
 * Amber when low, VividGreen when safe, Red when 0.
 */
@Composable
fun QuantityProgressRing(
    quantity: Double,
    lowStockLimit: Double,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    unitLabel: String? = null
) {
    val currentLanguage = LocalAppLanguage.current
    val isOut = quantity <= 0.0
    val isLow = quantity > 0.0 && quantity <= lowStockLimit

    val ringColor = when {
        isOut -> AlertRed
        isLow -> Color(0xFFFFB300) // Amber
        else -> VividGreen
    }

    val maxRef = (lowStockLimit * 3.0).coerceAtLeast(15.0)
    val progressFraction = if (isOut) 0f else (quantity.toFloat() / maxRef.toFloat()).coerceIn(0.08f, 1f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x88101310)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val strokeWidth = if (size >= 48.dp) 3.8.dp.toPx() else 3.dp.toPx()
            // Track
            drawArc(
                color = Color(0x28FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            if (isOut) {
                // Subtle red ring outline when out of stock
                drawArc(
                    color = AlertRed.copy(alpha = 0.45f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                // Soft glow halo
                drawArc(
                    color = ringColor.copy(alpha = 0.24f),
                    startAngle = -90f,
                    sweepAngle = 360f * progressFraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 2.0f, cap = StrokeCap.Round)
                )
                // Crisp value arc
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progressFraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        val qtyText = ShopLocale.formatNumber(quantity, currentLanguage)
        val numberFontSize = when {
            size >= 52.dp && qtyText.length <= 2 -> 15.sp
            size >= 52.dp && qtyText.length == 3 -> 13.sp
            size >= 52.dp -> 10.5.sp
            size > 42.dp && qtyText.length <= 2 -> 12.sp
            size > 42.dp && qtyText.length == 3 -> 10.5.sp
            size > 42.dp -> 9.sp
            qtyText.length <= 2 -> 10.sp
            else -> 8.5.sp
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 5.dp)
        ) {
            Text(
                text = qtyText,
                color = if (isOut) AlertRed else TextPrimary,
                fontSize = numberFontSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                lineHeight = numberFontSize
            )
            if (!unitLabel.isNullOrBlank()) {
                Text(
                    text = ShopLocale.displayUnit(unitLabel, currentLanguage),
                    color = TextSecondary,
                    fontSize = if (size >= 52.dp) 9.sp else 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (size >= 52.dp) 10.sp else 8.sp
                )
            }
        }
    }
}

/**
 * Interactive "Slide to confirm sale" widget
 */
@Composable
fun SlideToConfirmSale(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "Slide to confirm sale",
    resetKey: Any? = Unit
) {
    var offsetX by remember(resetKey) { mutableFloatStateOf(0f) }
    var isSuccess by remember(resetKey) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB242824), Color(0xEE121412))
                )
            )
            .clickable(enabled = false) {}
            .testTag("slide_to_confirm_container")
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbSizePx = with(density) { 50.dp.toPx() }
        val horizontalInsetPx = with(density) { 4.dp.toPx() }
        val maxOffsetPx = (widthPx - thumbSizePx - horizontalInsetPx * 2f).coerceAtLeast(0f)

        val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "sliderThumb")

        // Subtle outer border overlay
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(29.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (enabled) Color(0x3DFFFFFF) else Color(0x1CFFFFFF),
                        Color(0x12FFFFFF)
                    )
                )
            )
        ) {}

        // Filled active track behind thumb
        val fillWidthRatio = if (maxOffsetPx > 0f) (animatedOffsetX / maxOffsetPx).coerceIn(0f, 1f) else 0f
        if (fillWidthRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = fillWidthRatio)
                    .background(Color(0x335DD62C))
            )
        }

        // Centered prompt text
        Text(
            text = if (isSuccess) "Sale Confirmed!" else text,
            color = when {
                isSuccess -> VividGreen
                enabled -> TextPrimary
                else -> TextMuted
            },
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )

        // Subtle right chevron hint
        if (!isSuccess) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) TextSecondary else TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(18.dp)
            )
        }

        // Draggable glowing green thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((horizontalInsetPx + animatedOffsetX).roundToInt(), 0) }
                .size(50.dp)
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = VividGreen,
                    ambientColor = VividGreen
                )
                .clip(CircleShape)
                .background(
                    brush = if (enabled || isSuccess) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9BFF68),
                                VividGreen,
                                Color(0xFF3E9E1B)
                            )
                        )
                    } else {
                        SolidColor(Color(0xFF3A4436))
                    }
                )
                .draggable(
                    enabled = enabled && !isSuccess,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX + delta).coerceIn(0f, maxOffsetPx)
                        offsetX = newOffset
                    },
                    onDragStopped = {
                        if (offsetX >= maxOffsetPx * 0.82f) {
                            offsetX = maxOffsetPx
                            isSuccess = true
                            onConfirmed()
                        } else {
                            offsetX = 0f
                        }
                    }
                )
                .testTag("slide_thumb"),
            contentAlignment = Alignment.Center
        ) {
            if (isSuccess) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sale confirmed",
                    tint = DarkBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Returns matching Material icon for a category name
 */
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "grocery", "মুদি", "groceries", "rice & grains", "daily essentials", "packaged goods" -> Icons.Default.ShoppingBasket
        "oil & ghee", "spices", "dairy" -> Icons.Default.Kitchen
        "beverage", "beverages", "পানীয়", "drinks" -> Icons.Default.LocalDrink
        "snacks", "স্ন্যাক্স", "food" -> Icons.Default.Fastfood
        "hand tools", "power tools", "nuts & bolts" -> Icons.Default.Build
        "electrical", "cables & wires", "lighting" -> Icons.Default.ElectricalServices
        "paint" -> Icons.Default.FormatPaint
        "plumbing" -> Icons.Default.Plumbing
        "medicine", "ওষুধ", "pharmacy", "tablets & capsules", "syrup & drops", "first aid", "devices" -> Icons.Default.LocalPharmacy
        "baby care" -> Icons.Default.ChildCare
        "men's wear", "women's wear", "kids wear", "fabrics", "footwear", "accessories" -> Icons.Default.Checkroom
        "electronics", "ইলেকট্রনিক্স", "mobile accessories", "appliances", "batteries", "audio" -> Icons.Default.Devices
        "notebooks & paper", "pens & pencils", "office supplies", "art & craft", "files & folders", "school bags" -> Icons.Default.EditNote
        "household" -> Icons.Default.Home
        "personal care", "ব্যক্তিগত যত্ন", "cosmetics" -> Icons.Default.Spa
        else -> Icons.Default.Category
    }
}

@Composable
fun NoPermissionScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    currentLanguage: com.example.util.AppLanguage = LocalAppLanguage.current
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = ShopLocale.get("no_permission_title", currentLanguage),
                    tint = AlertRed,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = ShopLocale.get("no_permission_title", currentLanguage),
                    color = AlertRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ShopLocale.get("no_permission_desc", currentLanguage),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                if (onBack != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    GlassOutlinedButton(
                        text = ShopLocale.get("go_back", currentLanguage),
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        testTag = "no_permission_back_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun ProductThumbnail(
    photoUri: String,
    category: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 14.dp,
    storeId: String = "",
    productId: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolvedModel = remember(photoUri, storeId, productId, context) {
        com.example.util.ProductImageUtil.toCoilImageModel(
            context = context,
            photoUri = photoUri,
            storeId = storeId,
            productId = productId
        )
    }
    var loadFailed by remember(photoUri, storeId, productId, resolvedModel) { mutableStateOf(false) }
    val showImage = resolvedModel != null && !loadFailed

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0x20FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = if (showImage) null else contentDescription,
            tint = VividGreen,
            modifier = Modifier.size(iconSize)
        )
        if (showImage) {
            val imageModel: Any = remember(resolvedModel, context) {
                if (resolvedModel is java.io.File) {
                    val cacheKey = "${resolvedModel.absolutePath}_${resolvedModel.lastModified()}"
                    coil.request.ImageRequest.Builder(context)
                        .data(resolvedModel)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .build()
                } else {
                    resolvedModel!!
                }
            }
            AsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
