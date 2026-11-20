package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.ui.components.AccountPickerField
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.SearchGlassField
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale

/**
 * Step 7: Expenses Screen
 * Displays expenses grouped by day, newest first, with search.
 * Total expenses summary card.
 * FAB / Button to record new expense.
 * Tap opens details dialog with soft-delete action.
 */
@Composable
fun ExpensesScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    CashAndBankScreen(
        storeViewModel = storeViewModel,
        initialTab = 1,
        backButtonTag = "expenses_back_btn",
        modifier = modifier
    )
}

@Composable
fun ExpenseRowCard(
    expense: Expense,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB222522), Color(0xEC141614))
                )
            )
            .clickable(onClick = onClick)
            .testTag("expense_card_${expense.id}"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1616))
                    .border(1.5.dp, AlertRed.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = expense.category,
                    tint = AlertRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x26FF5252),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = ShopLocale.displayCategory(expense.category, currentLanguage),
                            color = AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (expense.receiptUri.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Receipt attached",
                            tint = VividGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (expense.note.isNotBlank()) {
                    Text(
                        text = expense.note,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${ShopLocale.formatTimeOnly(expense.date, currentLanguage)} · ${ShopLocale.displayAccountName(expense.accountName, expense.accountId, currentLanguage)}",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "-${ShopLocale.formatMoney(expense.amount, currentLanguage)}",
                color = AlertRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Add Expense Dialog
 * Amount, category, date (default today), account, note, optional receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, date: Long, accId: String, accName: String, note: String, receiptUri: String) -> Unit
) {
    val categories by storeViewModel.categories.collectAsState()
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()

    val expenseCategories = remember(categories) {
        categories.filter { it.type == "expense" && !it.isArchived }
    }

    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(expenseCategories.firstOrNull()?.name ?: "Rent") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val defaultAcc = accounts.find { it.id == lastUsedAccountId }
        ?: accounts.find { it.isDefault || it.id == "cash_in_hand" }
        ?: accounts.firstOrNull()

    var selectedAccountId by remember { mutableStateOf(defaultAcc?.id ?: "cash_in_hand") }
    var selectedAccountName by remember { mutableStateOf(defaultAcc?.name ?: "Cash in Hand") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "নতুন খরচ লিপিবদ্ধ করুন" else "Record New Expense",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                GlassTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        localError = null
                    },
                    label = ShopLocale.get("expense_amount_label", currentLanguage),
                    placeholder = ShopLocale.digits("0.00", currentLanguage),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "input_expense_amount"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    GlassTextField(
                        value = ShopLocale.displayCategory(selectedCategory, currentLanguage),
                        onValueChange = {},
                        readOnly = true,
                        label = ShopLocale.get("category_label", currentLanguage),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.background(DarkBackground)
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(ShopLocale.displayCategory(cat.name, currentLanguage), color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { showDatePickerDialog = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${ShopLocale.get("date_colon", currentLanguage)} ${ShopLocale.formatDateOnly(selectedDateMillis, currentLanguage)}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = ShopLocale.get("change", currentLanguage),
                        color = VividGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Account Picker (hidden if staff lacks Cash & Bank permission)
                if (canCashBank) {
                    AccountPickerField(
                        selectedAccountId = selectedAccountId,
                        selectedAccountName = selectedAccountName,
                        onAccountSelected = { acc ->
                            selectedAccountId = acc.id
                            selectedAccountName = acc.name
                        },
                        storeViewModel = storeViewModel,
                        label = ShopLocale.get("paid_from_account_label", currentLanguage)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = ShopLocale.get("note_desc_label", currentLanguage),
                    placeholder = ShopLocale.get("expense_note_placeholder", currentLanguage),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_expense_note"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Receipt
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (receiptUri.isNotBlank()) VividGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receiptUri.isNotBlank()) ShopLocale.get("receipt_selected", currentLanguage) else ShopLocale.get("attach_receipt_optional", currentLanguage),
                            color = if (receiptUri.isNotBlank()) VividGreen else TextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                    if (receiptUri.isNotBlank()) {
                        IconButton(
                            onClick = { receiptUri = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError ?: "",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        text = ShopLocale.get("save", currentLanguage),
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                localError = ShopLocale.get("err_valid_expense_amt", currentLanguage)
                                return@VividButton
                            }
                            onConfirm(
                                amt,
                                selectedCategory,
                                selectedDateMillis,
                                selectedAccountId,
                                selectedAccountName,
                                note.trim(),
                                receiptUri
                            )
                        },
                        containerColor = AlertRed,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        testTag = "btn_confirm_expense"
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.safeDialogWindowPadding(),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePickerDialog = false
                }) {
                    Text(ShopLocale.get("ok", currentLanguage), color = VividGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(ShopLocale.get("cancel", currentLanguage), color = TextSecondary)
                }
            }
        ) {
            ConfigureEdgeToEdgeDialogWindow()
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Expense Details Dialog
 * Shows full details and receipt. Allows delete with confirmation note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsDialog(
    expense: Expense,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onDelete: (deleteNote: String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteNote by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (!showDeleteConfirm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShopLocale.get("expense_details_title", currentLanguage),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = ShopLocale.get("close", currentLanguage), tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("amount_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = ShopLocale.formatMoney(expense.amount, currentLanguage),
                                color = AlertRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("category_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.displayCategory(expense.category, currentLanguage), color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("account_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.displayAccountName(expense.accountName, expense.accountId, currentLanguage), color = TextPrimary, fontSize = 13.5.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("date_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.formatDateLongFullMonth(expense.date, currentLanguage), color = TextMuted, fontSize = 12.sp)
                        }
                        if (expense.note.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = ShopLocale.get("note_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = expense.note,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (expense.receiptUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = ShopLocale.get("receipt_photo_colon", currentLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = expense.receiptUri,
                            contentDescription = "Receipt photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = ShopLocale.get("delete_entry_btn", currentLanguage),
                            onClick = { showDeleteConfirm = true },
                            icon = Icons.Default.Delete,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_delete_expense"
                        )
                        VividButton(
                            text = ShopLocale.get("done", currentLanguage),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Delete Confirmation Step
                    Text(
                        text = ShopLocale.get("confirm_delete_expense_title", currentLanguage),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ShopLocale.format("confirm_delete_expense_msg", currentLanguage, ShopLocale.formatMoney(expense.amount, currentLanguage)),
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    GlassTextField(
                        value = deleteNote,
                        onValueChange = { deleteNote = it },
                        label = ShopLocale.get("delete_reason_optional", currentLanguage),
                        placeholder = ShopLocale.get("delete_reason_placeholder", currentLanguage),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = ShopLocale.get("back", currentLanguage),
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = ShopLocale.get("delete", currentLanguage),
                            onClick = { onDelete(deleteNote.trim()) },
                            containerColor = AlertRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_confirm_delete_expense"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 8: Other Income Screen
 * Displays other income entries grouped by day, newest first, with search.
 * Summary card of total other income.
 * Add Other Income button & dialog.
 * Details dialog with soft-delete action.
 */
@Composable
fun OtherIncomesScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    CashAndBankScreen(
        storeViewModel = storeViewModel,
        initialTab = 2,
        backButtonTag = "incomes_back_btn",
        modifier = modifier
    )
}

@Composable
fun OtherIncomeRowCard(
    income: OtherIncome,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB222522), Color(0xEC141614))
                )
            )
            .clickable(onClick = onClick)
            .testTag("income_card_${income.id}"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF151915))
                    .border(1.5.dp, VividGreen.copy(alpha = 0.78f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = income.category,
                    tint = VividGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x245DD62C),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = ShopLocale.displayCategory(income.category, currentLanguage),
                        color = VividGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (income.note.isNotBlank()) {
                    Text(
                        text = income.note,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${ShopLocale.formatTimeOnly(income.date, currentLanguage)} · ${ShopLocale.displayAccountName(income.accountName, income.accountId, currentLanguage)}",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "+${ShopLocale.formatMoney(income.amount, currentLanguage)}",
                color = VividGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Add Other Income Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOtherIncomeDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, date: Long, accId: String, accName: String, note: String) -> Unit
) {
    val categories by storeViewModel.categories.collectAsState()
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()

    val incomeCategories = remember(categories) {
        categories.filter { it.type == "income" && !it.isArchived }
    }

    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(incomeCategories.firstOrNull()?.name ?: "Commission") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val defaultAcc = accounts.find { it.id == lastUsedAccountId }
        ?: accounts.find { it.isDefault || it.id == "cash_in_hand" }
        ?: accounts.firstOrNull()

    var selectedAccountId by remember { mutableStateOf(defaultAcc?.id ?: "cash_in_hand") }
    var selectedAccountName by remember { mutableStateOf(defaultAcc?.name ?: "Cash in Hand") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "নতুন আয় লিপিবদ্ধ করুন" else "Record Other Income",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                GlassTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        localError = null
                    },
                    label = ShopLocale.get("income_amount_label", currentLanguage),
                    placeholder = ShopLocale.digits("0.00", currentLanguage),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "input_income_amount"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    GlassTextField(
                        value = ShopLocale.displayCategory(selectedCategory, currentLanguage),
                        onValueChange = {},
                        readOnly = true,
                        label = ShopLocale.get("category_label", currentLanguage),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.background(DarkBackground)
                    ) {
                        incomeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(ShopLocale.displayCategory(cat.name, currentLanguage), color = TextPrimary) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { showDatePickerDialog = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${ShopLocale.get("date_colon", currentLanguage)} ${ShopLocale.formatDateOnly(selectedDateMillis, currentLanguage)}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = ShopLocale.get("change", currentLanguage),
                        color = VividGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Account Picker (hidden if staff lacks Cash & Bank permission)
                if (canCashBank) {
                    AccountPickerField(
                        selectedAccountId = selectedAccountId,
                        selectedAccountName = selectedAccountName,
                        onAccountSelected = { acc ->
                            selectedAccountId = acc.id
                            selectedAccountName = acc.name
                        },
                        storeViewModel = storeViewModel,
                        label = ShopLocale.get("deposited_to_account_label", currentLanguage)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = ShopLocale.get("note_desc_label", currentLanguage),
                    placeholder = ShopLocale.get("income_note_placeholder", currentLanguage),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_income_note"
                )

                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError ?: "",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        text = ShopLocale.get("save", currentLanguage),
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                localError = ShopLocale.get("err_valid_income_amt", currentLanguage)
                                return@VividButton
                            }
                            onConfirm(
                                amt,
                                selectedCategory,
                                selectedDateMillis,
                                selectedAccountId,
                                selectedAccountName,
                                note.trim()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "btn_confirm_income"
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.safeDialogWindowPadding(),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePickerDialog = false
                }) {
                    Text(ShopLocale.get("ok", currentLanguage), color = VividGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(ShopLocale.get("cancel", currentLanguage), color = TextSecondary)
                }
            }
        ) {
            ConfigureEdgeToEdgeDialogWindow()
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Other Income Details Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherIncomeDetailsDialog(
    income: OtherIncome,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onDelete: (deleteNote: String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteNote by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (!showDeleteConfirm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShopLocale.get("income_details_title", currentLanguage),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = ShopLocale.get("close", currentLanguage), tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("amount_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "+${ShopLocale.formatMoney(income.amount, currentLanguage)}",
                                color = VividGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("category_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.displayCategory(income.category, currentLanguage), color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("account_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.displayAccountName(income.accountName, income.accountId, currentLanguage), color = TextPrimary, fontSize = 13.5.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ShopLocale.get("date_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                            Text(text = ShopLocale.formatDateLongFullMonth(income.date, currentLanguage), color = TextMuted, fontSize = 12.sp)
                        }
                        if (income.note.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = ShopLocale.get("note_colon", currentLanguage), color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    text = income.note,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = ShopLocale.get("delete_entry_btn", currentLanguage),
                            onClick = { showDeleteConfirm = true },
                            icon = Icons.Default.Delete,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_delete_income"
                        )
                        VividButton(
                            text = ShopLocale.get("done", currentLanguage),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = ShopLocale.get("confirm_delete_income_title", currentLanguage),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ShopLocale.format("confirm_delete_income_msg", currentLanguage, ShopLocale.formatMoney(income.amount, currentLanguage)),
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    GlassTextField(
                        value = deleteNote,
                        onValueChange = { deleteNote = it },
                        label = ShopLocale.get("delete_reason_optional", currentLanguage),
                        placeholder = ShopLocale.get("delete_reason_placeholder", currentLanguage),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = ShopLocale.get("back", currentLanguage),
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = ShopLocale.get("delete", currentLanguage),
                            onClick = { onDelete(deleteNote.trim()) },
                            containerColor = AlertRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_confirm_delete_income"
                        )
                    }
                }
            }
        }
    }
}
