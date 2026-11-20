package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale

fun shopTypeIcon(type: ShopType): ImageVector {
    return when (type) {
        ShopType.GROCERY -> Icons.Default.ShoppingBasket
        ShopType.HARDWARE -> Icons.Default.Build
        ShopType.PHARMACY -> Icons.Default.LocalPharmacy
        ShopType.CLOTHING -> Icons.Default.Checkroom
        ShopType.ELECTRONICS -> Icons.Default.Devices
        ShopType.STATIONERY -> Icons.Default.EditNote
        ShopType.GENERAL -> Icons.Default.Storefront
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopTypePickerGrid(
    selectedType: ShopType?,
    currentLanguage: AppLanguage,
    onSelect: (ShopType) -> Unit,
    modifier: Modifier = Modifier,
    showSelectedPreview: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShopType.entries.forEach { type ->
                ShopTypeTile(
                    type = type,
                    isSelected = selectedType == type,
                    currentLanguage = currentLanguage,
                    onClick = { onSelect(type) }
                )
            }
        }

        if (showSelectedPreview && selectedType != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x1E5DD62C),
                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_type_preview")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = ShopTypeCatalog.subtitle(selectedType, currentLanguage),
                        color = VividGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        softWrap = true
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ShopTypeCatalog.startingCategories(selectedType)) { cat ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x26FFFFFF),
                                border = BorderStroke(1.dp, Color(0x24FFFFFF))
                            ) {
                                Text(
                                    text = ShopLocale.displayCategory(cat, currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
private fun ShopTypeTile(
    type: ShopType,
    isSelected: Boolean,
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isSelected) Color(0x305DD62C) else Color(0x1EFFFFFF),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) VividGreen else GlassBorder
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .testTag("shop_type_tile_${type.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VividGreen.copy(alpha = 0.22f) else Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = shopTypeIcon(type),
                    contentDescription = null,
                    tint = if (isSelected) VividGreen else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = ShopTypeCatalog.displayName(type, currentLanguage),
                color = if (isSelected) VividGreen else TextPrimary,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                softWrap = true
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = VividGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoShopTypePickerDialog(
    currentLanguage: AppLanguage,
    onSelectType: (ShopType) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(),
        modifier = Modifier.safeDialogWindowPadding()
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val shape = RoundedCornerShape(26.dp)
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color(0x45FFFFFF), Color(0x14FFFFFF)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xF71B201B), Color(0xFA111411))
                    )
                )
                .testTag("demo_shop_type_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ShopLocale.get("shop_type_demo_title", currentLanguage),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ShopLocale.get("shop_type_demo_sub", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("demo_shop_type_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = ShopLocale.get("cancel", currentLanguage),
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopType.entries.forEach { type ->
                        Surface(
                            onClick = { onSelectType(type) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x1EFFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 54.dp)
                                .testTag("shop_type_tile_${type.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VividGreen.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = shopTypeIcon(type),
                                        contentDescription = null,
                                        tint = VividGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = ShopTypeCatalog.displayName(type, currentLanguage),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ShopTypeCatalog.subtitle(type, currentLanguage),
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassOutlinedButton(
                    text = ShopLocale.get("cancel", currentLanguage),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "demo_shop_type_cancel_btn"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsShopTypeDialog(
    currentShopType: ShopType?,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onConfirmChange: (newType: ShopType, addSuggestedCategories: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingType by remember(currentShopType) { mutableStateOf<ShopType?>(null) }
    var addSuggestedCategories by remember { mutableStateOf(true) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(),
        modifier = Modifier.safeDialogWindowPadding()
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val shape = RoundedCornerShape(26.dp)
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color(0x45FFFFFF), Color(0x14FFFFFF)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xF71B201B), Color(0xFA111411))
                    )
                )
                .testTag(if (pendingType == null) "settings_shop_type_dialog" else "shop_type_confirm_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                if (pendingType == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ShopLocale.get("shop_type_picker_title", currentLanguage),
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ShopLocale.get("shop_type_change_desc", currentLanguage),
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = ShopLocale.get("cancel", currentLanguage),
                                tint = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ShopTypePickerGrid(
                            selectedType = currentShopType,
                            currentLanguage = currentLanguage,
                            onSelect = { chosen ->
                                pendingType = chosen
                            },
                            showSelectedPreview = false
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GlassOutlinedButton(
                        text = ShopLocale.get("cancel", currentLanguage),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_shop_type_cancel_btn"
                    )
                } else {
                    val chosen = pendingType!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                    Text(
                        text = ShopLocale.get("shop_type_change_title", currentLanguage),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x225DD62C),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = shopTypeIcon(chosen),
                                contentDescription = null,
                                tint = VividGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ShopTypeCatalog.displayName(chosen, currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ShopTypeCatalog.subtitle(chosen, currentLanguage),
                                    color = VividGreen,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = ShopLocale.get("shop_type_change_desc", currentLanguage),
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1CFFFFFF),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { addSuggestedCategories = !addSuggestedCategories }
                            .testTag("shop_type_suggestions_toggle")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = addSuggestedCategories,
                                    onCheckedChange = { addSuggestedCategories = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = VividGreen,
                                        checkmarkColor = DarkBackground,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Text(
                                    text = ShopLocale.get("shop_type_add_suggestions", currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (addSuggestedCategories) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, bottom = 2.dp)
                                ) {
                                    items(ShopTypeCatalog.startingCategories(chosen)) { cat ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0x20FFFFFF),
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                                        ) {
                                            Text(
                                                text = ShopLocale.displayCategory(cat, currentLanguage),
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = ShopLocale.get("cancel", currentLanguage),
                            onClick = { pendingType = null },
                            modifier = Modifier.weight(1f),
                            testTag = "shop_type_confirm_cancel_btn"
                        )
                        VividButton(
                            text = ShopLocale.get("shop_type_confirm_btn", currentLanguage),
                            onClick = { onConfirmChange(chosen, addSuggestedCategories) },
                            isLoading = isBusy,
                            modifier = Modifier.weight(1f),
                            testTag = "shop_type_confirm_btn"
                        )
                    }
                    }
                }
            }
        }
    }
}
