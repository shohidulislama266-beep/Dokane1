package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InviteCodeConfig
import com.example.domain.ShopType
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.ShopTypePickerGrid
import com.example.ui.components.VividButton
import com.example.ui.components.extractInviteCodeFromQr
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale

@Composable
fun StoreOnboardingScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by authViewModel.currentLanguage.collectAsState()
    val isBusy by authViewModel.isBusy.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Create Store, 1 = Join Store
    var shopName by rememberSaveable { mutableStateOf("") }
    var shopPhone by rememberSaveable { mutableStateOf("") }
    var shopAddress by rememberSaveable { mutableStateOf("") }
    var selectedShopType by remember { mutableStateOf<ShopType?>(ShopType.GENERAL) }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VividGreen.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.08f),
                        radius = size.width * 0.78f
                    )
                )
            }
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "আপনার দোকান সেটআপ করুন" else "Set Up Your Store",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "নতুন দোকান তৈরি করুন বা স্টাফ কোড দিয়ে যোগ দিন"
                        else "Create your shop profile or join an existing team",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { authViewModel.toggleLanguage() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("onboarding_lang_toggle")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "EN" else "বাংলা",
                                color = VividGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    IconButton(
                        onClick = { authViewModel.signOut(context) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("onboarding_signout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (currentLanguage == AppLanguage.BN) "লগ আউট" else "Sign Out",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Mode Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x20FFFFFF))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to (if (currentLanguage == AppLanguage.BN) "নতুন দোকান" else "Create Store"),
                    1 to (if (currentLanguage == AppLanguage.BN) "কোড দিয়ে যোগ দিন" else "Join with Code")
                ).forEach { (idx, label) ->
                    val active = selectedTab == idx
                    Surface(
                        onClick = {
                            selectedTab = idx
                            authViewModel.clearMessages()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) VividGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("onboarding_tab_$idx")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) DarkBackground else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AlertRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = AlertRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = if (currentLanguage == AppLanguage.BN) "দোকানের নাম *" else "Shop Name *",
                            placeholder = if (currentLanguage == AppLanguage.BN) "যেমন: বিসমিল্লাহ স্টোর" else "e.g. Bismillah General Store",
                            leadingIcon = Icons.Default.Storefront,
                            testTag = "onboarding_shop_name_input"
                        )
                        GlassTextField(
                            value = shopPhone,
                            onValueChange = { shopPhone = it },
                            label = if (currentLanguage == AppLanguage.BN) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)",
                            placeholder = "017XXXXXXXX",
                            leadingIcon = Icons.Default.Phone,
                            testTag = "onboarding_shop_phone_input"
                        )
                        GlassTextField(
                            value = shopAddress,
                            onValueChange = { shopAddress = it },
                            label = if (currentLanguage == AppLanguage.BN) "দোকানের ঠিকানা (ঐচ্ছিক)" else "Shop Address (Optional)",
                            placeholder = if (currentLanguage == AppLanguage.BN) "যেমন: মেইন রোড, ঢাকা" else "e.g. Main Road, Dhaka",
                            leadingIcon = Icons.Default.LocationOn,
                            singleLine = false,
                            testTag = "onboarding_shop_address_input"
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ShopLocale.get("shop_type_picker_title", currentLanguage),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        ShopTypePickerGrid(
                            selectedType = selectedShopType,
                            currentLanguage = currentLanguage,
                            onSelect = { selectedShopType = it },
                            showSelectedPreview = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        VividButton(
                            text = if (currentLanguage == AppLanguage.BN) "দোকান শুরু করুন" else "Create My Store",
                            onClick = {
                                authViewModel.createStore(
                                    name = shopName,
                                    phone = shopPhone,
                                    address = shopAddress,
                                    shopType = selectedShopType
                                )
                            },
                            isLoading = isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "onboarding_create_store_btn"
                        )
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "মালিকের আমন্ত্রণ কোড দিন"
                            else "Enter Store Invite Code",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "দোকানের মালিকের কাছ থেকে পাওয়া ${InviteCodeConfig.CODE_LENGTH} অক্ষরের কোডটি লিখুন বা QR স্ক্যান করুন।"
                            else "Enter the ${InviteCodeConfig.CODE_LENGTH}-character code shared by your store owner or scan the QR.",
                            color = TextSecondary,
                            fontSize = 12.5.sp
                        )

                        GlassTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it.uppercase().take(InviteCodeConfig.CODE_LENGTH) },
                            label = if (currentLanguage == AppLanguage.BN) "আমন্ত্রণ কোড" else "Invite Code",
                            placeholder = InviteCodeConfig.EXAMPLE_CODE,
                            leadingIcon = Icons.Default.QrCode2,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showQrScanner = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = if (currentLanguage == AppLanguage.BN) "QR স্ক্যান করুন" else "Scan QR",
                                        tint = VividGreen
                                    )
                                }
                            },
                            testTag = "onboarding_invite_code_input"
                        )

                        GlassOutlinedButton(
                            text = if (currentLanguage == AppLanguage.BN) "QR কোড স্ক্যান করুন" else "Scan Invite QR",
                            icon = Icons.Default.QrCodeScanner,
                            onClick = { showQrScanner = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "onboarding_scan_qr_btn"
                        )

                        VividButton(
                            text = if (currentLanguage == AppLanguage.BN) "দোকানে যোগ দিন" else "Join Store",
                            onClick = { authViewModel.joinStoreWithInvite(inviteCode) },
                            enabled = inviteCode.trim().length >= InviteCodeConfig.CODE_LENGTH,
                            isLoading = isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "onboarding_join_store_btn"
                        )
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            currentLanguage = currentLanguage,
            onQrScanned = { raw ->
                showQrScanner = false
                inviteCode = extractInviteCodeFromQr(raw)
            },
            onDismiss = { showQrScanner = false }
        )
    }
}
