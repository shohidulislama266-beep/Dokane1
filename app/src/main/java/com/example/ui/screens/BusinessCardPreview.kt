package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessCardDraft
import com.example.data.model.BusinessCardLayout
import com.example.data.model.BusinessCardPalettes
import com.example.data.model.BusinessCardSide
import com.example.data.model.BusinessCardTextAlign
import com.example.data.model.PaletteColorRoles
import com.example.domain.BusinessCardPaletteEngine
import com.example.util.AppLanguage
import com.example.util.BusinessCardLocalStore
import com.example.util.BusinessCardRenderer
import com.example.util.ShopLocale

/**
 * Live Jetpack Compose preview of the 3.5 x 2 inch Business Card.
 * Mirrors the exact color roles, typography pairs, alignment, layouts, Front/Back switch,
 * cropped background + dark overlay, logo, and faint sample placeholders for blank fields.
 */
@Composable
fun BusinessCardCanvasPreview(
    draft: BusinessCardDraft,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val palette = remember(draft.paletteId) { BusinessCardPalettes.byId(draft.paletteId) }
    val roles = remember(palette, draft.isLightCard) {
        BusinessCardPaletteEngine.resolveRoles(palette, draft.isLightCard)
    }

    val bgBitmap: Bitmap? = remember(draft.backgroundImageUri) {
        BusinessCardLocalStore.decodeLocalBitmap(draft.backgroundImageUri, maxDimensionPx = 1050)
    }
    val logoBitmap: Bitmap? = remember(draft.logoUri) {
        BusinessCardLocalStore.decodeLocalBitmap(draft.logoUri, maxDimensionPx = 400)
    }

    val hasBgPhoto = bgBitmap != null
    val (mainTextArgb, highlightTextArgb) = remember(roles, hasBgPhoto) {
        BusinessCardPaletteEngine.effectiveTextRolesForCanvas(roles, hasBgPhoto)
    }

    val bgColor = Color(roles.backgroundArgb)
    val mainColor = Color(mainTextArgb)
    val highlightColor = Color(highlightTextArgb)
    val accentColor = Color(roles.accentGraphicsArgb)

    val shopText = draft.shopName.trim()
    val ownerText = draft.ownerName.trim()
    val titleText = draft.title.trim()
    val taglineText = draft.tagline.trim()
    val phoneText = draft.phone.trim()
    val emailText = draft.email.trim()
    val addressText = draft.address.trim()

    val sampleShop = ShopLocale.get("bc_sample_shop", currentLanguage)
    val sampleOwner = ShopLocale.get("bc_sample_owner", currentLanguage)
    val sampleTitle = ShopLocale.get("bc_sample_title", currentLanguage)
    val sampleTagline = ShopLocale.get("bc_sample_tagline", currentLanguage)
    val samplePhone = ShopLocale.get("bc_sample_phone", currentLanguage)
    val sampleEmail = ShopLocale.get("bc_sample_email", currentLanguage)
    val sampleAddress = ShopLocale.get("bc_sample_address", currentLanguage)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BusinessCardLocalStore.CARD_ASPECT_RATIO)
            .shadow(12.dp, RoundedCornerShape(14.dp))
            .testTag("bc_card_preview"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            val scaleFactor = (maxWidth.value / 336f).coerceIn(0.75f, 1.45f) * draft.fontSize.scale

            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = draft.backgroundOverlayAlpha.coerceIn(0f, 0.85f)))
                )
            }

            if (draft.side == BusinessCardSide.BACK) {
                CardBackPreviewContent(
                    shopDisplay = shopText.ifBlank { sampleShop },
                    isShopSample = shopText.isBlank(),
                    taglineDisplay = taglineText.ifBlank { sampleTagline },
                    isTaglineSample = taglineText.isBlank(),
                    logoBitmap = logoBitmap,
                    roles = roles,
                    mainColor = mainColor,
                    highlightColor = highlightColor,
                    accentColor = accentColor,
                    draft = draft,
                    scaleFactor = scaleFactor
                )
            } else {
                when (draft.layout) {
                    BusinessCardLayout.LEFT_ALIGNED -> CardFrontLeftAlignedPreview(
                        shopDisplay = shopText.ifBlank { sampleShop },
                        isShopSample = shopText.isBlank(),
                        ownerDisplay = ownerText.ifBlank { sampleOwner },
                        isOwnerSample = ownerText.isBlank(),
                        titleDisplay = titleText.ifBlank { sampleTitle },
                        isTitleSample = titleText.isBlank(),
                        taglineDisplay = taglineText.ifBlank { sampleTagline },
                        isTaglineSample = taglineText.isBlank(),
                        phoneDisplay = phoneText.ifBlank { samplePhone },
                        isPhoneSample = phoneText.isBlank(),
                        emailDisplay = emailText.ifBlank { sampleEmail },
                        isEmailSample = emailText.isBlank(),
                        addressDisplay = addressText.ifBlank { sampleAddress },
                        isAddressSample = addressText.isBlank(),
                        logoBitmap = logoBitmap,
                        mainColor = mainColor,
                        highlightColor = highlightColor,
                        accentColor = accentColor,
                        draft = draft,
                        scaleFactor = scaleFactor
                    )
                    BusinessCardLayout.CENTERED -> CardFrontCenteredPreview(
                        shopDisplay = shopText.ifBlank { sampleShop },
                        isShopSample = shopText.isBlank(),
                        ownerDisplay = ownerText.ifBlank { sampleOwner },
                        isOwnerSample = ownerText.isBlank(),
                        titleDisplay = titleText.ifBlank { sampleTitle },
                        isTitleSample = titleText.isBlank(),
                        taglineDisplay = taglineText.ifBlank { sampleTagline },
                        isTaglineSample = taglineText.isBlank(),
                        phoneDisplay = phoneText.ifBlank { samplePhone },
                        isPhoneSample = phoneText.isBlank(),
                        emailDisplay = emailText.ifBlank { sampleEmail },
                        isEmailSample = emailText.isBlank(),
                        addressDisplay = addressText.ifBlank { sampleAddress },
                        isAddressSample = addressText.isBlank(),
                        logoBitmap = logoBitmap,
                        mainColor = mainColor,
                        highlightColor = highlightColor,
                        accentColor = accentColor,
                        draft = draft,
                        scaleFactor = scaleFactor
                    )
                    BusinessCardLayout.SPLIT_PANEL -> CardFrontSplitPanelPreview(
                        shopDisplay = shopText.ifBlank { sampleShop },
                        isShopSample = shopText.isBlank(),
                        ownerDisplay = ownerText.ifBlank { sampleOwner },
                        isOwnerSample = ownerText.isBlank(),
                        titleDisplay = titleText.ifBlank { sampleTitle },
                        isTitleSample = titleText.isBlank(),
                        taglineDisplay = taglineText.ifBlank { sampleTagline },
                        isTaglineSample = taglineText.isBlank(),
                        phoneDisplay = phoneText.ifBlank { samplePhone },
                        isPhoneSample = phoneText.isBlank(),
                        emailDisplay = emailText.ifBlank { sampleEmail },
                        isEmailSample = emailText.isBlank(),
                        addressDisplay = addressText.ifBlank { sampleAddress },
                        isAddressSample = addressText.isBlank(),
                        logoBitmap = logoBitmap,
                        roles = roles,
                        hasBgPhoto = hasBgPhoto,
                        mainColor = mainColor,
                        highlightColor = highlightColor,
                        accentColor = accentColor,
                        draft = draft,
                        scaleFactor = scaleFactor
                    )
                }
            }
        }
    }
}

