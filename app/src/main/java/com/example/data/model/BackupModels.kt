package com.example.data.model

import com.example.domain.ShopUnitPreset

data class StoreBackupSettings(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val shopType: String = "",
    val language: String = "en",
    val isSimpleMode: Boolean = false,
    val photoStorageMode: String = "sync"
)

data class StoreBackupUnits(
    val presets: List<ShopUnitPreset> = emptyList(),
    val allUnits: List<String> = emptyList()
)

data class StoreBackupCounters(
    val invoiceSeq: Long = 100L,
    val paymentSeq: Long = 0L,
    val purchaseSeq: Long = 0L,
    val returnSeq: Long = 0L
)

data class StoreBackupPayload(
    val format: String = FORMAT_ID,
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val store: Store = Store(),
    val storeSettings: StoreBackupSettings = StoreBackupSettings(),
    val shopType: String = "",
    val productCategories: List<String> = emptyList(),
    val categoryRecords: List<StoreCategory> = emptyList(),
    val units: StoreBackupUnits = StoreBackupUnits(),
    val counters: StoreBackupCounters = StoreBackupCounters(),
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val purchaseReturns: List<PurchaseReturn> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val supplierPayments: List<SupplierPayment> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val accountAdjustments: List<AccountAdjustment> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val otherIncomes: List<OtherIncome> = emptyList(),
    val notes: List<StoreNote> = emptyList()
) {
    companion object {
        const val FORMAT_ID = "storeos-backup"
        const val CURRENT_VERSION = 1
    }
}

enum class BackupImportMode {
    ADD_TO_EMPTY_STORE,
    MERGE
}

enum class CsvExportTarget(val slug: String, val labelKey: String) {
    PRODUCTS("products", "csv_target_products"),
    CUSTOMERS("customers", "csv_target_customers"),
    SUPPLIERS("suppliers", "csv_target_suppliers"),
    SALES("sales", "csv_target_sales")
}

data class BackupImportPreview(
    val payload: StoreBackupPayload,
    val version: Int,
    val sourceStoreName: String,
    val shopType: String,
    val exportedAtMillis: Long,
    val targetStoreIsEmpty: Boolean,
    val productCategoriesCount: Int,
    val categoryRecordsCount: Int,
    val productsCount: Int,
    val customersCount: Int,
    val suppliersCount: Int,
    val salesCount: Int,
    val purchasesCount: Int,
    val purchaseReturnsCount: Int,
    val paymentsCount: Int,
    val supplierPaymentsCount: Int,
    val accountsCount: Int,
    val accountAdjustmentsCount: Int,
    val expensesCount: Int,
    val incomeCount: Int,
    val notesCount: Int
) {
    val totalRecordsCount: Int
        get() = productsCount +
            customersCount +
            suppliersCount +
            salesCount +
            purchasesCount +
            purchaseReturnsCount +
            paymentsCount +
            supplierPaymentsCount +
            accountsCount +
            accountAdjustmentsCount +
            expensesCount +
            incomeCount +
            notesCount +
            categoryRecordsCount
}

data class BackupImportSummary(
    val mode: BackupImportMode,
    val backupVersion: Int,
    val autoBackupFileName: String,
    val autoBackupPath: String,
    val productsAdded: Int = 0,
    val productsSkipped: Int = 0,
    val customersAdded: Int = 0,
    val customersSkipped: Int = 0,
    val suppliersAdded: Int = 0,
    val suppliersSkipped: Int = 0,
    val salesAdded: Int = 0,
    val salesSkipped: Int = 0,
    val purchasesAdded: Int = 0,
    val purchasesSkipped: Int = 0,
    val purchaseReturnsAdded: Int = 0,
    val purchaseReturnsSkipped: Int = 0,
    val paymentsAdded: Int = 0,
    val paymentsSkipped: Int = 0,
    val supplierPaymentsAdded: Int = 0,
    val supplierPaymentsSkipped: Int = 0,
    val accountsAdded: Int = 0,
    val accountsSkipped: Int = 0,
    val adjustmentsAdded: Int = 0,
    val adjustmentsSkipped: Int = 0,
    val expensesAdded: Int = 0,
    val expensesSkipped: Int = 0,
    val incomeAdded: Int = 0,
    val incomeSkipped: Int = 0,
    val notesAdded: Int = 0,
    val notesSkipped: Int = 0,
    val categoriesAdded: Int = 0,
    val categoriesSkipped: Int = 0,
    val restoredSettings: StoreBackupSettings? = null
) {
    val totalAdded: Int
        get() = productsAdded + customersAdded + suppliersAdded + salesAdded +
            purchasesAdded + purchaseReturnsAdded + paymentsAdded + supplierPaymentsAdded +
            accountsAdded + adjustmentsAdded + expensesAdded + incomeAdded +
            notesAdded + categoriesAdded

    val totalSkipped: Int
        get() = productsSkipped + customersSkipped + suppliersSkipped + salesSkipped +
            purchasesSkipped + purchaseReturnsSkipped + paymentsSkipped + supplierPaymentsSkipped +
            accountsSkipped + adjustmentsSkipped + expensesSkipped + incomeSkipped +
            notesSkipped + categoriesSkipped
}
