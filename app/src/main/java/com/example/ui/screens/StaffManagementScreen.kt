package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreInvite
import com.example.data.model.StoreMember
import com.example.data.model.StorePermissions
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.NoPermissionScreen
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.QrCodeView
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale

private val AlertAmber = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val members by storeViewModel.members.collectAsState()
    val invites by storeViewModel.invites.collectAsState()

    if (!isOwner) {
        if (isSimpleMode) {
            LaunchedEffect(Unit) { storeViewModel.navigateBackInBusiness() }
            return
        }
        NoPermissionScreen(
            onBack = { storeViewModel.navigateBackInBusiness() },
            modifier = modifier,
            currentLanguage = currentLanguage
        )
        return
    }

    BackHandler { storeViewModel.navigateBackInBusiness() }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var activeQrInvite by remember { mutableStateOf<StoreInvite?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "staff_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("staff_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ShopLocale.get("back", currentLanguage),
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ShopLocale.get("staff_title", currentLanguage),
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                VividButton(
                    text = ShopLocale.get("add_staff_btn", currentLanguage),
                    icon = Icons.Default.Add,
                    onClick = { showAddStaffDialog = true },
                    testTag = "add_staff_btn"
                )
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = ShopLocale.get("staff_hero_title", currentLanguage),
                            color = VividGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ShopLocale.get("staff_hero_desc", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = ShopLocale.get("staff_non_admin_notice", currentLanguage),
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            val activeInvites = invites.filter { it.status == "active" }
            if (activeInvites.isNotEmpty()) {
                item {
                    Text(
                        text = ShopLocale.get("invited_staff_label", currentLanguage),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(activeInvites, key = { it.code }) { inv ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = { activeQrInvite = inv }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(inv.staffName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text("${inv.roleLabel} • ${inv.code}", color = VividGreen, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(inv.code))
                                        Toast.makeText(context, inv.code, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextPrimary)
                                }
                                IconButton(
                                    onClick = { storeViewModel.cancelInvite(inv.code) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Invite", tint = AlertRed)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "দোকানের সদস্য (${ShopLocale.digits(members.size.toString(), currentLanguage)})"
                    else "Team Members (${members.size})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (members.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PlainEmptyState(message = ShopLocale.get("no_staff_yet", currentLanguage))
                    }
                }
            } else {
                items(members, key = { it.userId }) { member ->
                    val isRemoved = member.status.equals("Removed", ignoreCase = true)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            storeViewModel.selectStaffMember(member)
                            storeViewModel.navigateInBusiness(BusinessNavDestination.STAFF_MEMBER_DETAILS_SCREEN)
                        },
                        testTag = "staff_member_card_${member.userId}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName.ifBlank { member.email.ifBlank { ShopLocale.get("staff_member_default", currentLanguage) } },
                                    color = if (isRemoved) TextMuted else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${member.roleLabel} • ${member.permissionLevel}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isRemoved) AlertRed.copy(alpha = 0.16f) else VividGreen.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = member.status,
                                    color = if (isRemoved) AlertRed else VividGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

    if (showAddStaffDialog) {
        AddStaffInviteDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showAddStaffDialog = false },
            onConfirm = { name, role, level, perms ->
                storeViewModel.createStaffInvite(name, role, level, perms) { created ->
                    showAddStaffDialog = false
                    activeQrInvite = created
                }
            }
        )
    }

    if (activeQrInvite != null) {
        val inv = activeQrInvite!!
        BasicAlertDialog(
            onDismissRequest = { activeQrInvite = null },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
        ) {
            ConfigureEdgeToEdgeDialogWindow()
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkBackground,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDialogWindowPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = inv.staffName,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    QrCodeView(content = "STOREOS_INVITE:${inv.code}", sizeDp = 190)
                    Text(
                        text = inv.code,
                        color = VividGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = if (currentLanguage == AppLanguage.BN) "কোড কপি" else "Copy Code",
                            icon = Icons.Default.ContentCopy,
                            onClick = {
                                clipboard.setText(AnnotatedString(inv.code))
                                Toast.makeText(context, inv.code, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = ShopLocale.get("close", currentLanguage),
                            onClick = { activeQrInvite = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffInviteDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (name: String, roleLabel: String, permissionLevel: String, perms: Map<String, Boolean>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("Limited") }
    val roles = listOf("Sales", "Manager", "Stock", "Accountant")
    var selectedRole by remember { mutableStateOf("Sales") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val shape = RoundedCornerShape(24.dp)
        Surface(
            shape = shape,
            color = DarkBackground,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = ShopLocale.get("add_staff_member_title", currentLanguage),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = ShopLocale.get("staff_name_label", currentLanguage),
                    placeholder = ShopLocale.get("staff_name_placeholder", currentLanguage),
                    testTag = "invite_staff_name_input"
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "পদবি" else "Role",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.forEach { role ->
                        val active = selectedRole == role
                        Surface(
                            onClick = { selectedRole = role },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) VividGreen.copy(alpha = 0.2f) else Color(0x1CFFFFFF),
                            border = BorderStroke(1.dp, if (active) VividGreen else GlassBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ShopLocale.displayStaffRole(role, currentLanguage),
                                    color = if (active) VividGreen else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "অ্যাক্সেস স্তর" else "Permission Level",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Limited", "Half", "Full").forEach { lvl ->
                        val active = selectedLevel == lvl
                        Surface(
                            onClick = { selectedLevel = lvl },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) VividGreen.copy(alpha = 0.2f) else Color(0x1CFFFFFF),
                            border = BorderStroke(1.dp, if (active) VividGreen else GlassBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ShopLocale.displayPermissionLevel(lvl, currentLanguage),
                                    color = if (active) VividGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassOutlinedButton(
                        text = ShopLocale.get("cancel", currentLanguage),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    VividButton(
                        text = ShopLocale.get("save_staff_btn", currentLanguage),
                        enabled = name.isNotBlank(),
                        onClick = {
                            onConfirm(
                                name.trim(),
                                selectedRole,
                                selectedLevel,
                                StorePermissions.forLevel(selectedLevel.lowercase())
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_create_invite_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun StaffMemberDetailsScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val member by storeViewModel.selectedStaffMember.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()

    if (!isOwner || member == null) {
        if (isSimpleMode) {
            LaunchedEffect(Unit) { storeViewModel.navigateBackInBusiness() }
            return
        }
        BackHandler { storeViewModel.navigateBackInBusiness() }
        NoPermissionScreen(
            onBack = { storeViewModel.navigateBackInBusiness() },
            modifier = modifier,
            currentLanguage = currentLanguage
        )
        return
    }

    val activeMember = member!!
    BackHandler { storeViewModel.navigateBackInBusiness() }

    val permsMap = remember(activeMember.userId) {
        mutableStateMapOf<String, Boolean>().apply {
            StorePermissions.ALL_KEYS.forEach { key ->
                put(key, activeMember.permissions[key] ?: false)
            }
        }
    }
    var confirmRemoveRestore by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "staff_member_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { storeViewModel.navigateBackInBusiness() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = ShopLocale.get("staff_details_header", currentLanguage),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(activeMember.displayName.ifBlank { activeMember.email }, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${activeMember.roleLabel} • ${activeMember.status}", color = VividGreen, fontSize = 13.sp)
                }
            }
        }

        items(StorePermissions.ALL_KEYS) { key ->
            val checked = permsMap[key] ?: false
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = checked,
                        onCheckedChange = { permsMap[key] = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = VividGreen
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            VividButton(
                text = if (currentLanguage == AppLanguage.BN) "অনুমতি সংরক্ষণ করুন" else "Save Permissions",
                onClick = {
                    storeViewModel.updateMemberPermissions(
                        memberId = activeMember.userId,
                        roleLabel = activeMember.roleLabel,
                        permissionLevel = "Selected",
                        permissions = permsMap.toMap()
                    ) {
                        storeViewModel.navigateBackInBusiness()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (!activeMember.role.equals("owner", ignoreCase = true)) {
            item {
                val isRemoved = activeMember.status.equals("Removed", ignoreCase = true)
                GlassOutlinedButton(
                    text = if (isRemoved) ShopLocale.get("restore_staff_member", currentLanguage)
                    else ShopLocale.get("remove_staff_member", currentLanguage),
                    icon = if (isRemoved) Icons.Default.Restore else Icons.Default.PersonRemove,
                    onClick = { confirmRemoveRestore = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (confirmRemoveRestore) {
        val isRemoved = activeMember.status.equals("Removed", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { confirmRemoveRestore = false },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = true),
            modifier = Modifier.safeDialogWindowPadding(),
            containerColor = DarkBackground,
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = if (isRemoved) ShopLocale.get("confirm_restore_staff_title", currentLanguage)
                    else ShopLocale.get("confirm_remove_staff_title", currentLanguage),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (isRemoved) ShopLocale.format("confirm_restore_staff_msg", currentLanguage, activeMember.displayName)
                        else ShopLocale.format("confirm_remove_staff_msg", currentLanguage, activeMember.displayName),
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoveRestore = false
                        if (isRemoved) {
                            storeViewModel.restoreMember(activeMember.userId, activeMember.displayName) {
                                storeViewModel.navigateBackInBusiness()
                            }
                        } else {
                            storeViewModel.removeMember(activeMember.userId, activeMember.displayName) {
                                storeViewModel.navigateBackInBusiness()
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (isRemoved) ShopLocale.get("restore", currentLanguage) else ShopLocale.get("remove", currentLanguage),
                        color = if (isRemoved) VividGreen else AlertRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveRestore = false }) {
                    Text(ShopLocale.get("cancel", currentLanguage), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun TestChecklistScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    BackHandler { storeViewModel.navigateBackInBusiness() }

    val checklist = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.BN) {
            listOf(
                "নগদ ও বাকি বিক্রি করে Share Receipt-এ PNG ছবি শেয়ার হয় কিনা দেখুন",
                "বড় পণ্যের নাম রশিদে ও ছবিতে পুরো লাইন ভেঙে দেখায় কিনা দেখুন",
                "বাংলা ও ইংরেজি সংখ্যার ধরন ও টাকার যোগফল সঠিক আছে কিনা যাচাই করুন",
                "ডিসকাউন্ট থাকলে তবেই Discount লাইন দেখায় কিনা পরীক্ষা করুন",
                "Save Image বোতামে গ্যালারিতে PNG সেভ এবং Copy Slip-এ টেক্সট কপি যাচাই করুন",
                "Light / Dark রশিদ ছবির থিম সেটিংস পরিবর্তন করে আবার শেয়ার করুন"
            )
        } else {
            listOf(
                "Cash & Baki checkout shares a 2x crisp PNG file via Share Receipt",
                "Long product names wrap cleanly across lines with no ellipsis",
                "Consistent digits (EN/BN) & exact CalculationEngine totals to 2 decimals",
                "Discount line shown only when > 0; Paid/Due shown on Baki sales",
                "Save Image stores PNG to device gallery and Copy Slip copies plain text",
                "Light / Dark receipt image style setting switches slip & PNG theme cleanly"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { storeViewModel.navigateBackInBusiness() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = if (currentLanguage == AppLanguage.BN) "যাচাই তালিকা" else "Test Checklist",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        checklist.forEach { itemText ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = VividGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = itemText,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}