@Composable
private fun CardBackPreviewContent(
    shopDisplay: String,
    isShopSample: Boolean,
    taglineDisplay: String,
    isTaglineSample: Boolean,
    logoBitmap: Bitmap?,
    roles: PaletteColorRoles,
    mainColor: Color,
    highlightColor: Color,
    accentColor: Color,
    draft: BusinessCardDraft,
    scaleFactor: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap.asImageBitmap(),
                    contentDescription = shopDisplay,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = shopDisplay.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                    Text(
                        text = BusinessCardRenderer.buildComposeStyledText(initial, draft.fontPair),
                        color = highlightColor.copy(alpha = if (isShopSample) 0.48f else 1f),
                        fontSize = (16f * scaleFactor).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = BusinessCardRenderer.buildComposeStyledText(shopDisplay, draft.fontPair),
                color = highlightColor.copy(alpha = if (isShopSample) 0.44f else 1f),
                fontSize = (16f * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("bc_preview_back_shop")
            )
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(2.dp)
                    .background(Color(roles.accentGraphicsArgb))
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = BusinessCardRenderer.buildComposeStyledText(taglineDisplay, draft.fontPair),
                color = mainColor.copy(alpha = if (isTaglineSample) 0.36f else 0.85f),
                fontSize = (9.5f * scaleFactor).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardFrontLeftAlignedPreview(
    shopDisplay: String,
    isShopSample: Boolean,
    ownerDisplay: String,
    isOwnerSample: Boolean,
    titleDisplay: String,
    isTitleSample: Boolean,
    taglineDisplay: String,
    isTaglineSample: Boolean,
    phoneDisplay: String,
    isPhoneSample: Boolean,
    emailDisplay: String,
    isEmailSample: Boolean,
    addressDisplay: String,
    isAddressSample: Boolean,
    logoBitmap: Bitmap?,
    mainColor: Color,
    highlightColor: Color,
    accentColor: Color,
    draft: BusinessCardDraft,
    scaleFactor: Float
) {
    val resolvedAlign = toComposeTextAlign(draft.textAlign, TextAlign.Start)
    val colAlign = toHorizontalAlignment(resolvedAlign)

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = colAlign
                ) {
                    Text(
                        text = BusinessCardRenderer.buildComposeStyledText(shopDisplay, draft.fontPair),
                        color = highlightColor.copy(alpha = if (isShopSample) 0.42f else 1f),
                        fontSize = (15.5f * scaleFactor).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = resolvedAlign,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().testTag("bc_preview_shop_name")
                    )
                    Text(
                        text = BusinessCardRenderer.buildComposeStyledText(taglineDisplay, draft.fontPair),
                        color = mainColor.copy(alpha = if (isTaglineSample) 0.36f else 0.80f),
                        fontSize = (9f * scaleFactor).sp,
                        textAlign = resolvedAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (logoBitmap != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = shopDisplay,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("bc_preview_logo")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accentColor.copy(alpha = 0.7f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign
            ) {
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(ownerDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isOwnerSample) 0.42f else 1f),
                    fontSize = (12f * scaleFactor).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_owner_name")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(titleDisplay, draft.fontPair),
                    color = highlightColor.copy(alpha = if (isTitleSample) 0.36f else 0.90f),
                    fontSize = (9.2f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign,
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(phoneDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isPhoneSample) 0.38f else 0.92f),
                    fontSize = (9f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_phone")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(emailDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isEmailSample) 0.38f else 0.90f),
                    fontSize = (8.8f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_email")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(addressDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isAddressSample) 0.38f else 0.88f),
                    fontSize = (8.8f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_address")
                )
            }
        }
    }
}

@Composable
private fun CardFrontCenteredPreview(
    shopDisplay: String,
    isShopSample: Boolean,
    ownerDisplay: String,
    isOwnerSample: Boolean,
    titleDisplay: String,
    isTitleSample: Boolean,
    taglineDisplay: String,
    isTaglineSample: Boolean,
    phoneDisplay: String,
    isPhoneSample: Boolean,
    emailDisplay: String,
    isEmailSample: Boolean,
    addressDisplay: String,
    isAddressSample: Boolean,
    logoBitmap: Bitmap?,
    mainColor: Color,
    highlightColor: Color,
    accentColor: Color,
    draft: BusinessCardDraft,
    scaleFactor: Float
) {
    val resolvedAlign = toComposeTextAlign(draft.textAlign, TextAlign.Center)
    val colAlign = toHorizontalAlignment(resolvedAlign)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = colAlign,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = shopDisplay,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("bc_preview_logo")
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(shopDisplay, draft.fontPair),
                    color = highlightColor.copy(alpha = if (isShopSample) 0.42f else 1f),
                    fontSize = (15f * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_shop_name")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(taglineDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isTaglineSample) 0.36f else 0.80f),
                    fontSize = (8.8f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(1.5.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign
            ) {
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(ownerDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isOwnerSample) 0.42f else 1f),
                    fontSize = (11.5f * scaleFactor).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_owner_name")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(titleDisplay, draft.fontPair),
                    color = highlightColor.copy(alpha = if (isTitleSample) 0.36f else 0.90f),
                    fontSize = (9f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val combinedContact = "$phoneDisplay  •  $emailDisplay"
                val anySampleContact = isPhoneSample && isEmailSample
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(combinedContact, draft.fontPair),
                    color = mainColor.copy(alpha = if (anySampleContact) 0.38f else 0.90f),
                    fontSize = (8.5f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(addressDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isAddressSample) 0.38f else 0.86f),
                    fontSize = (8.5f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CardFrontSplitPanelPreview(
    shopDisplay: String,
    isShopSample: Boolean,
    ownerDisplay: String,
    isOwnerSample: Boolean,
    titleDisplay: String,
    isTitleSample: Boolean,
    taglineDisplay: String,
    isTaglineSample: Boolean,
    phoneDisplay: String,
    isPhoneSample: Boolean,
    emailDisplay: String,
    isEmailSample: Boolean,
    addressDisplay: String,
    isAddressSample: Boolean,
    logoBitmap: Bitmap?,
    roles: PaletteColorRoles,
    hasBgPhoto: Boolean,
    mainColor: Color,
    highlightColor: Color,
    accentColor: Color,
    draft: BusinessCardDraft,
    scaleFactor: Float
) {
    val panelBgArgb = if (hasBgPhoto) android.graphics.Color.BLACK else roles.splitPanelArgb
    val panelBg = if (hasBgPhoto) Color.Black.copy(alpha = 0.68f) else Color(roles.splitPanelArgb)
    val panelTextColor = Color(
        BusinessCardPaletteEngine.resolveSplitPanelTextColor(panelBgArgb, roles)
    )

    val resolvedAlign = toComposeTextAlign(draft.textAlign, TextAlign.Start)
    val colAlign = toHorizontalAlignment(resolvedAlign)

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.37f)
                .fillMaxHeight()
                .background(panelBg)
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap.asImageBitmap(),
                    contentDescription = shopDisplay,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("bc_preview_logo")
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = BusinessCardRenderer.buildComposeStyledText(shopDisplay, draft.fontPair),
                color = panelTextColor.copy(alpha = if (isShopSample) 0.45f else 1f),
                fontSize = (12.5f * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("bc_preview_shop_name")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = BusinessCardRenderer.buildComposeStyledText(taglineDisplay, draft.fontPair),
                color = panelTextColor.copy(alpha = if (isTaglineSample) 0.36f else 0.85f),
                fontSize = (8.2f * scaleFactor).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .weight(0.63f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = colAlign
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign
            ) {
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(ownerDisplay, draft.fontPair),
                    color = highlightColor.copy(alpha = if (isOwnerSample) 0.42f else 1f),
                    fontSize = (13f * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = resolvedAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("bc_preview_owner_name")
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(titleDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isTitleSample) 0.36f else 0.86f),
                    fontSize = (9.2f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accentColor.copy(alpha = 0.7f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = colAlign,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(phoneDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isPhoneSample) 0.38f else 0.92f),
                    fontSize = (8.8f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(emailDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isEmailSample) 0.38f else 0.90f),
                    fontSize = (8.6f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = BusinessCardRenderer.buildComposeStyledText(addressDisplay, draft.fontPair),
                    color = mainColor.copy(alpha = if (isAddressSample) 0.38f else 0.88f),
                    fontSize = (8.6f * scaleFactor).sp,
                    textAlign = resolvedAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun toComposeTextAlign(
    textAlign: BusinessCardTextAlign,
    defaultAlign: TextAlign
): TextAlign = when (textAlign) {
    BusinessCardTextAlign.AUTO -> defaultAlign
    BusinessCardTextAlign.LEFT -> TextAlign.Start
    BusinessCardTextAlign.CENTER -> TextAlign.Center
    BusinessCardTextAlign.RIGHT -> TextAlign.End
}

private fun toHorizontalAlignment(textAlign: TextAlign): Alignment.Horizontal = when (textAlign) {
    TextAlign.Center -> Alignment.CenterHorizontally
    TextAlign.End -> Alignment.End
    else -> Alignment.Start
}
