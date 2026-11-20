package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.ui.theme.AlertRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.AppLanguage
import com.example.util.ShopLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatherConfirmActionDialog(
    title: String,
    body: String? = null,
    details: List<Pair<String, String>> = emptyList(),
    confirmLabel: String,
    currentLanguage: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    testTag: String = "father_confirm_dialog"
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .safeDialogWindowPadding()
            .testTag(testTag)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val cardShape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(cardShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1F261F), Color(0xFF131613))
                    )
                ),
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VividGreen.copy(alpha = 0.18f))
                            .border(1.dp, VividGreen.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!body.isNullOrBlank()) {
                    Text(
                        text = body,
                        color = TextSecondary,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    )
                }

                if (details.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1B201B),
                        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = TextSecondary,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = value,
                                        color = TextPrimary,
                                        fontSize = 14.5.sp,
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

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x3AFFFFFF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("father_confirm_cancel_btn")
                    ) {
                        Text(
                            text = ShopLocale.get("cancel", currentLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VividGreen,
                            contentColor = Color(0xFF0A1406)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("father_confirm_action_btn")
                    ) {
                        Text(
                            text = confirmLabel,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatherLockInfoDialog(
    currentLanguage: AppLanguage,
    isOwner: Boolean,
    onTryDetailedMode: () -> Unit,
    onTurnOffFatherMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val amberAccent = Color(0xFFFFB300)
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .safeDialogWindowPadding()
            .testTag("father_lock_info_dialog")
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val cardShape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(cardShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF232219), Color(0xFF141614))
                    )
                ),
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(1.2.dp, amberAccent.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(amberAccent.copy(alpha = 0.20f))
                            .border(1.dp, amberAccent.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = amberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ShopLocale.get("father_mode_chip", currentLanguage),
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ShopLocale.get("father_mode_subtitle", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("father_lock_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1C201C),
                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ShopLocale.get("father_mode_lock_info", currentLanguage),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                OutlinedButton(
                    onClick = onTryDetailedMode,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x3AFFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("father_try_detailed_btn")
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "বিস্তারিত মোড খুলুন" else "Detailed Mode",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isOwner) {
                    Button(
                        onClick = onTurnOffFatherMode,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = amberAccent,
                            contentColor = Color(0xFF1A1200)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("father_exit_btn")
                    ) {
                        Text(
                            text = ShopLocale.get("father_mode_exit_btn", currentLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBinDialog(
    binnedProducts: List<Product>,
    currentLanguage: AppLanguage,
    isBusy: Boolean,
    onRestore: (Product) -> Unit,
    onEmptyBin: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmEmpty by remember(binnedProducts.size) { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .safeDialogWindowPadding()
            .testTag("product_bin_dialog")
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val cardShape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(cardShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1D221D), Color(0xFF121512))
                    )
                ),
            shape = cardShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color(0x38FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x28FFB300))
                            .border(1.dp, Color(0x55FFB300), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ShopLocale.get("product_bin_title", currentLanguage)} (${binnedProducts.size})",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ShopLocale.get("product_bin_desc", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("product_bin_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                if (binnedProducts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1B1F1B),
                        border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ShopLocale.get("product_bin_empty_lbl", currentLanguage),
                            color = TextMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 100.dp, max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(binnedProducts, key = { it.id }) { p ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1C201C),
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bin_product_${p.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.name,
                                            color = TextPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ShopLocale.formatNumber(p.stock, currentLanguage)} ${ShopLocale.displayUnit(p.unit, currentLanguage)} · ${ShopLocale.formatMoney(p.sellPrice, currentLanguage)}",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = { onRestore(p) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VividGreen,
                                            contentColor = Color(0xFF0A1406)
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 44.dp)
                                            .testTag("bin_restore_${p.id}")
                                    ) {
                                        Text(
                                            text = ShopLocale.get("product_bin_restore_btn", currentLanguage),
                                            fontSize = 12.5.sp,
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

                    if (confirmEmpty) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AlertRed.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = ShopLocale.format(
                                        "product_bin_empty_confirm",
                                        currentLanguage,
                                        binnedProducts.size
                                    ),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { confirmEmpty = false },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text(
                                            text = ShopLocale.get("cancel", currentLanguage),
                                            color = TextSecondary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            confirmEmpty = false
                                            onEmptyBin()
                                        },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AlertRed,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("product_bin_empty_confirm_btn")
                                    ) {
                                        Text(
                                            text = ShopLocale.get("father_confirm_btn", currentLanguage),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { confirmEmpty = true },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("product_bin_empty_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ShopLocale.get("product_bin_empty_btn", currentLanguage),
                                fontSize = 14.sp,
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
}
