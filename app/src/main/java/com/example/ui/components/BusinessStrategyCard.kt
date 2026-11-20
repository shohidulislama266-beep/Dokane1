package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.StrategyActionTarget
import com.example.domain.BusinessStrategyBanglaBuilder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen

/**
 * Business Strategy card ("আজকের পরামর্শ") shown on Home directly below Today's Summary.
 * Always rendered in plain, simple Bangla regardless of app language.
 */
@Composable
fun BusinessStrategyCard(
    strategy: SavedBusinessStrategy,
    isCompactSingleActionMode: Boolean,
    showRefreshButton: Boolean,
    isRefreshing: Boolean,
    refreshesRemaining: Int,
    onRefresh: () -> Unit,
    onActionClick: (StrategyActionTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    val timeLabel = BusinessStrategyBanglaBuilder.formatBanglaTime(strategy.generatedAtMs)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xF01C2819), Color(0xF0121612))
                )
            )
            .testTag("home_business_strategy_card"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(VividGreen.copy(alpha = 0.55f), Color(0x18FFFFFF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompactSingleActionMode) 16.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactSingleActionMode) 12.dp else 10.dp)
        ) {
            // Header row: "আজকের পরামর্শ" + time made + optional Refresh button (non-Father Owner only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VividGreen.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.size(if (isCompactSingleActionMode) 36.dp else 32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "আজকের পরামর্শ",
                                tint = VividGreen,
                                modifier = Modifier.size(if (isCompactSingleActionMode) 19.dp else 17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আজকের পরামর্শ",
                            color = TextPrimary,
                            fontSize = if (isCompactSingleActionMode) 17.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("strategy_title")
                        )
                        Text(
                            text = "তৈরি: $timeLabel",
                            color = TextSecondary,
                            fontSize = if (isCompactSingleActionMode) 12.5.sp else 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("strategy_time_badge")
                        )
                    }
                }

                if (showRefreshButton) {
                    val remainingBn = BusinessStrategyBanglaBuilder.formatIntBn(refreshesRemaining.coerceAtLeast(0))
                    val refreshShape = RoundedCornerShape(12.dp)
                    Surface(
                        shape = refreshShape,
                        color = if (refreshesRemaining > 0) Color(0xFF1A2916) else Color(0xFF1D201D),
                        border = BorderStroke(
                            1.dp,
                            if (refreshesRemaining > 0) VividGreen.copy(alpha = 0.65f) else Color(0x28FFFFFF)
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(refreshShape)
                            .clickable(enabled = !isRefreshing, onClick = onRefresh)
                            .testTag("strategy_refresh_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    color = VividGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "রিফ্রেশ",
                                    tint = if (refreshesRemaining > 0) VividGreen else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "রিফ্রেশ ($remainingBn)",
                                color = if (refreshesRemaining > 0) VividGreen else TextSecondary,
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

            if (isCompactSingleActionMode) {
                // Simple & Father mode: show ONLY the first action in larger text + direct action button
                val firstAction = strategy.actions.firstOrNull()
                if (firstAction != null) {
                    val target = StrategyActionTarget.fromWireKey(firstAction.target)
                    val btnText = BusinessStrategyBanglaBuilder.actionButtonLabel(target)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x99111711),
                        border = BorderStroke(1.dp, Color(0x24FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = firstAction.text,
                                color = TextPrimary,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp,
                                modifier = Modifier.testTag("strategy_action_0")
                            )
                            StrategyActionButton(
                                label = btnText,
                                large = true,
                                testTag = "strategy_action_btn_0",
                                onClick = { onActionClick(target) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Detailed mode: Diagnosis + Focus line + 3 single-line actions with direct buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strategy.diagnosis,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 19.sp,
                        modifier = Modifier.testTag("strategy_diagnosis_text")
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VividGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strategy.focusLine,
                            color = VividGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("strategy_focus_text")
                        )
                    }
                }

                HorizontalDivider(color = Color(0x18FFFFFF))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    strategy.actions.take(3).forEachIndexed { idx, action ->
                        val target = StrategyActionTarget.fromWireKey(action.target)
                        val btnText = BusinessStrategyBanglaBuilder.actionButtonLabel(target)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x88101410),
                            border = BorderStroke(1.dp, Color(0x1EFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${BusinessStrategyBanglaBuilder.formatIntBn(idx + 1)}. ${action.text}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("strategy_action_$idx")
                                )
                                StrategyActionButton(
                                    label = btnText,
                                    large = false,
                                    testTag = "strategy_action_btn_$idx",
                                    onClick = { onActionClick(target) }
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
private fun StrategyActionButton(
    label: String,
    large: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = Color(0xFF1E3716),
        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
        modifier = modifier
            .defaultMinSize(minHeight = if (large) 48.dp else 40.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(
                PaddingValues(
                    horizontal = if (large) 16.dp else 10.dp,
                    vertical = if (large) 12.dp else 8.dp
                )
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = VividGreen,
                fontSize = if (large) 15.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VividGreen,
                modifier = Modifier.size(if (large) 17.dp else 14.dp)
            )
        }
    }
}
