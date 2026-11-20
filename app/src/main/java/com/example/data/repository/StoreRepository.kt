package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.Account
import com.example.data.model.AccountAdjustment
import com.example.data.model.AiKeyUiSummary
import com.example.data.model.BackupImportMode
import com.example.data.model.BackupImportSummary
import com.example.data.model.BusinessStrategyFacts
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.HealthFixLog
import com.example.data.model.InviteCodeConfig
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.PrivateAiKeyDoc
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.Store
import com.example.data.model.StoreBackupCounters
import com.example.data.model.StoreBackupPayload
import com.example.data.model.StoreBackupSettings
import com.example.data.model.StoreBackupUnits
import com.example.data.model.StoreCategory
import com.example.data.model.StoreInvite
import com.example.data.model.StoreMember
import com.example.data.model.StoreNote
import com.example.data.model.StorePermissions
import com.example.data.model.StrategyActionAdvice
import com.example.data.model.StrategyActionTarget
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiStrategyClient
import com.example.data.remote.StrategyAiService
import com.example.domain.BusinessStrategyBanglaBuilder
import com.example.domain.CalculationEngine
import com.example.domain.HealthProblem
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.util.AppLanguage
import com.example.util.FirestoreErrorHandler
import com.example.util.OperationType
import com.example.util.PhotoStorageMode
import com.example.util.ProductImageUtil
import com.example.util.ShopLocale
import com.example.util.StockPreferences
import com.example.util.StoreBackupCodec
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.io.File
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ProductSaveOutcome(
    val productId: String,
    val photoUri: String,
    val photoFailed: Boolean = false,
    val photoWarning: String? = null
)

data class PhotoConversionResult(
    val convertedCount: Int,
    val skippedCount: Int,
    val failedCount: Int
)

class StoreRepository(
    private val db: FirebaseFirestore?,
    val appContext: Context? = null
) {

    constructor(context: Context) : this(
        db = try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId) else null
        } catch (e: Exception) {
            Log.w("StoreRepository", "Firestore init fallback: ${e.message}")
            null
        },
        appContext = context.applicationContext
    )

    val hasCloudDatabase: Boolean get() = db != null

    private val auth get() = if (db == null) null else try { Firebase.auth } catch (e: Exception) { null }

    val currentUid: String? get() = auth?.currentUser?.uid
    val currentUserEmail: String? get() = auth?.currentUser?.email
    val currentUserDisplayName: String? get() = auth?.currentUser?.displayName

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: "demo_user"
    }

    // =============================================================
    // IN-MEMORY DEMO STORE STATE (seeded from ShopTypeCatalog)
    // =============================================================

    private val initialDemoSeed = ShopTypeCatalog.buildDemoSeed(ShopType.GROCERY)

    private val demoInvoiceCounter = AtomicLong(initialDemoSeed.nextInvoiceSeq)
    private val demoPaymentCounter = AtomicLong(initialDemoSeed.nextPaymentSeq)
    private val demoPurchaseCounter = AtomicLong(initialDemoSeed.nextPurchaseSeq)
    private val demoReturnCounter = AtomicLong(initialDemoSeed.nextReturnSeq)

    private val _demoStore = MutableStateFlow<Store?>(initialDemoSeed.store)
    private val _demoProducts = MutableStateFlow(initialDemoSeed.products)
    private val _demoCustomers = MutableStateFlow(initialDemoSeed.customers)
    private val _demoSales = MutableStateFlow(initialDemoSeed.sales)
    private val _demoPayments = MutableStateFlow(initialDemoSeed.payments)
    private val _demoSuppliers = MutableStateFlow(initialDemoSeed.suppliers)
    private val _demoPurchases = MutableStateFlow(initialDemoSeed.purchases)
    private val _demoPurchaseReturns = MutableStateFlow(initialDemoSeed.purchaseReturns)
    private val _demoHealthFixLogs = MutableStateFlow<List<HealthFixLog>>(emptyList())
    private val _demoAccounts = MutableStateFlow(initialDemoSeed.accounts)
    private val _demoAdjustments = MutableStateFlow<List<AccountAdjustment>>(emptyList())
    private val _demoExpenses = MutableStateFlow(initialDemoSeed.expenses)
    private val _demoOtherIncomes = MutableStateFlow(initialDemoSeed.otherIncomes)
    private val _demoSupplierPayments = MutableStateFlow(initialDemoSeed.supplierPayments)
    private val _demoCategories = MutableStateFlow(initialDemoSeed.categories)
    private val _demoNotes = MutableStateFlow(initialDemoSeed.notes)
    private val _demoMembers = MutableStateFlow(initialDemoSeed.members)
    private val _demoInvites = MutableStateFlow<List<StoreInvite>>(emptyList())
    private val _demoProductBin = MutableStateFlow<List<Product>>(emptyList())
    private val _demoPrivateAiKey = MutableStateFlow<PrivateAiKeyDoc?>(null)
    private val _demoBusinessStrategy = MutableStateFlow<SavedBusinessStrategy?>(null)

    internal fun seedDemoMemberForTest(member: StoreMember) {
        _demoMembers.value = _demoMembers.value.filter { it.userId != member.userId } + member
        val curStore = _demoStore.value
        if (curStore != null && !curStore.memberUids.contains(member.userId)) {
            _demoStore.value = curStore.copy(
                memberUids = curStore.memberUids + member.userId,
                members = curStore.members.filter { it.userId != member.userId } + member
            )
        }
    }

    @Volatile
    var strategyAiService: StrategyAiService = GeminiStrategyClient()

    // -------------------------------------------------------------
    // USER PROFILE & STORE LINKAGE
    // -------------------------------------------------------------

    private val modePrefs by lazy {
        appContext?.getSharedPreferences("user_mode_prefs", Context.MODE_PRIVATE)
    }

    private fun loadPersistedLanguage(): AppLanguage {
        val code = modePrefs?.getString("app_language", AppLanguage.EN.code)
        val lang = if (code == AppLanguage.BN.code) AppLanguage.BN else AppLanguage.EN
        ShopLocale.activeLanguage = lang
        return lang
    }

    private val _currentLanguage = MutableStateFlow(loadPersistedLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        ShopLocale.activeLanguage = language
        _currentLanguage.value = language
        modePrefs?.edit()?.putString("app_language", language.code)?.apply()
    }

    fun toggleLanguage() {
        val next = if (_currentLanguage.value == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
        setLanguage(next)
    }

    private val _localSimpleModes = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    fun getCachedUserSimpleMode(userId: String): Boolean {
        if (userId.isBlank()) return false
        _localSimpleModes.value[userId]?.let { return it }
        return modePrefs?.getBoolean("simple_mode_$userId", false) ?: false
    }

    private fun cacheUserSimpleModeLocally(userId: String, isSimpleMode: Boolean) {
        if (userId.isBlank()) return
        modePrefs?.edit()?.putBoolean("simple_mode_$userId", isSimpleMode)?.apply()
        _localSimpleModes.value = _localSimpleModes.value + (userId to isSimpleMode)
    }

    fun clearDemoLocalPrefs() {
        modePrefs?.edit()?.remove("simple_mode_demo_user")?.apply()
        _localSimpleModes.value = _localSimpleModes.value - "demo_user"
        appContext?.let { StockPreferences.clearDemoPrefs(it) }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val database = db
        if (database == null || userId.isBlank() || userId == "demo_user") {
            return _localSimpleModes.map { map ->
                val mode = map[userId] ?: getCachedUserSimpleMode(userId)
                UserProfile(
                    id = userId,
                    email = if (userId == "demo_user") "demo@storeos.app" else "",
                    storeId = if (userId == "demo_user") "demo_store" else "",
                    isSimpleMode = mode
                )
            }
        }
        val signedInUid = currentUid
        if (signedInUid == null || signedInUid != userId) {
            return _localSimpleModes.map { map ->
                val mode = map[userId] ?: getCachedUserSimpleMode(userId)
                UserProfile(id = userId, isSimpleMode = mode)
            }
        }
        return combine(
            database.collection("users").document(userId).snapshots()
                .map<DocumentSnapshot, DocumentSnapshot?> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, "users/$userId")
                    emit(null)
                },
            _localSimpleModes
        ) { snap, localMap ->
            if (snap != null && snap.exists()) {
                val rawProfile = snap.toObject(UserProfile::class.java)
                if (rawProfile != null) {
                    val serverHasField = snap.contains("isSimpleMode") || snap.contains("simpleMode")
                    if (serverHasField) {
                        val serverMode = snap.getBoolean("isSimpleMode")
                            ?: snap.getBoolean("simpleMode")
                            ?: rawProfile.isSimpleMode
                        val profile = rawProfile.copy(isSimpleMode = serverMode)
                        modePrefs?.edit()?.putBoolean("simple_mode_$userId", profile.isSimpleMode)?.apply()
                        profile
                    } else {
                        val fallbackMode = localMap[userId] ?: getCachedUserSimpleMode(userId)
                        rawProfile.copy(isSimpleMode = fallbackMode)
                    }
                } else null
            } else {
                val localMode = localMap[userId] ?: getCachedUserSimpleMode(userId)
                UserProfile(id = userId, isSimpleMode = localMode)
            }
        }
    }

    suspend fun getUserProfileOnce(userId: String): UserProfile? {
        val database = db
        if (database == null || userId.isBlank() || userId == "demo_user") {
            return UserProfile(
                id = userId,
                email = "demo@storeos.app",
                storeId = "demo_store",
                isSimpleMode = getCachedUserSimpleMode(userId)
            )
        }
        val signedInUid = currentUid
        if (signedInUid == null || signedInUid != userId) {
            return UserProfile(
                id = userId,
                isSimpleMode = getCachedUserSimpleMode(userId)
            )
        }
        val snap = try {
            database.collection("users").document(userId).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, "users/$userId")
            throw e
        }
        if (!snap.exists()) return null
        val rawProfile = snap.toObject(UserProfile::class.java) ?: return null
        val serverHasField = snap.contains("isSimpleMode") || snap.contains("simpleMode")
        return if (serverHasField) {
            val serverMode = snap.getBoolean("isSimpleMode")
                ?: snap.getBoolean("simpleMode")
                ?: rawProfile.isSimpleMode
            val profile = rawProfile.copy(isSimpleMode = serverMode)
            cacheUserSimpleModeLocally(userId, profile.isSimpleMode)
            profile
        } else {
            rawProfile.copy(isSimpleMode = getCachedUserSimpleMode(userId))
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val database = db
        if (database == null || profile.id == "demo_user") {
            val uid = profile.id.ifBlank { "demo_user" }
            cacheUserSimpleModeLocally(uid, profile.isSimpleMode)
            return
        }
        val signedInUid = currentUid
        val uid = signedInUid ?: profile.id.ifBlank { "demo_user" }
        cacheUserSimpleModeLocally(uid, profile.isSimpleMode)
        if (signedInUid == null || uid == "demo_user") return
        val docRef = database.collection("users").document(uid)
        val existingSnap = runCatching { docRef.get().await() }.getOrNull()
        val payload = buildMap<String, Any> {
            put("email", profile.email)
            put("storeId", profile.storeId)
            put("role", profile.role)
            put("isSimpleMode", profile.isSimpleMode)
            if (existingSnap == null || !existingSnap.exists() || !existingSnap.contains("createdAt")) {
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            throw e
        }
    }

    suspend fun updateUserSimpleMode(userId: String, isSimpleMode: Boolean) {
        val database = db
        if (database == null || userId == "demo_user") {
            val targetUid = userId.ifBlank { "demo_user" }
            cacheUserSimpleModeLocally(targetUid, isSimpleMode)
            return
        }
        val signedInUid = currentUid
        val targetUid = userId.ifBlank { signedInUid ?: "demo_user" }
        cacheUserSimpleModeLocally(targetUid, isSimpleMode)
        if (targetUid == "demo_user" || signedInUid == null || signedInUid != targetUid) {
            return
        }
        val docRef = database.collection("users").document(targetUid)
        val payload = mapOf(
            "isSimpleMode" to isSimpleMode,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val writeTask = docRef.set(payload, SetOptions.merge())
            if (isDeviceOnline()) {
                writeTask.await()
            } else {
                withTimeoutOrNull(1_200L) { writeTask.await() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/$targetUid")
            throw e
        }
    }

    // -------------------------------------------------------------
    // STORE SETUP & MANAGEMENT
    // -------------------------------------------------------------

    fun observeStore(storeId: String): Flow<Store?> {
        if (storeId == "demo_store" || db == null) {
            return _demoStore.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(Store::class.java) else null
            }
            .catch { e ->
                if (e is CancellationException) throw e
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, "stores/$storeId")
                emit(null)
            }
    }

    fun configureDemoStore(shopType: ShopType?) {
        val resolvedType = shopType ?: ShopType.GENERAL
        val seed = ShopTypeCatalog.buildDemoSeed(resolvedType)
        demoInvoiceCounter.set(seed.nextInvoiceSeq)
        demoPaymentCounter.set(seed.nextPaymentSeq)
        demoPurchaseCounter.set(seed.nextPurchaseSeq)
        demoReturnCounter.set(seed.nextReturnSeq)
        _demoStore.value = seed.store
        _demoProducts.value = seed.products
        _demoCustomers.value = seed.customers
        _demoSuppliers.value = seed.suppliers
        _demoSales.value = seed.sales
        _demoPurchases.value = seed.purchases
        _demoPurchaseReturns.value = seed.purchaseReturns
        _demoPayments.value = seed.payments
        _demoSupplierPayments.value = seed.supplierPayments
        _demoAccounts.value = seed.accounts
        _demoAdjustments.value = emptyList()
        _demoExpenses.value = seed.expenses
        _demoOtherIncomes.value = seed.otherIncomes
        _demoCategories.value = seed.categories
        _demoNotes.value = seed.notes
        _demoMembers.value = seed.members
        _demoInvites.value = emptyList()
        _demoProductBin.value = emptyList()
        _demoHealthFixLogs.value = emptyList()
        _demoPrivateAiKey.value = null
        _demoBusinessStrategy.value = null
    }

    suspend fun updateStoreShopType(
        storeId: String,
        shopType: ShopType,
        addSuggestedCategories: Boolean,
        existingCategoriesFallback: List<String> = emptyList()
    ) {
        if (storeId == "demo_store" || db == null) {
            val currentStore = _demoStore.value ?: return
            val baseExisting = (currentStore.productCategories + existingCategoriesFallback)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
            val updatedCategories = if (addSuggestedCategories) {
                ShopTypeCatalog.mergeSuggestedCategories(baseExisting, shopType).take(50)
            } else {
                currentStore.productCategories
            }
            _demoStore.value = currentStore.copy(
                shopType = shopType.id,
                productCategories = updatedCategories,
                updatedAt = Timestamp.now()
            )
            return
        }

        val storeRef = db.collection("stores").document(storeId)
        val storeSnap = storeRef.get().await()
        if (!storeSnap.exists()) {
            throw IllegalStateException("Store not found")
        }
        val currentCategories = (storeSnap.get("productCategories") as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
        val baseExisting = (currentCategories + existingCategoriesFallback)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        val updates = buildMap<String, Any> {
            put("shopType", shopType.id)
            if (addSuggestedCategories) {
                put("productCategories", ShopTypeCatalog.mergeSuggestedCategories(baseExisting, shopType).take(50))
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        storeRef.update(updates).await()
    }

    suspend fun createStore(
        name: String,
        phone: String,
        address: String,
        shopType: String = "",
        productCategories: List<String> = emptyList()
    ): String {
        val cleanShopType = shopType.trim().take(40)
        val cleanCategories = productCategories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(50)

        val database = db
            ?: throw IllegalStateException("Store creation is unavailable without a cloud database.")

        val uid = requireUserId()
        val userEmail = auth?.currentUser?.email ?: ""
        val storeRef = database.collection("stores").document()
        val storeId = storeRef.id

        val storePayload = mapOf(
            "name" to name.trim(),
            "phone" to phone.trim(),
            "address" to address.trim(),
            "shopType" to cleanShopType,
            "productCategories" to cleanCategories,
            "ownerId" to uid,
            "memberUids" to listOf(uid),
            "members" to listOf(
                mapOf(
                    "userId" to uid,
                    "role" to "owner",
                    "status" to "active"
                )
            ),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val counterRef = storeRef.collection("meta").document("counters")
        val counterPayload = mapOf("invoiceSeq" to 100L)

        val userRef = database.collection("users").document(uid)
        val existingUserSnap = runCatching { userRef.get().await() }.getOrNull()
        val userPayload = buildMap<String, Any> {
            put("email", userEmail)
            put("storeId", storeId)
            put("role", "owner")
            if (existingUserSnap == null || !existingUserSnap.exists() || !existingUserSnap.contains("createdAt")) {
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }

        val ownerName = auth?.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Owner"
        val ownerMemberRef = storeRef.collection("members").document(uid)
        val ownerMemberPayload = mapOf(
            "userId" to uid,
            "displayName" to ownerName,
            "email" to userEmail,
            "role" to "owner",
            "roleLabel" to "Owner",
            "permissionLevel" to "Full",
            "permissions" to StorePermissions.defaultOwnerPermissions(),
            "status" to "Active",
            "joinedTime" to System.currentTimeMillis(),
            "invitedBy" to "",
            "inviteCode" to "",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            storeRef.set(storePayload).await()
            userRef.set(userPayload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "stores/$storeId")
            throw e
        }
        try {
            ownerMemberRef.set(ownerMemberPayload).await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "stores/$storeId/members/$uid")
            Log.w("StoreRepository", "Owner member init note: ${e.message}")
        }
        try {
            counterRef.set(counterPayload, SetOptions.merge()).await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "stores/$storeId/meta/counters")
            Log.w("StoreRepository", "Counter init note: ${e.message}")
        }

        return storeId
    }

    // -------------------------------------------------------------
    // PRODUCTS & INVENTORY
    // -------------------------------------------------------------

    fun observeProducts(storeId: String): Flow<List<Product>> {
        if (storeId == "demo_store" || db == null) {
            return _demoProducts.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                    .sortedBy { it.name.lowercase() }
            }
    }

    private data class PreparedPhotoResult(
        val photoUri: String,
        val photoFailed: Boolean = false,
        val warning: String? = null
    )

    private fun isDeviceOnline(): Boolean {
        val ctx = appContext ?: return true
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private suspend fun prepareProductPhotoForSave(
        storeId: String,
        productId: String,
        photoUriString: String,
        storageMode: PhotoStorageMode,
        previousPhotoUri: String = "",
        isEdit: Boolean = false
    ): PreparedPhotoResult {
        val trimmed = photoUriString.trim()
        val prevTrimmed = previousPhotoUri.trim()

        if (trimmed.isBlank()) {
            appContext?.let { ProductImageUtil.deleteLocalOnlyPhoto(it, storeId, productId) }
            return PreparedPhotoResult(photoUri = "")
        }

        // Preserve unchanged photo if user did not change it during edit
        if (isEdit && trimmed == prevTrimmed) {
            if (ProductImageUtil.isSyncDataUri(trimmed) ||
                ProductImageUtil.isLocalOnlyFlag(trimmed) ||
                ProductImageUtil.isRemoteUrl(trimmed)
            ) {
                return PreparedPhotoResult(photoUri = trimmed)
            }
        }

        // Already formatted for target mode
        if (storageMode == PhotoStorageMode.SYNC && ProductImageUtil.isSyncDataUri(trimmed)) {
            return PreparedPhotoResult(photoUri = trimmed)
        }
        if (storageMode == PhotoStorageMode.PHONE_ONLY && ProductImageUtil.isLocalOnlyFlag(trimmed)) {
            return PreparedPhotoResult(photoUri = ProductImageUtil.LOCAL_ONLY_FLAG)
        }

        val context = appContext
        if (context == null) {
            val fallback = if (isEdit) prevTrimmed else ""
            val msg = ShopLocale.format(
                "err_photo_read_save_product_saved",
                _currentLanguage.value,
                ShopLocale.get("err_context_required_photo", _currentLanguage.value)
            )
            return PreparedPhotoResult(photoUri = fallback, photoFailed = true, warning = msg)
        }

        return try {
            when (storageMode) {
                PhotoStorageMode.SYNC -> {
                    val compressedBytes = withContext(Dispatchers.IO) {
                        when {
                            ProductImageUtil.isLocalOnlyFlag(trimmed) -> {
                                val localFile = ProductImageUtil.findLocalOnlyPhotoFile(context, storeId, productId)
                                    ?: throw IllegalStateException(ShopLocale.get("err_open_image", _currentLanguage.value))
                                ProductImageUtil.compressUriForSync(context, Uri.fromFile(localFile))
                            }
                            ProductImageUtil.isLocalPhotoUri(trimmed) -> {
                                ProductImageUtil.compressUriForSync(context, Uri.parse(trimmed))
                            }
                            else -> throw IllegalStateException(ShopLocale.get("err_open_image", _currentLanguage.value))
                        }
                    }
                    val dataUri = ProductImageUtil.encodeBytesToDataUri(compressedBytes)
                    ProductImageUtil.deleteLocalOnlyPhoto(context, storeId, productId)
                    if (ProductImageUtil.isLocalPhotoUri(trimmed)) {
                        ProductImageUtil.deleteLocalPhotoIfOwned(context, trimmed)
                    }
                    PreparedPhotoResult(photoUri = dataUri)
                }

                PhotoStorageMode.PHONE_ONLY -> {
                    val compressedBytes = withContext(Dispatchers.IO) {
                        when {
                            ProductImageUtil.isSyncDataUri(trimmed) -> {
                                ProductImageUtil.decodeDataUriBytes(trimmed)
                                    ?: throw IllegalStateException(ShopLocale.get("err_open_image", _currentLanguage.value))
                            }
                            ProductImageUtil.isLocalPhotoUri(trimmed) -> {
                                ProductImageUtil.compressUriForPhoneOnly(context, Uri.parse(trimmed))
                            }
                            else -> throw IllegalStateException(ShopLocale.get("err_open_image", _currentLanguage.value))
                        }
                    }
                    withContext(Dispatchers.IO) {
                        ProductImageUtil.saveLocalOnlyPhotoBytes(context, storeId, productId, compressedBytes)
                    }
                    if (ProductImageUtil.isLocalPhotoUri(trimmed)) {
                        val savedFilePath = ProductImageUtil.localOnlyPhotoFile(context, storeId, productId).absolutePath
                        val srcPath = Uri.parse(trimmed).path
                        if (srcPath != savedFilePath) {
                            ProductImageUtil.deleteLocalPhotoIfOwned(context, trimmed)
                        }
                    }
                    PreparedPhotoResult(photoUri = ProductImageUtil.LOCAL_ONLY_FLAG)
                }
            }
        } catch (e: Exception) {
            Log.w("StoreRepository", "Product photo read/save failed; saving product without new photo", e)
            if (ProductImageUtil.isLocalPhotoUri(trimmed)) {
                ProductImageUtil.deleteLocalPhotoIfOwned(context, trimmed)
            }
            val fallback = if (isEdit) prevTrimmed else ""
            val reason = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: ShopLocale.get("err_open_image", _currentLanguage.value)
            PreparedPhotoResult(
                photoUri = fallback,
                photoFailed = true,
                warning = ShopLocale.format("err_photo_read_save_product_saved", _currentLanguage.value, reason)
            )
        }
    }

    suspend fun convertExistingProductPhotos(
        storeId: String,
        targetMode: PhotoStorageMode,
        currentProducts: List<Product>
    ): PhotoConversionResult {
        val context = appContext ?: return PhotoConversionResult(0, 0, currentProducts.count { it.photoUri.isNotBlank() })
        val isDemo = (storeId == "demo_store" || db == null)
        if (!isDemo) {
            requireUserId()
        }

        var converted = 0
        var skipped = 0
        var failed = 0

        for (product in currentProducts) {
            if (product.id.isBlank()) continue
            val uriStr = product.photoUri.trim()
            if (uriStr.isBlank()) continue

            when (targetMode) {
                PhotoStorageMode.PHONE_ONLY -> {
                    if (ProductImageUtil.isLocalOnlyFlag(uriStr)) {
                        continue
                    }
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            when {
                                ProductImageUtil.isSyncDataUri(uriStr) -> {
                                    ProductImageUtil.decodeDataUriBytes(uriStr)
                                        ?: throw IllegalStateException("Invalid image data")
                                }
                                ProductImageUtil.isLocalPhotoUri(uriStr) -> {
                                    ProductImageUtil.compressUriForPhoneOnly(context, Uri.parse(uriStr))
                                }
                                else -> null
                            }
                        }
                        if (bytes == null) {
                            skipped++
                            continue
                        }
                        withContext(Dispatchers.IO) {
                            ProductImageUtil.saveLocalOnlyPhotoBytes(context, storeId, product.id, bytes)
                        }
                        updateProductPhotoField(storeId, product.id, ProductImageUtil.LOCAL_ONLY_FLAG, isDemo)
                        if (ProductImageUtil.isLocalPhotoUri(uriStr)) {
                            ProductImageUtil.deleteLocalPhotoIfOwned(context, uriStr)
                        }
                        converted++
                    } catch (e: Exception) {
                        Log.w("StoreRepository", "Failed converting synced photo to phone-only for ${product.id}", e)
                        failed++
                    }
                }

                PhotoStorageMode.SYNC -> {
                    if (ProductImageUtil.isSyncDataUri(uriStr)) {
                        continue
                    }
                    try {
                        val compressedBytes = withContext(Dispatchers.IO) {
                            when {
                                ProductImageUtil.isLocalOnlyFlag(uriStr) -> {
                                    val localFile = ProductImageUtil.findLocalOnlyPhotoFile(context, storeId, product.id)
                                        ?: return@withContext null
                                    ProductImageUtil.compressUriForSync(context, Uri.fromFile(localFile))
                                }
                                ProductImageUtil.isLocalPhotoUri(uriStr) -> {
                                    if (!ProductImageUtil.isLocalFilePresent(context, uriStr)) {
                                        return@withContext null
                                    }
                                    ProductImageUtil.compressUriForSync(context, Uri.parse(uriStr))
                                }
                                else -> null
                            }
                        }
                        if (compressedBytes == null) {
                            // Local file lives on another device or is unreadable; never delete/overwrite the flag
                            skipped++
                            continue
                        }
                        val dataUri = ProductImageUtil.encodeBytesToDataUri(compressedBytes)
                        updateProductPhotoField(storeId, product.id, dataUri, isDemo)
                        ProductImageUtil.deleteLocalOnlyPhoto(context, storeId, product.id)
                        if (ProductImageUtil.isLocalPhotoUri(uriStr)) {
                            ProductImageUtil.deleteLocalPhotoIfOwned(context, uriStr)
                        }
                        converted++
                    } catch (e: Exception) {
                        Log.w("StoreRepository", "Failed syncing local photo for ${product.id}", e)
                        failed++
                    }
                }
            }
        }

        return PhotoConversionResult(
            convertedCount = converted,
            skippedCount = skipped,
            failedCount = failed
        )
    }

    private suspend fun updateProductPhotoField(
        storeId: String,
        productId: String,
        newPhotoUri: String,
        isDemo: Boolean
    ) {
        if (isDemo || db == null) {
            _demoProducts.value = _demoProducts.value.map { p ->
                if (p.id == productId) p.copy(photoUri = newPhotoUri, updatedAt = Timestamp.now()) else p
            }
            return
        }
        val docRef = db.collection("stores").document(storeId).collection("products").document(productId)
        val writeTask = docRef.update(
            mapOf(
                "photoUri" to newPhotoUri,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (isDeviceOnline()) {
            writeTask.await()
        } else {
            withTimeoutOrNull(1_200L) { writeTask.await() }
        }
    }

    private fun deleteProductPhotoBestEffort(
        storeId: String,
        productId: String,
        photoUriString: String
    ) {
        val ctx = appContext ?: return
        ProductImageUtil.deleteLocalOnlyPhoto(ctx, storeId, productId)
        val trimmed = photoUriString.trim()
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            ProductImageUtil.deleteLocalPhotoIfOwned(ctx, trimmed)
        }
    }

    suspend fun addProduct(
        storeId: String,
        product: Product,
        photoStorageMode: PhotoStorageMode = PhotoStorageMode.SYNC
    ): ProductSaveOutcome {
        val lang = _currentLanguage.value
        require(product.stock >= 0.0) { ShopLocale.get("err_stock_negative", lang) }
        require(product.buyPrice >= 0.0) { ShopLocale.get("err_cost_negative", lang) }
        require(product.sellPrice >= 0.0) { ShopLocale.get("err_sale_price_negative", lang) }

        val stockUnit = product.unit.trim().ifEmpty { "pcs" }
        val cleanBuyUnit = product.buyUnit.trim().ifEmpty { stockUnit }
        val cleanBuyConv = CalculationEngine.effectiveBuyConversion(
            product.copy(unit = stockUnit, buyUnit = cleanBuyUnit)
        )
        val cleanSellUnits = product.sellUnits.filter { (k, v) ->
            k.isNotBlank() && !k.trim().equals(stockUnit, ignoreCase = true) && v > 0.0
        }.mapKeys { it.key.trim() }

        if (storeId == "demo_store" || db == null) {
            val newId = "prod_${System.currentTimeMillis()}"
            val preparedPhoto = prepareProductPhotoForSave(
                storeId = storeId,
                productId = newId,
                photoUriString = product.photoUri,
                storageMode = photoStorageMode
            )
            val newProd = product.copy(
                id = newId,
                storeId = storeId,
                unit = stockUnit,
                buyUnit = cleanBuyUnit,
                buyConversion = cleanBuyConv,
                sellUnits = cleanSellUnits,
                stock = CalculationEngine.roundStock(product.stock),
                lowStockLimit = CalculationEngine.roundStock(product.lowStockLimit),
                photoUri = preparedPhoto.photoUri,
                createdAt = Timestamp.now()
            )
            _demoProducts.value = (_demoProducts.value + newProd).sortedBy { it.name.lowercase() }
            if (product.photoUri != preparedPhoto.photoUri && product.photoUri.contains("/product_photos/cam_")) {
                appContext?.let { ProductImageUtil.deleteLocalPhotoIfOwned(it, product.photoUri) }
            }
            return ProductSaveOutcome(
                productId = newId,
                photoUri = preparedPhoto.photoUri,
                photoFailed = preparedPhoto.photoFailed,
                photoWarning = preparedPhoto.warning
            )
        }

        val uid = requireUserId()
        val prodRef = db.collection("stores").document(storeId).collection("products").document()
        val preparedPhoto = prepareProductPhotoForSave(
            storeId = storeId,
            productId = prodRef.id,
            photoUriString = product.photoUri,
            storageMode = photoStorageMode
        )
        val payload = mapOf(
            "storeId" to storeId,
            "name" to product.name.trim(),
            "nameBn" to product.nameBn.trim(),
            "barcode" to product.barcode.trim(),
            "category" to product.category.trim().ifEmpty { "General" },
            "unit" to stockUnit,
            "buyUnit" to cleanBuyUnit,
            "buyConversion" to cleanBuyConv,
            "sellUnits" to cleanSellUnits,
            "buyPrice" to product.buyPrice,
            "sellPrice" to product.sellPrice,
            "wholesalePrice" to product.wholesalePrice,
            "mrp" to product.mrp,
            "itemLocation" to product.itemLocation.trim(),
            "partyRate" to product.partyRate,
            "stock" to CalculationEngine.roundStock(product.stock),
            "lowStockLimit" to CalculationEngine.roundStock(product.lowStockLimit),
            "photoUri" to preparedPhoto.photoUri,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val writeTask = prodRef.set(payload)
        if (isDeviceOnline()) {
            writeTask.await()
        } else {
            // Firestore caches offline writes immediately; don't block the UI waiting for server ACK.
            withTimeoutOrNull(1_200L) { writeTask.await() }
        }
        if (product.photoUri != preparedPhoto.photoUri && product.photoUri.contains("/product_photos/cam_")) {
            appContext?.let { ProductImageUtil.deleteLocalPhotoIfOwned(it, product.photoUri) }
        }
        return ProductSaveOutcome(
            productId = prodRef.id,
            photoUri = preparedPhoto.photoUri,
            photoFailed = preparedPhoto.photoFailed,
            photoWarning = preparedPhoto.warning
        )
    }

    suspend fun updateProduct(
        storeId: String,
        product: Product,
        previousPhotoUri: String = "",
        photoStorageMode: PhotoStorageMode = PhotoStorageMode.SYNC
    ): ProductSaveOutcome {
        val lang = _currentLanguage.value
        require(product.id.isNotBlank()) { ShopLocale.get("err_product_id_blank", lang) }
        require(product.stock >= 0.0) { ShopLocale.get("err_stock_negative", lang) }
        require(product.buyPrice >= 0.0) { ShopLocale.get("err_cost_negative", lang) }
        require(product.sellPrice >= 0.0) { ShopLocale.get("err_sale_price_negative", lang) }

        val stockUnit = product.unit.trim().ifEmpty { "pcs" }
        val cleanBuyUnit = product.buyUnit.trim().ifEmpty { stockUnit }
        val cleanBuyConv = CalculationEngine.effectiveBuyConversion(
            product.copy(unit = stockUnit, buyUnit = cleanBuyUnit)
        )
        val cleanSellUnits = product.sellUnits.filter { (k, v) ->
            k.isNotBlank() && !k.trim().equals(stockUnit, ignoreCase = true) && v > 0.0
        }.mapKeys { it.key.trim() }

        if (storeId == "demo_store" || db == null) {
            val preparedPhoto = prepareProductPhotoForSave(
                storeId = storeId,
                productId = product.id,
                photoUriString = product.photoUri,
                storageMode = photoStorageMode,
                previousPhotoUri = previousPhotoUri,
                isEdit = true
            )
            _demoProducts.value = _demoProducts.value.map {
                if (it.id == product.id) {
                    product.copy(
                        unit = stockUnit,
                        buyUnit = cleanBuyUnit,
                        buyConversion = cleanBuyConv,
                        sellUnits = cleanSellUnits,
                        stock = CalculationEngine.roundStock(product.stock),
                        lowStockLimit = CalculationEngine.roundStock(product.lowStockLimit),
                        photoUri = preparedPhoto.photoUri,
                        updatedAt = Timestamp.now()
                    )
                } else it
            }
            if (product.photoUri != preparedPhoto.photoUri && product.photoUri.contains("/product_photos/cam_")) {
                appContext?.let { ProductImageUtil.deleteLocalPhotoIfOwned(it, product.photoUri) }
            }
            return ProductSaveOutcome(
                productId = product.id,
                photoUri = preparedPhoto.photoUri,
                photoFailed = preparedPhoto.photoFailed,
                photoWarning = preparedPhoto.warning
            )
        }

        requireUserId()
        val preparedPhoto = prepareProductPhotoForSave(
            storeId = storeId,
            productId = product.id,
            photoUriString = product.photoUri,
            storageMode = photoStorageMode,
            previousPhotoUri = previousPhotoUri,
            isEdit = true
        )
        val docRef = db.collection("stores").document(storeId).collection("products").document(product.id)
        val payload = mapOf(
            "name" to product.name.trim(),
            "nameBn" to product.nameBn.trim(),
            "barcode" to product.barcode.trim(),
            "category" to product.category.trim().ifEmpty { "General" },
            "unit" to stockUnit,
            "buyUnit" to cleanBuyUnit,
            "buyConversion" to cleanBuyConv,
            "sellUnits" to cleanSellUnits,
            "buyPrice" to product.buyPrice,
            "sellPrice" to product.sellPrice,
            "wholesalePrice" to product.wholesalePrice,
            "mrp" to product.mrp,
            "itemLocation" to product.itemLocation.trim(),
            "partyRate" to product.partyRate,
            "stock" to CalculationEngine.roundStock(product.stock),
            "lowStockLimit" to CalculationEngine.roundStock(product.lowStockLimit),
            "photoUri" to preparedPhoto.photoUri,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val writeTask = docRef.update(payload)
        if (isDeviceOnline()) {
            writeTask.await()
        } else {
            withTimeoutOrNull(1_200L) { writeTask.await() }
        }
        if (product.photoUri != preparedPhoto.photoUri && product.photoUri.contains("/product_photos/cam_")) {
            appContext?.let { ProductImageUtil.deleteLocalPhotoIfOwned(it, product.photoUri) }
        }
        return ProductSaveOutcome(
            productId = product.id,
            photoUri = preparedPhoto.photoUri,
            photoFailed = preparedPhoto.photoFailed,
            photoWarning = preparedPhoto.warning
        )
    }

    suspend fun deleteProduct(storeId: String, productId: String, photoUri: String = "") {
        require(productId.isNotBlank()) { ShopLocale.get("err_product_id_blank", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            _demoProducts.value = _demoProducts.value.filter { it.id != productId }
            deleteProductPhotoBestEffort(storeId, productId, photoUri)
            return
        }

        requireUserId()
        db.collection("stores").document(storeId).collection("products").document(productId)
            .delete().await()
        deleteProductPhotoBestEffort(storeId, productId, photoUri)
    }

    fun observeProductBin(storeId: String): Flow<List<Product>> {
        if (storeId == "demo_store" || db == null) {
            return _demoProductBin.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("productBin")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                    .sortedBy { it.name.lowercase() }
            }
    }

    suspend fun moveProductToBin(storeId: String, product: Product, deletedByUid: String) {
        require(product.id.isNotBlank()) { ShopLocale.get("err_product_id_blank", _currentLanguage.value) }
        if (storeId == "demo_store" || db == null) {
            _demoProducts.value = _demoProducts.value.filter { it.id != product.id }
            _demoProductBin.value = (_demoProductBin.value.filter { it.id != product.id } + product.copy(updatedAt = Timestamp.now()))
                .sortedBy { it.name.lowercase() }
            return
        }
        val uid = requireUserId().ifBlank { deletedByUid }
        val storeRef = db.collection("stores").document(storeId)
        val prodRef = storeRef.collection("products").document(product.id)
        val binRef = storeRef.collection("productBin").document(product.id)
        val payload = hashMapOf<String, Any>(
            "storeId" to storeId,
            "name" to product.name.trim().ifBlank { "Item" },
            "nameBn" to product.nameBn.trim(),
            "barcode" to product.barcode.trim(),
            "category" to product.category.trim().ifEmpty { "General" },
            "unit" to product.unit.trim().ifEmpty { "pcs" },
            "buyUnit" to product.buyUnit.trim().ifEmpty { product.unit.trim().ifEmpty { "pcs" } },
            "buyConversion" to if (product.buyConversion > 0.0) product.buyConversion else 1.0,
            "sellUnits" to product.sellUnits,
            "buyPrice" to product.buyPrice.coerceAtLeast(0.0),
            "sellPrice" to product.sellPrice.coerceAtLeast(0.0),
            "wholesalePrice" to product.wholesalePrice.coerceAtLeast(0.0),
            "mrp" to product.mrp.coerceAtLeast(0.0),
            "itemLocation" to product.itemLocation.trim(),
            "partyRate" to product.partyRate.coerceAtLeast(0.0),
            "stock" to CalculationEngine.roundStock(product.stock.coerceAtLeast(0.0)),
            "lowStockLimit" to CalculationEngine.roundStock(product.lowStockLimit.coerceAtLeast(0.0)),
            "photoUri" to product.photoUri,
            "createdBy" to product.createdBy.ifBlank { uid },
            "deletedBy" to uid,
            "deletedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(binRef, payload)
        batch.delete(prodRef)
        batch.commit().await()
    }

    suspend fun restoreProductFromBin(storeId: String, productId: String) {
        require(productId.isNotBlank()) { ShopLocale.get("err_product_id_blank", _currentLanguage.value) }
        if (storeId == "demo_store" || db == null) {
            val found = _demoProductBin.value.find { it.id == productId } ?: return
            _demoProductBin.value = _demoProductBin.value.filter { it.id != productId }
            _demoProducts.value = (_demoProducts.value.filter { it.id != productId } + found.copy(updatedAt = Timestamp.now()))
                .sortedBy { it.name.lowercase() }
            return
        }
        val uid = requireUserId()
        val storeRef = db.collection("stores").document(storeId)
        val binRef = storeRef.collection("productBin").document(productId)
        val prodRef = storeRef.collection("products").document(productId)
        val binSnap = binRef.get().await()
        val product = binSnap.toObject(Product::class.java)
            ?: throw IllegalStateException("Product not found in Bin")
        val payload = hashMapOf<String, Any>(
            "storeId" to storeId,
            "name" to product.name.trim().ifBlank { "Item" },
            "nameBn" to product.nameBn.trim(),
            "barcode" to product.barcode.trim(),
            "category" to product.category.trim().ifEmpty { "General" },
            "unit" to product.unit.trim().ifEmpty { "pcs" },
            "buyUnit" to product.buyUnit.trim().ifEmpty { product.unit.trim().ifEmpty { "pcs" } },
            "buyConversion" to if (product.buyConversion > 0.0) product.buyConversion else 1.0,
            "sellUnits" to product.sellUnits,
            "buyPrice" to product.buyPrice.coerceAtLeast(0.0),
            "sellPrice" to product.sellPrice.coerceAtLeast(0.0),
            "wholesalePrice" to product.wholesalePrice.coerceAtLeast(0.0),
            "mrp" to product.mrp.coerceAtLeast(0.0),
            "itemLocation" to product.itemLocation.trim(),
            "partyRate" to product.partyRate.coerceAtLeast(0.0),
            "stock" to CalculationEngine.roundStock(product.stock.coerceAtLeast(0.0)),
            "lowStockLimit" to CalculationEngine.roundStock(product.lowStockLimit.coerceAtLeast(0.0)),
            "photoUri" to product.photoUri,
            "createdBy" to product.createdBy.ifBlank { uid },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(prodRef, payload)
        batch.delete(binRef)
        batch.commit().await()
    }

    suspend fun emptyProductBin(storeId: String) {
        if (storeId == "demo_store" || db == null) {
            _demoProductBin.value.forEach { deleteProductPhotoBestEffort(storeId, it.id, it.photoUri) }
            _demoProductBin.value = emptyList()
            return
        }
        requireUserId()
        val binCol = db.collection("stores").document(storeId).collection("productBin")
        val snaps = binCol.get().await()
        if (snaps.isEmpty) return
        snaps.documents.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc ->
                val photo = doc.getString("photoUri") ?: ""
                deleteProductPhotoBestEffort(storeId, doc.id, photo)
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    suspend fun addStock(storeId: String, productId: String, additionalStock: Double) {
        require(additionalStock > 0.0) { ShopLocale.get("err_add_stock_positive", _currentLanguage.value) }
        val cleanDelta = CalculationEngine.roundStock(additionalStock)

        if (storeId == "demo_store" || db == null) {
            _demoProducts.value = _demoProducts.value.map {
                if (it.id == productId) it.copy(stock = CalculationEngine.roundStock(it.stock + cleanDelta)) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("products").document(productId)
        docRef.update(
            "stock", FieldValue.increment(cleanDelta),
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun updateProductStock(storeId: String, productId: String, newStock: Double) {
        require(newStock >= 0.0) { ShopLocale.get("err_stock_below_zero", _currentLanguage.value) }
        val cleanStock = CalculationEngine.roundStock(newStock)

        if (storeId == "demo_store" || db == null) {
            _demoProducts.value = _demoProducts.value.map {
                if (it.id == productId) it.copy(stock = cleanStock) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("products").document(productId)
        docRef.update(
            "stock", cleanStock,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    // -------------------------------------------------------------
    // SALES & CHECKOUT
    // -------------------------------------------------------------

    fun observeSales(storeId: String): Flow<List<Sale>> {
        if (storeId == "demo_store" || db == null) {
            return _demoSales.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("sales")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Sale::class.java) }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun executeSaleCheckout(
        storeId: String,
        items: List<SaleItem>,
        invoiceDiscount: Double = 0.0,
        paymentType: String = "cash",
        customerId: String = "",
        customerName: String = "Cash Sale",
        customerPhone: String = "",
        notes: String = "",
        accountId: String = "cash_in_hand",
        accountName: String = "Cash in Hand",
        paidNowAmount: Double? = null,
        clientSaleId: String = ""
    ): String {
        val lang = _currentLanguage.value
        require(items.isNotEmpty()) { ShopLocale.get("cart_empty_error", lang) }
        val safeInvoiceDiscount = CalculationEngine.roundMoney(invoiceDiscount.coerceAtLeast(0.0))
        if (CalculationEngine.isSaleDiscountOverProfit(items, safeInvoiceDiscount)) {
            val maxProfitDiscount = CalculationEngine.calculateMaxInvoiceDiscount(items)
            throw IllegalArgumentException(
                if (lang == AppLanguage.BN) {
                    "লাভের (${ShopLocale.formatMoney(maxProfitDiscount, AppLanguage.BN)}) চেয়ে বেশি ছাড় দেওয়া যাবে না"
                } else {
                    "Cannot give more discount than profit (${ShopLocale.formatMoney(maxProfitDiscount, AppLanguage.EN)})"
                }
            )
        }
        val safeClientSaleId = clientSaleId.trim()

        val stockRequestedByProduct = items
            .groupBy { it.productId }
            .mapValues { (_, list) -> CalculationEngine.roundStock(list.sumOf { CalculationEngine.saleItemStockQty(it) }) }

        if (storeId == "demo_store" || db == null) {
            if (safeClientSaleId.isNotBlank()) {
                val existing = _demoSales.value.firstOrNull { it.id == safeClientSaleId }
                if (existing != null) {
                    return existing.invoiceNo
                }
            }

            // Validate ALL stock deductions before mutating any state (atomic rollback on error)
            val currentProds = _demoProducts.value.toMutableList()
            for ((prodId, requestedStock) in stockRequestedByProduct) {
                val idx = currentProds.indexOfFirst { it.id == prodId }
                if (idx != -1) {
                    val p = currentProds[idx]
                    if (!CalculationEngine.isStockSufficient(p.stock, requestedStock)) {
                        val unitDisp = ShopLocale.displayUnit(p.unit, lang)
                        throw IllegalStateException(
                            ShopLocale.format("err_concurrent_stock", lang, p.stock, unitDisp, p.name)
                        )
                    }
                    val remaining = CalculationEngine.validateStockDeduction(p.stock, requestedStock)
                    currentProds[idx] = p.copy(stock = remaining)
                }
            }

            val totalItems = CalculationEngine.roundStock(items.sumOf { CalculationEngine.saleItemStockQty(it) })
            val subtotal = CalculationEngine.roundMoney(items.sumOf { CalculationEngine.calculateLineGrossTotal(it.sellPrice, it.qty) })
            val itemDiscounts = CalculationEngine.roundMoney(items.sumOf { CalculationEngine.calculateBoundedDiscount(it.sellPrice, it.qty, it.discount) })
            val totalDiscount = CalculationEngine.roundMoney((itemDiscounts + safeInvoiceDiscount).coerceIn(0.0, subtotal))
            val grandTotal = CalculationEngine.roundMoney((subtotal - totalDiscount).coerceAtLeast(0.0))
            val totalProfit = CalculationEngine.roundMoney(items.sumOf { CalculationEngine.calculateLineProfit(it.sellPrice, it.buyPrice, it.qty, it.discount) } - safeInvoiceDiscount)

            val effectivePaid: Double
            val effectiveDue: Double
            val effectivePaymentType: String
            if (paidNowAmount != null) {
                effectivePaid = CalculationEngine.roundMoney(paidNowAmount.coerceIn(0.0, grandTotal))
                effectiveDue = CalculationEngine.roundMoney((grandTotal - effectivePaid).coerceAtLeast(0.0))
                effectivePaymentType = if (effectiveDue > 0.001) "baki" else "cash"
            } else {
                val isBaki = paymentType.equals("baki", ignoreCase = true)
                effectivePaid = if (isBaki) 0.0 else grandTotal
                effectiveDue = if (isBaki) grandTotal else 0.0
                effectivePaymentType = if (isBaki) "baki" else "cash"
            }

            if (effectiveDue > 0.001 && customerId.isBlank()) {
                throw IllegalArgumentException(
                    if (lang == AppLanguage.BN) "বাকি রাখতে ক্রেতা নির্বাচন করুন" else "Customer required for unpaid balance"
                )
            }

            val seq = demoInvoiceCounter.incrementAndGet()
            val formattedInvoiceNo = CalculationEngine.formatInvoiceNumber(seq)

            val normalizedItems = items.map {
                val stockQ = CalculationEngine.saleItemStockQty(it)
                val boundD = CalculationEngine.calculateBoundedDiscount(it.sellPrice, it.qty, it.discount)
                val netT = CalculationEngine.calculateLineNetTotal(it.sellPrice, it.qty, boundD)
                val prof = CalculationEngine.calculateLineProfit(it.sellPrice, it.buyPrice, it.qty, boundD)
                it.copy(stockQty = stockQ, discount = boundD, lineTotal = netT, profit = prof)
            }

            val newSale = Sale(
                id = safeClientSaleId.ifBlank { "sale_${System.currentTimeMillis()}" },
                storeId = storeId,
                invoiceNo = formattedInvoiceNo,
                items = normalizedItems,
                totalItems = totalItems,
                subtotal = subtotal,
                discount = totalDiscount,
                grandTotal = grandTotal,
                paidAmount = effectivePaid,
                dueAmount = effectiveDue,
                profit = totalProfit,
                paymentType = effectivePaymentType,
                customerId = customerId.trim(),
                customerName = customerName.trim().ifEmpty { "Cash Sale" },
                customerPhone = customerPhone.trim(),
                accountId = if (effectivePaid > 0.001) accountId else "",
                accountName = if (effectivePaid > 0.001) accountName else "",
                notes = notes.trim(),
                soldBy = "demo_user",
                createdAt = Timestamp.now()
            )

            // Commit all demo state updates together after validation succeeds
            _demoProducts.value = currentProds
            if (effectiveDue > 0.001 && customerId.isNotBlank()) {
                _demoCustomers.value = _demoCustomers.value.map { cust ->
                    if (cust.id == customerId) cust.copy(currentBalance = CalculationEngine.roundMoney(cust.currentBalance + effectiveDue)) else cust
                }
            }
            _demoSales.value = listOf(newSale) + _demoSales.value
            return formattedInvoiceNo
        }

        val database = db ?: throw IllegalStateException(ShopLocale.get("err_firestore_unavailable", lang))
        val uid = requireUserId()
        val storeRef = database.collection("stores").document(storeId)
        val counterRef = storeRef.collection("meta").document("counters")
        val saleRef = if (safeClientSaleId.isNotBlank()) {
            storeRef.collection("sales").document(safeClientSaleId)
        } else {
            storeRef.collection("sales").document()
        }
        val customerRef = if (customerId.isNotBlank()) storeRef.collection("customers").document(customerId) else null

        return database.runTransaction<String> { transaction ->
            // Idempotency check: if this sale attempt already committed, return its invoiceNo without duplicating
            if (safeClientSaleId.isNotBlank()) {
                val existingSaleSnap = transaction.get(saleRef)
                if (existingSaleSnap.exists()) {
                    return@runTransaction existingSaleSnap.getString("invoiceNo").orEmpty()
                }
            }

            val counterSnap = transaction.get(counterRef)
            val currentSeq = (counterSnap.getLong("invoiceSeq") ?: 100L) + 1L

            val customerSnap = customerRef?.let { transaction.get(it) }
            val currentCustomerBalance = customerSnap?.getDouble("currentBalance") ?: 0.0

            val uniqueProductIds = stockRequestedByProduct.keys.filter { it.isNotBlank() }
            val prodSnapMap = mutableMapOf<String, Pair<com.google.firebase.firestore.DocumentReference, DocumentSnapshot>>()
            for (prodId in uniqueProductIds) {
                val prodRef = storeRef.collection("products").document(prodId)
                prodSnapMap[prodId] = prodRef to transaction.get(prodRef)
            }

            val deductions = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Double>>()
            for ((prodId, requestedStock) in stockRequestedByProduct) {
                val pair = prodSnapMap[prodId] ?: continue
                val (prodRef, snap) = pair
                val liveStock = snap.getDouble("stock") ?: (snap.getLong("stock")?.toDouble() ?: 0.0)
                val prodName = snap.getString("name") ?: items.firstOrNull { it.productId == prodId }?.name ?: ""
                val stockUnit = snap.getString("unit") ?: items.firstOrNull { it.productId == prodId }?.unit ?: "pcs"
                if (!CalculationEngine.isStockSufficient(liveStock, requestedStock)) {
                    val unitDisp = ShopLocale.displayUnit(stockUnit, lang)
                    throw IllegalStateException(ShopLocale.format("err_concurrent_stock", lang, liveStock, unitDisp, prodName))
                }
                val remainingStock = CalculationEngine.validateStockDeduction(liveStock, requestedStock)
                deductions.add(prodRef to remainingStock)
            }

            val formattedInvoiceNo = CalculationEngine.formatInvoiceNumber(currentSeq)
            var totalItems = 0.0
            var subtotal = 0.0
            var itemDiscounts = 0.0
            var totalProfit = 0.0
            val itemMaps = mutableListOf<Map<String, Any>>()

            for (item in items) {
                val stockQ = CalculationEngine.saleItemStockQty(item)
                totalItems += stockQ
                val gross = CalculationEngine.calculateLineGrossTotal(item.sellPrice, item.qty)
                val boundDiscount = CalculationEngine.calculateBoundedDiscount(item.sellPrice, item.qty, item.discount)
                val netTotal = CalculationEngine.calculateLineNetTotal(item.sellPrice, item.qty, boundDiscount)
                val lineProfit = CalculationEngine.calculateLineProfit(item.sellPrice, item.buyPrice, item.qty, boundDiscount)

                subtotal += gross
                itemDiscounts += boundDiscount
                totalProfit += lineProfit

                itemMaps.add(
                    mapOf(
                        "productId" to item.productId,
                        "name" to item.name,
                        "qty" to item.qty,
                        "unit" to item.unit,
                        "stockQty" to stockQ,
                        "buyPrice" to item.buyPrice,
                        "sellPrice" to item.sellPrice,
                        "discount" to boundDiscount,
                        "lineTotal" to netTotal,
                        "profit" to lineProfit
                    )
                )
            }

            val totalDiscount = CalculationEngine.roundMoney((itemDiscounts + safeInvoiceDiscount).coerceIn(0.0, subtotal))
            val grandTotal = CalculationEngine.roundMoney((subtotal - totalDiscount).coerceAtLeast(0.0))
            val netProfit = CalculationEngine.roundMoney(totalProfit - safeInvoiceDiscount)

            val effectivePaid: Double
            val effectiveDue: Double
            val effectivePaymentType: String
            if (paidNowAmount != null) {
                effectivePaid = CalculationEngine.roundMoney(paidNowAmount.coerceIn(0.0, grandTotal))
                effectiveDue = CalculationEngine.roundMoney((grandTotal - effectivePaid).coerceAtLeast(0.0))
                effectivePaymentType = if (effectiveDue > 0.001) "baki" else "cash"
            } else {
                val isBaki = paymentType.equals("baki", ignoreCase = true)
                effectivePaid = if (isBaki) 0.0 else grandTotal
                effectiveDue = if (isBaki) grandTotal else 0.0
                effectivePaymentType = if (isBaki) "baki" else "cash"
            }

            if (effectiveDue > 0.001 && customerId.isBlank()) {
                throw IllegalArgumentException(
                    if (lang == AppLanguage.BN) "বাকি রাখতে ক্রেতা নির্বাচন করুন" else "Customer required for unpaid balance"
                )
            }

            transaction.set(counterRef, mapOf("invoiceSeq" to currentSeq), SetOptions.merge())

            for ((prodRef, remainingStock) in deductions) {
                transaction.update(prodRef, "stock", remainingStock, "updatedAt", FieldValue.serverTimestamp())
            }

            if (effectiveDue > 0.001 && customerRef != null && customerSnap != null && customerSnap.exists()) {
                val newCustomerBalance = CalculationEngine.roundMoney(currentCustomerBalance + effectiveDue)
                transaction.update(
                    customerRef,
                    "currentBalance", newCustomerBalance,
                    "updatedAt", FieldValue.serverTimestamp()
                )
            }

            val salePayload = mapOf(
                "storeId" to storeId,
                "invoiceNo" to formattedInvoiceNo,
                "items" to itemMaps,
                "totalItems" to CalculationEngine.roundStock(totalItems),
                "subtotal" to CalculationEngine.roundMoney(subtotal),
                "discount" to totalDiscount,
                "grandTotal" to grandTotal,
                "paidAmount" to effectivePaid,
                "dueAmount" to effectiveDue,
                "profit" to netProfit,
                "paymentType" to effectivePaymentType,
                "customerId" to customerId.trim(),
                "customerName" to customerName.trim().ifEmpty { "Cash Sale" },
                "customerPhone" to customerPhone.trim(),
                "accountId" to (if (effectivePaid > 0.001) accountId else ""),
                "accountName" to (if (effectivePaid > 0.001) accountName else ""),
                "notes" to notes.trim(),
                "soldBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(saleRef, salePayload)

            formattedInvoiceNo
        }.await()
    }

    // -------------------------------------------------------------
    // CUSTOMERS MANAGEMENT
    // -------------------------------------------------------------

    fun observeCustomers(storeId: String): Flow<List<Customer>> {
        if (storeId == "demo_store" || db == null) {
            return _demoCustomers.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("customers")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Customer::class.java) }
                    .sortedBy { it.name.lowercase() }
            }
    }

    suspend fun addCustomer(storeId: String, customer: Customer): String {
        require(customer.name.isNotBlank()) { ShopLocale.get("customer_name_required", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            val newId = "cust_${System.currentTimeMillis()}"
            val newCust = customer.copy(id = newId, storeId = storeId, createdAt = Timestamp.now())
            _demoCustomers.value = (_demoCustomers.value + newCust).sortedBy { it.name.lowercase() }
            return newId
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("customers").document()
        val payload = mapOf(
            "storeId" to storeId,
            "name" to customer.name.trim(),
            "mobile" to customer.mobile.trim(),
            "address" to customer.address.trim(),
            "notes" to customer.notes.trim(),
            "currentBalance" to customer.currentBalance.coerceAtLeast(0.0),
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateCustomer(storeId: String, customer: Customer) {
        val lang = _currentLanguage.value
        require(customer.id.isNotBlank()) { ShopLocale.get("err_customer_id_blank", lang) }
        require(customer.name.isNotBlank()) { ShopLocale.get("customer_name_required", lang) }

        if (storeId == "demo_store" || db == null) {
            _demoCustomers.value = _demoCustomers.value.map {
                if (it.id == customer.id) customer.copy(updatedAt = Timestamp.now()) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("customers").document(customer.id)
        val payload = mapOf(
            "name" to customer.name.trim(),
            "mobile" to customer.mobile.trim(),
            "address" to customer.address.trim(),
            "notes" to customer.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
    }

    suspend fun deleteCustomer(storeId: String, customerId: String) {
        require(customerId.isNotBlank()) { ShopLocale.get("err_customer_id_blank", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            _demoCustomers.value = _demoCustomers.value.filter { it.id != customerId }
            return
        }

        requireUserId()
        db.collection("stores").document(storeId).collection("customers").document(customerId)
            .delete().await()
    }

    // -------------------------------------------------------------
    // BAKI PAYMENTS
    // -------------------------------------------------------------

    fun observePayments(storeId: String): Flow<List<Payment>> {
        if (storeId == "demo_store" || db == null) {
            return _demoPayments.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("payments")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Payment::class.java) }
                    .sortedByDescending { if (it.paymentDate > 0L) it.paymentDate else (it.createdAt?.toDate()?.time ?: 0L) }
            }
    }

    suspend fun recordCustomerPaymentAtomic(
        storeId: String,
        customerId: String,
        customerName: String,
        customerMobile: String,
        amount: Double,
        note: String,
        paymentDate: Long,
        accountId: String = "cash_in_hand",
        accountName: String = "Cash in Hand"
    ): String {
        val lang = _currentLanguage.value
        require(amount > 0.0) { ShopLocale.get("payment_positive_error", lang) }

        if (storeId == "demo_store" || db == null) {
            val seq = demoPaymentCounter.incrementAndGet()
            val paymentNo = CalculationEngine.formatPaymentNumber(seq)

            _demoCustomers.value = _demoCustomers.value.map { cust ->
                if (cust.id == customerId) {
                    cust.copy(currentBalance = (cust.currentBalance - amount).coerceAtLeast(0.0))
                } else cust
            }

            val newPay = Payment(
                id = "pay_${System.currentTimeMillis()}",
                storeId = storeId,
                paymentNo = paymentNo,
                customerId = customerId.trim(),
                customerName = customerName.trim(),
                customerMobile = customerMobile.trim(),
                amount = amount,
                paymentMethod = "cash",
                accountId = accountId,
                accountName = accountName,
                paymentDate = if (paymentDate > 0L) paymentDate else System.currentTimeMillis(),
                note = note.trim(),
                receivedBy = "demo_user",
                createdAt = Timestamp.now()
            )

            _demoPayments.value = listOf(newPay) + _demoPayments.value
            return paymentNo
        }

        val database = db ?: throw IllegalStateException(ShopLocale.get("err_firestore_unavailable", lang))
        val uid = requireUserId()
        val storeRef = database.collection("stores").document(storeId)
        val counterRef = storeRef.collection("meta").document("counters")
        val paymentRef = storeRef.collection("payments").document()
        val customerRef = if (customerId.isNotBlank() && !customerId.startsWith("legacy_")) {
            storeRef.collection("customers").document(customerId)
        } else null

        return database.runTransaction<String> { transaction ->
            val counterSnap = transaction.get(counterRef)
            val currentSeq = (counterSnap.getLong("paymentSeq") ?: 0L) + 1L

            val customerSnap = customerRef?.let { transaction.get(it) }
            val liveBalance = customerSnap?.getDouble("currentBalance") ?: Double.MAX_VALUE

            if (customerSnap != null && customerSnap.exists() && amount > (liveBalance + 0.001)) {
                throw IllegalStateException(
                    ShopLocale.format(
                        "err_payment_exceeds_live_bal",
                        lang,
                        ShopLocale.formatMoney(amount, lang),
                        ShopLocale.formatMoney(liveBalance, lang)
                    )
                )
            }

            val newBalance = if (customerSnap != null && customerSnap.exists()) {
                (liveBalance - amount).coerceAtLeast(0.0)
            } else 0.0

            val paymentNo = CalculationEngine.formatPaymentNumber(currentSeq)

            transaction.set(counterRef, mapOf("paymentSeq" to currentSeq), SetOptions.merge())

            if (customerRef != null && customerSnap != null && customerSnap.exists()) {
                transaction.update(
                    customerRef,
                    "currentBalance", newBalance,
                    "updatedAt", FieldValue.serverTimestamp()
                )
            }

            val paymentPayload = mapOf(
                "storeId" to storeId,
                "paymentNo" to paymentNo,
                "customerId" to customerId.trim(),
                "customerName" to customerName.trim(),
                "customerMobile" to customerMobile.trim(),
                "amount" to amount,
                "paymentMethod" to "cash",
                "accountId" to accountId,
                "accountName" to accountName,
                "paymentDate" to if (paymentDate > 0L) paymentDate else System.currentTimeMillis(),
                "note" to note.trim(),
                "receivedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(paymentRef, paymentPayload)

            paymentNo
        }.await()
    }

    // -------------------------------------------------------------
    // SUPPLIERS MANAGEMENT
    // -------------------------------------------------------------

    fun observeSuppliers(storeId: String): Flow<List<Supplier>> {
        if (storeId == "demo_store" || db == null) {
            return _demoSuppliers.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("suppliers")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Supplier::class.java) }
                    .sortedBy { it.name.lowercase() }
            }
    }

    suspend fun addSupplier(storeId: String, supplier: Supplier): String {
        require(supplier.name.isNotBlank()) { ShopLocale.get("supplier_name_required", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            val newId = "sup_${System.currentTimeMillis()}"
            val newSup = supplier.copy(id = newId, storeId = storeId, createdAt = Timestamp.now())
            _demoSuppliers.value = (_demoSuppliers.value + newSup).sortedBy { it.name.lowercase() }
            return newId
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("suppliers").document()
        val payload = mapOf(
            "storeId" to storeId,
            "name" to supplier.name.trim(),
            "mobile" to supplier.mobile.trim(),
            "company" to supplier.company.trim(),
            "address" to supplier.address.trim(),
            "balance" to supplier.balance.coerceAtLeast(0.0),
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateSupplier(storeId: String, supplier: Supplier) {
        val lang = _currentLanguage.value
        require(supplier.id.isNotBlank()) { ShopLocale.get("err_supplier_id_blank", lang) }
        require(supplier.name.isNotBlank()) { ShopLocale.get("supplier_name_required", lang) }

        if (storeId == "demo_store" || db == null) {
            _demoSuppliers.value = _demoSuppliers.value.map {
                if (it.id == supplier.id) supplier.copy(updatedAt = Timestamp.now()) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("suppliers").document(supplier.id)
        val payload = mapOf(
            "name" to supplier.name.trim(),
            "mobile" to supplier.mobile.trim(),
            "company" to supplier.company.trim(),
            "address" to supplier.address.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
    }

    suspend fun deleteSupplier(storeId: String, supplierId: String) {
        require(supplierId.isNotBlank()) { ShopLocale.get("err_supplier_id_blank", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            _demoSuppliers.value = _demoSuppliers.value.filter { it.id != supplierId }
            return
        }

        requireUserId()
        db.collection("stores").document(storeId).collection("suppliers").document(supplierId)
            .delete().await()
    }

    // -------------------------------------------------------------
    // PURCHASES & RETURNS
    // -------------------------------------------------------------

    fun observePurchases(storeId: String): Flow<List<Purchase>> {
        if (storeId == "demo_store" || db == null) {
            return _demoPurchases.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("purchases")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Purchase::class.java) }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun recordPurchase(storeId: String, purchase: Purchase): String {
        require(purchase.items.isNotEmpty()) { ShopLocale.get("purchase_min_one_item", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            val seq = demoPurchaseCounter.incrementAndGet()
            val purNo = String.format(java.util.Locale.US, "PUR-%04d", seq)
            val newPur = purchase.copy(
                id = "pur_${System.currentTimeMillis()}",
                purchaseNo = purNo,
                storeId = storeId,
                createdAt = Timestamp.now()
            )

            // Adjust supplier balance if credit
            if (purchase.dueAmount > 0.0 && purchase.supplierId.isNotBlank()) {
                _demoSuppliers.value = _demoSuppliers.value.map { sup ->
                    if (sup.id == purchase.supplierId) sup.copy(balance = sup.balance + purchase.dueAmount) else sup
                }
            }

            // Adjust stock in stock unit & update product buying price per stock unit
            val curProds = _demoProducts.value.toMutableList()
            for (item in purchase.items) {
                val idx = curProds.indexOfFirst { it.id == item.productId }
                if (idx != -1) {
                    val p = curProds[idx]
                    val stockToAdd = CalculationEngine.purchaseItemStockQty(item)
                    val updatedBuyPrice = if (item.costPerStockUnit > 0.0) {
                        item.costPerStockUnit
                    } else if (item.conversionFactor > 0.0) {
                        CalculationEngine.costPerStockUnit(item.buyPrice, item.conversionFactor)
                    } else {
                        item.buyPrice
                    }
                    curProds[idx] = p.copy(
                        stock = CalculationEngine.roundStock(p.stock + stockToAdd),
                        buyPrice = updatedBuyPrice
                    )
                }
            }
            _demoProducts.value = curProds
            _demoPurchases.value = listOf(newPur) + _demoPurchases.value
            return purNo
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("purchases").document()
        val payload = mapOf(
            "storeId" to storeId,
            "purchaseNo" to purchase.purchaseNo.ifBlank { "PUR-${System.currentTimeMillis() % 10000}" },
            "supplierId" to purchase.supplierId,
            "supplierName" to purchase.supplierName,
            "supplierMobile" to purchase.supplierMobile,
            "items" to purchase.items.map {
                val stockQ = CalculationEngine.purchaseItemStockQty(it)
                val conv = if (it.conversionFactor > 0.0) it.conversionFactor else 1.0
                val unitCost = if (it.costPerStockUnit > 0.0) it.costPerStockUnit else CalculationEngine.costPerStockUnit(it.buyPrice, conv)
                mapOf(
                    "productId" to it.productId,
                    "name" to it.name,
                    "qty" to it.qty,
                    "unit" to it.unit,
                    "stockQty" to stockQ,
                    "conversionFactor" to conv,
                    "costPerStockUnit" to unitCost,
                    "buyPrice" to it.buyPrice,
                    "lineTotal" to it.lineTotal
                )
            },
            "totalItems" to CalculationEngine.roundStock(purchase.totalItems),
            "totalAmount" to purchase.totalAmount,
            "paidAmount" to purchase.paidAmount,
            "dueAmount" to purchase.dueAmount,
            "paymentType" to purchase.paymentType,
            "accountId" to purchase.accountId.ifBlank { "cash_in_hand" },
            "accountName" to purchase.accountName.ifBlank { "Cash in Hand" },
            "note" to purchase.note,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()

        if (purchase.dueAmount > 0.0 && purchase.supplierId.isNotBlank()) {
            db.collection("stores").document(storeId).collection("suppliers").document(purchase.supplierId)
                .update("balance", FieldValue.increment(purchase.dueAmount))
        }

        for (item in purchase.items) {
            val stockToAdd = CalculationEngine.purchaseItemStockQty(item)
            val conv = if (item.conversionFactor > 0.0) item.conversionFactor else 1.0
            val updatedBuyPrice = if (item.costPerStockUnit > 0.0) item.costPerStockUnit else CalculationEngine.costPerStockUnit(item.buyPrice, conv)
            db.collection("stores").document(storeId).collection("products").document(item.productId)
                .update(
                    mapOf(
                        "stock" to FieldValue.increment(stockToAdd),
                        "buyPrice" to updatedBuyPrice,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
        }

        return docRef.id
    }

    fun observePurchaseReturns(storeId: String): Flow<List<PurchaseReturn>> {
        if (storeId == "demo_store" || db == null) {
            return _demoPurchaseReturns.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("purchaseReturns")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(PurchaseReturn::class.java) }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun recordPurchaseReturn(storeId: String, returnItem: PurchaseReturn): String {
        require(returnItem.amount > 0.0) { ShopLocale.get("err_return_amount_positive", _currentLanguage.value) }

        if (storeId == "demo_store" || db == null) {
            val seq = demoReturnCounter.incrementAndGet()
            val retNo = String.format(java.util.Locale.US, "RET-%04d", seq)
            val newRet = returnItem.copy(
                id = "ret_${System.currentTimeMillis()}",
                returnNo = retNo,
                storeId = storeId,
                createdAt = Timestamp.now()
            )
            if (returnItem.supplierId.isNotBlank()) {
                _demoSuppliers.value = _demoSuppliers.value.map { sup ->
                    if (sup.id == returnItem.supplierId) sup.copy(balance = (sup.balance - returnItem.amount).coerceAtLeast(0.0)) else sup
                }
            }
            _demoPurchaseReturns.value = listOf(newRet) + _demoPurchaseReturns.value
            return retNo
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId).collection("purchaseReturns").document()
        val payload = mapOf(
            "storeId" to storeId,
            "returnNo" to returnItem.returnNo.ifBlank { "RET-${System.currentTimeMillis() % 10000}" },
            "purchaseId" to returnItem.purchaseId,
            "supplierId" to returnItem.supplierId,
            "supplierName" to returnItem.supplierName,
            "amount" to returnItem.amount,
            "reason" to returnItem.reason,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()

        if (returnItem.supplierId.isNotBlank()) {
            db.collection("stores").document(storeId).collection("suppliers").document(returnItem.supplierId)
                .update("balance", FieldValue.increment(-returnItem.amount))
        }

        return docRef.id
    }

    // -------------------------------------------------------------
    // HEALTH FIX LOGS & AUDITED FIXES
    // -------------------------------------------------------------

    fun observeHealthFixLogs(storeId: String): Flow<List<HealthFixLog>> {
        if (storeId == "demo_store" || db == null) {
            return _demoHealthFixLogs.asStateFlow()
        }
        return db.collection("stores").document(storeId).collection("healthFixLogs")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(HealthFixLog::class.java) }
                    .sortedByDescending { it.fixedAt?.toDate()?.time ?: 0L }
            }
    }

    suspend fun applyHealthFix(storeId: String, problem: HealthProblem, ownerUid: String): Boolean {
        val fixLog = HealthFixLog(
            id = "fix_${System.currentTimeMillis()}",
            storeId = storeId,
            problemType = problem.type.name,
            description = problem.description,
            entityId = problem.entityId,
            entityCollection = problem.entityCollection,
            fixDetails = problem.fixDescription,
            fixedBy = ownerUid,
            fixedAt = Timestamp.now()
        )

        if (storeId == "demo_store" || db == null) {
            when (problem.entityCollection) {
                "products" -> {
                    _demoProducts.value = _demoProducts.value.map { p ->
                        if (p.id == problem.entityId) {
                            var updated = p
                            problem.fixActionPayload.forEach { (k, v) ->
                                when (k) {
                                    "stock" -> (v as? Number)?.toDouble()?.let { updated = updated.copy(stock = it) }
                                    "sellPrice" -> (v as? Number)?.toDouble()?.let { updated = updated.copy(sellPrice = it) }
                                    "barcode" -> (v as? String)?.let { updated = updated.copy(barcode = it) }
                                    "name" -> (v as? String)?.let { updated = updated.copy(name = it) }
                                    "category" -> (v as? String)?.let { updated = updated.copy(category = it) }
                                }
                            }
                            updated
                        } else p
                    }
                }
                "customers" -> {
                    _demoCustomers.value = _demoCustomers.value.map { c ->
                        if (c.id == problem.entityId) {
                            var updated = c
                            problem.fixActionPayload.forEach { (k, v) ->
                                when (k) {
                                    "currentBalance" -> (v as? Number)?.toDouble()?.let { updated = updated.copy(currentBalance = it) }
                                    "mobile" -> (v as? String)?.let { updated = updated.copy(mobile = it) }
                                }
                            }
                            updated
                        } else c
                    }
                }
                "suppliers" -> {
                    _demoSuppliers.value = _demoSuppliers.value.map { s ->
                        if (s.id == problem.entityId) {
                            var updated = s
                            problem.fixActionPayload.forEach { (k, v) ->
                                when (k) {
                                    "balance" -> (v as? Number)?.toDouble()?.let { updated = updated.copy(balance = it) }
                                    "mobile" -> (v as? String)?.let { updated = updated.copy(mobile = it) }
                                }
                            }
                            updated
                        } else s
                    }
                }
                "sales" -> {
                    _demoSales.value = _demoSales.value.map { s ->
                        if (s.id == problem.entityId) {
                            var updated = s
                            problem.fixActionPayload.forEach { (k, v) ->
                                when (k) {
                                    "customerId" -> (v as? String)?.let { updated = updated.copy(customerId = it) }
                                    "customerName" -> (v as? String)?.let { updated = updated.copy(customerName = it) }
                                }
                            }
                            updated
                        } else s
                    }
                }
                "purchases" -> {
                    _demoPurchases.value = _demoPurchases.value.map { pu ->
                        if (pu.id == problem.entityId) {
                            var updated = pu
                            problem.fixActionPayload.forEach { (k, v) ->
                                when (k) {
                                    "supplierId" -> (v as? String)?.let { updated = updated.copy(supplierId = it) }
                                }
                            }
                            updated
                        } else pu
                    }
                }
            }
            _demoHealthFixLogs.value = listOf(fixLog) + _demoHealthFixLogs.value
            return true
        }

        if (problem.entityCollection.isNotBlank() && problem.entityId.isNotBlank() && problem.fixActionPayload.isNotEmpty()) {
            val docRef = db.collection("stores").document(storeId)
                .collection(problem.entityCollection).document(problem.entityId)
            docRef.update(problem.fixActionPayload).await()
        }

        val logRef = db.collection("stores").document(storeId).collection("healthFixLogs").document()
        val logPayload = mapOf(
            "storeId" to storeId,
            "problemType" to fixLog.problemType,
            "description" to fixLog.description,
            "entityId" to fixLog.entityId,
            "entityCollection" to fixLog.entityCollection,
            "fixDetails" to fixLog.fixDetails,
            "fixedBy" to ownerUid,
            "fixedAt" to FieldValue.serverTimestamp()
        )
        logRef.set(logPayload).await()
        return true
    }

    // -------------------------------------------------------------
    // ACCOUNTS & CASH / BANK
    // -------------------------------------------------------------

    fun observeAccounts(storeId: String): Flow<List<Account>> {
        if (storeId == "demo_store" || db == null) {
            return _demoAccounts.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("accounts")
            .snapshots()
            .map { snap ->
                val list = snap.documents.mapNotNull { it.toObject(Account::class.java) }
                if (list.none { it.isDefault || it.id == "cash_in_hand" }) {
                    val defaultAccount = Account(
                        id = "cash_in_hand",
                        storeId = storeId,
                        name = "Cash in Hand",
                        type = "Cash",
                        accountNumber = "",
                        openingBalance = 0.0,
                        isDefault = true,
                        isArchived = false
                    )
                    listOf(defaultAccount) + list
                } else {
                    list
                }
            }
    }

    suspend fun createAccount(account: Account): String {
        val lang = _currentLanguage.value
        val nameTrimmed = account.name.trim()
        if (nameTrimmed.isBlank()) {
            throw IllegalArgumentException(ShopLocale.get("account_name_required", lang))
        }

        if (account.storeId == "demo_store" || db == null) {
            if (_demoAccounts.value.any { it.name.trim().equals(nameTrimmed, ignoreCase = true) }) {
                throw IllegalArgumentException(ShopLocale.get("account_already_exists", lang))
            }
            val newId = "acc_${System.currentTimeMillis()}"
            val newAcc = account.copy(
                id = newId,
                name = nameTrimmed,
                isDefault = false,
                isArchived = false,
                createdBy = "demo_user",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _demoAccounts.value = _demoAccounts.value + newAcc
            return newId
        }

        val colRef = db.collection("stores").document(account.storeId).collection("accounts")
        val existing = colRef.get().await()
        if (existing.documents.any { it.getString("name")?.trim().equals(nameTrimmed, ignoreCase = true) }) {
            throw IllegalArgumentException(ShopLocale.get("account_already_exists", lang))
        }

        val uid = requireUserId()
        val docRef = colRef.document()
        val payload = mapOf(
            "storeId" to account.storeId,
            "name" to nameTrimmed,
            "type" to account.type,
            "accountNumber" to account.accountNumber.trim(),
            "openingBalance" to account.openingBalance,
            "isDefault" to false,
            "isArchived" to false,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun archiveAccount(storeId: String, accountId: String, currentBalance: Double) {
        val lang = _currentLanguage.value
        if (accountId == "cash_in_hand") {
            throw IllegalArgumentException(ShopLocale.get("cannot_archive_default_account", lang))
        }
        if (kotlin.math.abs(currentBalance) > 0.001) {
            throw IllegalStateException(
                ShopLocale.format("archive_zero_balance_only", lang, ShopLocale.formatMoneyFixed2(currentBalance, lang))
            )
        }

        if (storeId == "demo_store" || db == null) {
            _demoAccounts.value = _demoAccounts.value.map { acc ->
                if (acc.id == accountId) {
                    if (acc.isDefault) throw IllegalArgumentException(ShopLocale.get("cannot_archive_default_account", lang))
                    acc.copy(isArchived = true)
                } else acc
            }
            return
        }

        val docRef = db.collection("stores").document(storeId).collection("accounts").document(accountId)
        val snap = docRef.get().await()
        if (snap.getBoolean("isDefault") == true) {
            throw IllegalArgumentException(ShopLocale.get("cannot_archive_default_account", lang))
        }
        docRef.update("isArchived", true, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    // -------------------------------------------------------------
    // BALANCE ADJUSTMENTS
    // -------------------------------------------------------------

    fun observeAdjustments(storeId: String): Flow<List<AccountAdjustment>> {
        if (storeId == "demo_store" || db == null) {
            return _demoAdjustments.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("accountAdjustments")
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(AccountAdjustment::class.java) } }
    }

    suspend fun recordAccountAdjustment(adjustment: AccountAdjustment): String {
        if (adjustment.note.trim().isBlank()) {
            throw IllegalArgumentException(ShopLocale.get("adjustment_note_required", _currentLanguage.value))
        }

        if (adjustment.storeId == "demo_store" || db == null) {
            val newId = "adj_${System.currentTimeMillis()}"
            val newAdj = adjustment.copy(
                id = newId,
                note = adjustment.note.trim(),
                adjustedBy = "demo_user",
                createdAt = Timestamp.now()
            )
            _demoAdjustments.value = listOf(newAdj) + _demoAdjustments.value
            return newId
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(adjustment.storeId)
            .collection("accountAdjustments").document()
        val payload = mapOf(
            "storeId" to adjustment.storeId,
            "accountId" to adjustment.accountId,
            "accountName" to adjustment.accountName,
            "oldBalance" to adjustment.oldBalance,
            "newBalance" to adjustment.newBalance,
            "difference" to adjustment.difference,
            "note" to adjustment.note.trim(),
            "adjustedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    // -------------------------------------------------------------
    // EXPENSES
    // -------------------------------------------------------------

    fun observeExpenses(storeId: String): Flow<List<Expense>> {
        if (storeId == "demo_store" || db == null) {
            return _demoExpenses.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("expenses")
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(Expense::class.java) } }
    }

    suspend fun recordExpense(expense: Expense): String {
        if (expense.amount <= 0.0) {
            throw IllegalArgumentException(ShopLocale.get("expense_amount_positive", _currentLanguage.value))
        }

        if (expense.storeId == "demo_store" || db == null) {
            val newId = "exp_${System.currentTimeMillis()}"
            val newExp = expense.copy(
                id = newId,
                createdBy = "demo_user",
                isDeleted = false,
                createdAt = Timestamp.now()
            )
            _demoExpenses.value = listOf(newExp) + _demoExpenses.value
            return newId
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(expense.storeId)
            .collection("expenses").document()
        val payload = mapOf(
            "storeId" to expense.storeId,
            "amount" to expense.amount,
            "category" to expense.category.ifBlank { "Other" },
            "accountId" to expense.accountId.ifBlank { "cash_in_hand" },
            "accountName" to expense.accountName.ifBlank { "Cash in Hand" },
            "date" to if (expense.date > 0L) expense.date else System.currentTimeMillis(),
            "note" to expense.note.trim(),
            "receiptUri" to expense.receiptUri,
            "createdBy" to uid,
            "isDeleted" to false,
            "deletedNote" to "",
            "deletedBy" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun deleteExpense(storeId: String, expenseId: String, deleteNote: String) {
        if (storeId == "demo_store" || db == null) {
            _demoExpenses.value = _demoExpenses.value.map {
                if (it.id == expenseId) it.copy(
                    isDeleted = true,
                    deletedNote = deleteNote.trim(),
                    deletedBy = "demo_user",
                    deletedAt = Timestamp.now()
                ) else it
            }
            return
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId)
            .collection("expenses").document(expenseId)
        val payload = mapOf(
            "isDeleted" to true,
            "deletedNote" to deleteNote.trim(),
            "deletedBy" to uid,
            "deletedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
    }

    // -------------------------------------------------------------
    // OTHER INCOME
    // -------------------------------------------------------------

    fun observeOtherIncomes(storeId: String): Flow<List<OtherIncome>> {
        if (storeId == "demo_store" || db == null) {
            return _demoOtherIncomes.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("otherIncomes")
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(OtherIncome::class.java) } }
    }

    suspend fun recordOtherIncome(income: OtherIncome): String {
        if (income.amount <= 0.0) {
            throw IllegalArgumentException(ShopLocale.get("income_amount_positive", _currentLanguage.value))
        }

        if (income.storeId == "demo_store" || db == null) {
            val newId = "inc_${System.currentTimeMillis()}"
            val newInc = income.copy(
                id = newId,
                createdBy = "demo_user",
                isDeleted = false,
                createdAt = Timestamp.now()
            )
            _demoOtherIncomes.value = listOf(newInc) + _demoOtherIncomes.value
            return newId
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(income.storeId)
            .collection("otherIncomes").document()
        val payload = mapOf(
            "storeId" to income.storeId,
            "amount" to income.amount,
            "category" to income.category.ifBlank { "Other" },
            "accountId" to income.accountId.ifBlank { "cash_in_hand" },
            "accountName" to income.accountName.ifBlank { "Cash in Hand" },
            "date" to if (income.date > 0L) income.date else System.currentTimeMillis(),
            "note" to income.note.trim(),
            "createdBy" to uid,
            "isDeleted" to false,
            "deletedNote" to "",
            "deletedBy" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun deleteOtherIncome(storeId: String, incomeId: String, deleteNote: String) {
        if (storeId == "demo_store" || db == null) {
            _demoOtherIncomes.value = _demoOtherIncomes.value.map {
                if (it.id == incomeId) it.copy(
                    isDeleted = true,
                    deletedNote = deleteNote.trim(),
                    deletedBy = "demo_user",
                    deletedAt = Timestamp.now()
                ) else it
            }
            return
        }

        val uid = requireUserId()
        val docRef = db.collection("stores").document(storeId)
            .collection("otherIncomes").document(incomeId)
        val payload = mapOf(
            "isDeleted" to true,
            "deletedNote" to deleteNote.trim(),
            "deletedBy" to uid,
            "deletedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(payload).await()
    }

    // -------------------------------------------------------------
    // STORE CATEGORIES (Expense & Income)
    // -------------------------------------------------------------

    fun observeCategories(storeId: String): Flow<List<StoreCategory>> {
        if (storeId == "demo_store" || db == null) {
            return _demoCategories.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("categories")
            .snapshots()
            .map { snap ->
                val list = snap.documents.mapNotNull { it.toObject(StoreCategory::class.java) }
                if (list.isEmpty()) {
                    // Seed default categories in memory if none present
                    listOf(
                        StoreCategory(id = "cat_1", storeId = storeId, type = "expense", name = "Rent"),
                        StoreCategory(id = "cat_2", storeId = storeId, type = "expense", name = "Electricity"),
                        StoreCategory(id = "cat_3", storeId = storeId, type = "expense", name = "Salary"),
                        StoreCategory(id = "cat_4", storeId = storeId, type = "expense", name = "Transport"),
                        StoreCategory(id = "cat_5", storeId = storeId, type = "expense", name = "Tea and Snacks"),
                        StoreCategory(id = "cat_6", storeId = storeId, type = "expense", name = "Repair"),
                        StoreCategory(id = "cat_7", storeId = storeId, type = "expense", name = "Other"),
                        StoreCategory(id = "cat_8", storeId = storeId, type = "income", name = "Commission"),
                        StoreCategory(id = "cat_9", storeId = storeId, type = "income", name = "Rent Received"),
                        StoreCategory(id = "cat_10", storeId = storeId, type = "income", name = "Other")
                    )
                } else {
                    list
                }
            }
    }

    suspend fun createCategory(category: StoreCategory): String {
        val lang = _currentLanguage.value
        val nameTrimmed = category.name.trim()
        if (nameTrimmed.isBlank()) {
            throw IllegalArgumentException(ShopLocale.get("category_name_required", lang))
        }

        if (category.storeId == "demo_store" || db == null) {
            if (_demoCategories.value.any { it.type == category.type && it.name.trim().equals(nameTrimmed, ignoreCase = true) }) {
                throw IllegalArgumentException(ShopLocale.get("category_exists", lang))
            }
            val newId = "cat_${System.currentTimeMillis()}"
            val newCat = category.copy(
                id = newId,
                name = nameTrimmed,
                isArchived = false,
                createdBy = "demo_user",
                createdAt = Timestamp.now()
            )
            _demoCategories.value = _demoCategories.value + newCat
            return newId
        }

        val colRef = db.collection("stores").document(category.storeId).collection("categories")
        val existing = colRef.whereEqualTo("type", category.type).get().await()
        if (existing.documents.any { it.getString("name")?.trim().equals(nameTrimmed, ignoreCase = true) }) {
            throw IllegalArgumentException(ShopLocale.get("category_exists", lang))
        }

        val uid = requireUserId()
        val docRef = colRef.document()
        val payload = mapOf(
            "storeId" to category.storeId,
            "type" to category.type,
            "name" to nameTrimmed,
            "isArchived" to false,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateCategory(storeId: String, categoryId: String, newName: String) {
        val nameTrimmed = newName.trim()
        if (nameTrimmed.isBlank()) throw IllegalArgumentException(ShopLocale.get("category_name_required", _currentLanguage.value))

        if (storeId == "demo_store" || db == null) {
            _demoCategories.value = _demoCategories.value.map {
                if (it.id == categoryId) it.copy(name = nameTrimmed) else it
            }
            return
        }

        val docRef = db.collection("stores").document(storeId)
            .collection("categories").document(categoryId)
        docRef.update("name", nameTrimmed).await()
    }

    suspend fun archiveCategory(storeId: String, categoryId: String) {
        if (storeId == "demo_store" || db == null) {
            _demoCategories.value = _demoCategories.value.map {
                if (it.id == categoryId) it.copy(isArchived = true) else it
            }
            return
        }

        val docRef = db.collection("stores").document(storeId)
            .collection("categories").document(categoryId)
        docRef.update("isArchived", true).await()
    }

    // -------------------------------------------------------------
    // STORE NOTES
    // -------------------------------------------------------------

    fun observeNotes(storeId: String): Flow<List<StoreNote>> {
        if (storeId == "demo_store" || db == null) {
            return _demoNotes.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("notes")
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(StoreNote::class.java) } }
    }

    suspend fun saveNote(note: StoreNote): String {
        val titleTrimmed = note.title.trim()
        if (titleTrimmed.isBlank()) throw IllegalArgumentException(ShopLocale.get("note_title_required", _currentLanguage.value))

        if (note.storeId == "demo_store" || db == null) {
            if (note.id.isNotBlank()) {
                _demoNotes.value = _demoNotes.value.map {
                    if (it.id == note.id) note.copy(updatedAt = Timestamp.now()) else it
                }
                return note.id
            } else {
                val newId = "note_${System.currentTimeMillis()}"
                val newNote = note.copy(
                    id = newId,
                    title = titleTrimmed,
                    date = if (note.date > 0L) note.date else System.currentTimeMillis(),
                    createdBy = "demo_user",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                _demoNotes.value = listOf(newNote) + _demoNotes.value
                return newId
            }
        }

        val uid = requireUserId()
        val colRef = db.collection("stores").document(note.storeId).collection("notes")
        if (note.id.isNotBlank()) {
            val docRef = colRef.document(note.id)
            val payload = mapOf(
                "title" to titleTrimmed,
                "text" to note.text.trim(),
                "date" to if (note.date > 0L) note.date else System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            return note.id
        } else {
            val docRef = colRef.document()
            val payload = mapOf(
                "storeId" to note.storeId,
                "title" to titleTrimmed,
                "text" to note.text.trim(),
                "date" to if (note.date > 0L) note.date else System.currentTimeMillis(),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        }
    }

    suspend fun deleteNote(storeId: String, noteId: String) {
        if (storeId == "demo_store" || db == null) {
            _demoNotes.value = _demoNotes.value.filter { it.id != noteId }
            return
        }

        val docRef = db.collection("stores").document(storeId)
            .collection("notes").document(noteId)
        docRef.delete().await()
    }

    // -------------------------------------------------------------
    // SUPPLIER PAYMENTS (PAYMENTS OUT)
    // -------------------------------------------------------------

    fun observeSupplierPayments(storeId: String): Flow<List<SupplierPayment>> {
        if (storeId == "demo_store" || db == null) {
            return _demoSupplierPayments.asStateFlow()
        }
        return db.collection("stores").document(storeId)
            .collection("supplierPayments")
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(SupplierPayment::class.java) } }
    }

    suspend fun recordSupplierPayment(payment: SupplierPayment): String {
        if (payment.amount <= 0.0) throw IllegalArgumentException(ShopLocale.get("payment_positive_error", _currentLanguage.value))

        if (payment.storeId == "demo_store" || db == null) {
            val newId = "sp_${System.currentTimeMillis()}"
            val newPay = payment.copy(
                id = newId,
                paidBy = "demo_user",
                createdAt = Timestamp.now()
            )
            _demoSupplierPayments.value = listOf(newPay) + _demoSupplierPayments.value

            // Reduce supplier debt in demo
            _demoSuppliers.value = _demoSuppliers.value.map { sup ->
                if (sup.id == payment.supplierId) {
                    sup.copy(balance = (sup.balance - payment.amount).coerceAtLeast(0.0))
                } else sup
            }
            return newId
        }

        val uid = requireUserId()
        val storeDoc = db.collection("stores").document(payment.storeId)
        val payDoc = storeDoc.collection("supplierPayments").document()
        val supDoc = storeDoc.collection("suppliers").document(payment.supplierId)

        db.runTransaction { tx ->
            val supSnap = tx.get(supDoc)
            val currentBal = supSnap.getDouble("balance") ?: 0.0
            val newBal = (currentBal - payment.amount).coerceAtLeast(0.0)

            val payload = mapOf(
                "storeId" to payment.storeId,
                "paymentNo" to payment.paymentNo.ifBlank { "PAY-OUT-${System.currentTimeMillis().toString().takeLast(6)}" },
                "supplierId" to payment.supplierId,
                "supplierName" to payment.supplierName,
                "amount" to payment.amount,
                "accountId" to payment.accountId.ifBlank { "cash_in_hand" },
                "accountName" to payment.accountName.ifBlank { "Cash in Hand" },
                "paymentDate" to if (payment.paymentDate > 0L) payment.paymentDate else System.currentTimeMillis(),
                "note" to payment.note.trim(),
                "paidBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            tx.set(payDoc, payload)
            tx.update(supDoc, "balance", newBal, "updatedAt", FieldValue.serverTimestamp())
        }.await()

        return payDoc.id
    }

    // -------------------------------------------------------------
    // STAFF MEMBERS & INVITES
    // -------------------------------------------------------------
    fun observeMyMember(storeId: String, uid: String): Flow<StoreMember?> {
        val database = db
        if (storeId == "demo_store" || database == null || uid.isBlank()) {
            return _demoMembers.map { list -> list.find { it.userId == uid } }
        }
        return database.collection("stores").document(storeId)
            .collection("members").document(uid)
            .snapshots()
            .map { snap -> if (snap.exists()) snap.toObject(StoreMember::class.java) else null }
            .catch { e ->
                if (e is CancellationException) throw e
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, "stores/$storeId/members/$uid")
                emit(null)
            }
    }

    fun observeMembers(storeId: String): Flow<List<StoreMember>> {
        val database = db
        if (storeId == "demo_store" || database == null) {
            return _demoMembers.asStateFlow()
        }
        return database.collection("stores").document(storeId)
            .collection("members")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(StoreMember::class.java)
                if (list.isNotEmpty()) list else {
                    val storeDoc = runCatching {
                        database.collection("stores").document(storeId).get().await()
                    }.getOrNull()
                    storeDoc?.toObject(Store::class.java)?.members ?: emptyList()
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, "stores/$storeId/members")
                emit(emptyList())
            }
    }

    fun observeInvites(storeId: String): Flow<List<StoreInvite>> {
        val database = db
        if (storeId == "demo_store" || database == null) {
            return flowOf(emptyList())
        }
        val uid = currentUid ?: return flowOf(emptyList())
        return database.collection("invites")
            .whereEqualTo("storeId", storeId)
            .whereEqualTo("createdByUid", uid)
            .snapshots()
            .map { it.toObjects(StoreInvite::class.java) }
            .catch { e ->
                if (e is CancellationException) throw e
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, "invites")
                emit(emptyList())
            }
    }

    suspend fun createStaffInvite(invite: StoreInvite): String {
        val database = db
        if (invite.storeId == "demo_store" || database == null) {
            throw IllegalStateException("Staff invites are unavailable in local demo mode.")
        }
        val code = if (invite.code.isBlank()) {
            InviteCodeConfig.generate()
        } else {
            InviteCodeConfig.normalize(invite.code)
        }
        require(InviteCodeConfig.isValidFormat(code)) {
            ShopLocale.format("err_invite_code_format", _currentLanguage.value, InviteCodeConfig.CODE_LENGTH)
        }
        val uid = requireUserId()
        val createdMs = if (invite.createdAt > 0L) invite.createdAt else System.currentTimeMillis()
        val expiresMs = if (invite.expiresAt > createdMs) invite.expiresAt else createdMs + (24 * 60 * 60 * 1000L)
        val inviteWithCode = invite.copy(
            code = code,
            id = code,
            createdByUid = uid,
            status = "active",
            createdAt = createdMs,
            expiresAt = expiresMs,
            usedByUid = "",
            usedByEmail = "",
            usedAt = 0L
        )
        val inviteRef = database.collection("invites").document(code)
        val payload = mapOf(
            "code" to code,
            "storeId" to inviteWithCode.storeId,
            "storeName" to inviteWithCode.storeName,
            "staffName" to inviteWithCode.staffName,
            "roleLabel" to inviteWithCode.roleLabel,
            "permissionLevel" to inviteWithCode.permissionLevel,
            "permissions" to inviteWithCode.permissions,
            "createdByUid" to uid,
            "createdByName" to inviteWithCode.createdByName,
            "status" to "active",
            "createdAt" to createdMs,
            "expiresAt" to expiresMs,
            "usedByUid" to "",
            "usedByEmail" to "",
            "usedAt" to 0L
        )
        try {
            inviteRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "invites/$code")
            throw e
        }
        return code
    }

    suspend fun cancelInvite(code: String) {
        val cleanCode = InviteCodeConfig.normalize(code)
        val database = db
            ?: throw IllegalStateException("Staff invites are unavailable in local demo mode.")
        try {
            database.collection("invites").document(cleanCode)
                .update("status", "cancelled", "updatedAt", FieldValue.serverTimestamp())
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, "invites/$cleanCode")
            throw e
        }
    }

    suspend fun joinStoreWithInvite(
        code: String,
        uid: String,
        email: String,
        displayName: String
    ): Result<String> {
        val database = db
            ?: return Result.failure(IllegalStateException("Joining with an invite code requires a cloud database."))
        val lang = _currentLanguage.value
        val now = System.currentTimeMillis()
        val cleanCode = InviteCodeConfig.normalize(code)

        if (!InviteCodeConfig.isValidFormat(cleanCode)) {
            return Result.failure(
                IllegalArgumentException(ShopLocale.format("err_wrong_invite_code", lang, InviteCodeConfig.CODE_LENGTH))
            )
        }

        // 1. Check if user already owns or belongs to another store
        val currentProfile = getUserProfileOnce(uid)
        if (currentProfile != null && currentProfile.storeId.isNotBlank()) {
            return Result.failure(
                IllegalStateException(ShopLocale.get("err_already_linked_store", lang))
            )
        }

        // Real Firestore Mode with single atomic Transaction.
        // Note: Non-members cannot read /stores/{storeId} until after joining, so we read only /invites/{code}
        // and update /stores/{storeId}.memberUids atomically with FieldValue.arrayUnion(uid).
        val inviteRef = database.collection("invites").document(cleanCode)
        return try {
            val storeIdResult = database.runTransaction { tx ->
                val inviteSnap = tx.get(inviteRef)
                if (!inviteSnap.exists()) {
                    throw IllegalArgumentException(ShopLocale.format("err_wrong_invite_code", lang, InviteCodeConfig.CODE_LENGTH))
                }
                val invite = inviteSnap.toObject(StoreInvite::class.java)
                    ?: throw IllegalArgumentException(ShopLocale.get("err_invite_corrupt", lang))

                when {
                    invite.status == "cancelled" ->
                        throw IllegalStateException(ShopLocale.get("err_invite_cancelled", lang))
                    invite.status == "used" ->
                        throw IllegalStateException(ShopLocale.get("err_invite_used", lang))
                    now > invite.expiresAt ->
                        throw IllegalStateException(ShopLocale.get("err_invite_expired", lang))
                    invite.storeId.isBlank() ->
                        throw IllegalArgumentException(ShopLocale.get("err_invite_store_missing", lang))
                    invite.createdByUid == uid ->
                        throw IllegalStateException(ShopLocale.get("err_already_owner", lang))
                }

                val targetStoreId = invite.storeId
                val storeRef = database.collection("stores").document(targetStoreId)
                val memberRef = storeRef.collection("members").document(uid)
                val userRef = database.collection("users").document(uid)

                val newMember = StoreMember(
                    userId = uid,
                    displayName = invite.staffName.ifBlank { displayName.ifBlank { ShopLocale.get("staff_member_default", lang) } },
                    email = email,
                    role = invite.roleLabel.lowercase(),
                    roleLabel = invite.roleLabel,
                    permissionLevel = invite.permissionLevel,
                    permissions = invite.permissions,
                    status = "Active",
                    joinedTime = now,
                    invitedBy = invite.createdByUid,
                    inviteCode = cleanCode
                )

                tx.update(
                    inviteRef,
                    mapOf(
                        "status" to "used",
                        "usedByUid" to uid,
                        "usedByEmail" to email,
                        "usedAt" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(memberRef, newMember)

                tx.update(
                    storeRef,
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    userRef,
                    mapOf(
                        "email" to email,
                        "storeId" to targetStoreId,
                        "role" to invite.roleLabel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )

                targetStoreId
            }.await()

            Result.success(storeIdResult)
        } catch (e: FirebaseFirestoreException) {
            val friendlyMessage = resolveBlockedJoinMessage(inviteRef, uid, e)
            Result.failure(IllegalStateException(friendlyMessage, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveBlockedJoinMessage(
        inviteRef: com.google.firebase.firestore.DocumentReference,
        uid: String,
        exception: FirebaseFirestoreException
    ): String {
        val lang = _currentLanguage.value
        if (exception.code == FirebaseFirestoreException.Code.NOT_FOUND) {
            return ShopLocale.get("err_invite_not_found", lang)
        }
        val latestInvite = runCatching {
            inviteRef.get().await().toObject(StoreInvite::class.java)
        }.getOrNull()
        return when {
            latestInvite == null ->
                ShopLocale.format("err_wrong_invite_code", lang, InviteCodeConfig.CODE_LENGTH)
            latestInvite.status == "cancelled" ->
                ShopLocale.get("err_invite_cancelled", lang)
            latestInvite.status == "used" ->
                ShopLocale.get("err_invite_used", lang)
            System.currentTimeMillis() > latestInvite.expiresAt ->
                ShopLocale.get("err_invite_expired", lang)
            latestInvite.createdByUid == uid ->
                ShopLocale.get("err_already_owner", lang)
            else ->
                ShopLocale.get("err_invite_blocked", lang)
        }
    }

    suspend fun updateMemberPermissions(
        storeId: String,
        memberId: String,
        roleLabel: String,
        permissionLevel: String,
        permissions: Map<String, Boolean>
    ) {
        val database = db
        if (storeId == "demo_store" || database == null) {
            throw IllegalStateException("Staff permission management is unavailable in local demo mode.")
        }

        val memberRef = database.collection("stores").document(storeId).collection("members").document(memberId)
        val payload = mapOf(
            "roleLabel" to roleLabel,
            "role" to roleLabel.lowercase(),
            "permissionLevel" to permissionLevel,
            "permissions" to permissions,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        memberRef.set(payload, SetOptions.merge()).await()
    }

    suspend fun setMemberFatherMode(
        storeId: String,
        memberId: String,
        enabled: Boolean,
        fallbackName: String = "Owner",
        fallbackEmail: String = "",
        isOwnerTarget: Boolean = false
    ) {
        val database = db
        if (storeId == "demo_store" || database == null) {
            val exists = _demoMembers.value.any { it.userId == memberId }
            if (!exists) {
                val basePerms = (if (isOwnerTarget) StorePermissions.defaultOwnerPermissions() else StorePermissions.forLevel("full")) +
                    (StorePermissions.FATHER_MODE to enabled)
                _demoMembers.value = _demoMembers.value + StoreMember(
                    userId = memberId,
                    displayName = fallbackName.ifBlank { "Owner" },
                    email = fallbackEmail,
                    role = if (isOwnerTarget) "owner" else "staff",
                    roleLabel = if (isOwnerTarget) "Owner" else "Staff",
                    permissionLevel = "Full",
                    permissions = basePerms,
                    status = "Active"
                )
            } else {
                _demoMembers.value = _demoMembers.value.map { mem ->
                    if (mem.userId == memberId) {
                        mem.copy(permissions = mem.permissions + (StorePermissions.FATHER_MODE to enabled))
                    } else mem
                }
            }
            if (enabled) {
                cacheUserSimpleModeLocally(memberId, true)
            }
            return
        }

        requireUserId()
        val memberRef = database.collection("stores").document(storeId).collection("members").document(memberId)
        val existingSnap = runCatching { memberRef.get().await() }.getOrNull()
        val existingMember = existingSnap?.toObject(StoreMember::class.java)
        if (existingMember == null) {
            val basePerms = (if (isOwnerTarget) StorePermissions.defaultOwnerPermissions() else StorePermissions.forLevel("full")) +
                (StorePermissions.FATHER_MODE to enabled)
            val newMemberPayload = mapOf(
                "userId" to memberId,
                "displayName" to fallbackName.ifBlank { "Owner" },
                "email" to fallbackEmail,
                "role" to if (isOwnerTarget) "owner" else "staff",
                "roleLabel" to if (isOwnerTarget) "Owner" else "Staff",
                "permissionLevel" to "Full",
                "permissions" to basePerms,
                "status" to "Active",
                "joinedTime" to System.currentTimeMillis(),
                "invitedBy" to "",
                "inviteCode" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(newMemberPayload).await()
        } else {
            val updatedPerms = existingMember.permissions + (StorePermissions.FATHER_MODE to enabled)
            memberRef.update(
                mapOf(
                    "permissions" to updatedPerms,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        if (enabled && memberId == currentUid) {
            cacheUserSimpleModeLocally(memberId, true)
            database.collection("users").document(memberId)
                .set(
                    mapOf(
                        "isSimpleMode" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    suspend fun removeMember(storeId: String, memberId: String) {
        val database = db
        if (storeId == "demo_store" || database == null) {
            throw IllegalStateException("Staff management is unavailable in local demo mode.")
        }

        val storeRef = database.collection("stores").document(storeId)
        val memberRef = storeRef.collection("members").document(memberId)

        database.runTransaction { tx ->
            val storeSnap = tx.get(storeRef)
            val currentUids = (storeSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updatedUids = currentUids.filter { it != memberId }

            tx.update(memberRef, "status", "Removed", "updatedAt", FieldValue.serverTimestamp())
            tx.update(storeRef, "memberUids", updatedUids, "updatedAt", FieldValue.serverTimestamp())
        }.await()
    }

    suspend fun restoreMember(storeId: String, memberId: String) {
        val database = db
        if (storeId == "demo_store" || database == null) {
            throw IllegalStateException("Staff management is unavailable in local demo mode.")
        }

        val storeRef = database.collection("stores").document(storeId)
        val memberRef = storeRef.collection("members").document(memberId)

        database.runTransaction { tx ->
            val storeSnap = tx.get(storeRef)
            val currentUids = (storeSnap.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updatedUids = (currentUids + memberId).distinct()

            tx.update(memberRef, "status", "Active", "updatedAt", FieldValue.serverTimestamp())
            tx.update(storeRef, "memberUids", updatedUids, "updatedAt", FieldValue.serverTimestamp())
        }.await()
    }

    // -------------------------------------------------------------
    // BACKUP EXPORT & IMPORT (OWNER ONLY)
    // -------------------------------------------------------------

    private data class BackupBatchWrite(
        val ref: DocumentReference,
        val data: Map<String, Any>,
        val isUpdate: Boolean = false
    )

    private suspend fun requireStoreOwnerForBackup(
        storeId: String,
        actingUserId: String? = null
    ): Pair<Store, String> {
        val lang = _currentLanguage.value
        val database = db
        val isDemo = (storeId == "demo_store" || database == null)
        if (isDemo) {
            val callerUid = (actingUserId ?: requireUserId()).trim()
            val store = _demoStore.value
                ?: throw IllegalStateException(ShopLocale.get("backup_err_owner_only", lang))
            if (callerUid.isBlank() || callerUid != store.ownerId) {
                throw SecurityException(ShopLocale.get("backup_err_owner_only", lang))
            }
            return store to callerUid
        }

        val uid = auth?.currentUser?.uid
            ?: throw SecurityException(ShopLocale.get("backup_err_owner_only", lang))
        if (!actingUserId.isNullOrBlank() && actingUserId != uid) {
            throw SecurityException(ShopLocale.get("backup_err_owner_only", lang))
        }
        val storeSnap = database.collection("stores").document(storeId).get().await()
        if (!storeSnap.exists()) {
            throw IllegalStateException("Store not found")
        }
        val store = storeSnap.toObject(Store::class.java)?.copy(id = storeSnap.id)
            ?: throw IllegalStateException("Store not found")
        if (store.ownerId != uid) {
            throw SecurityException(ShopLocale.get("backup_err_owner_only", lang))
        }
        return store to uid
    }

    private fun resolveProductPhotoThumbnailForExport(storeId: String, product: Product): String {
        val raw = product.photoUri.trim()
        if (raw.isBlank()) return ""
        if (ProductImageUtil.isSyncDataUri(raw)) {
            return if (raw.length <= 98_000) raw else ""
        }
        val ctx = appContext ?: return raw
        val targetUri: Uri? = when {
            ProductImageUtil.isLocalOnlyFlag(raw) -> {
                ProductImageUtil.findLocalOnlyPhotoFile(ctx, storeId, product.id)?.let { Uri.fromFile(it) }
            }
            ProductImageUtil.isLocalPhotoUri(raw) -> {
                if (ProductImageUtil.isLocalFilePresent(ctx, raw)) Uri.parse(raw) else null
            }
            else -> null
        }
        if (targetUri == null) return raw
        return try {
            val bytes = ProductImageUtil.compressUriForSync(ctx, targetUri)
            val dataUri = ProductImageUtil.encodeBytesToDataUri(bytes)
            if (dataUri.length <= 98_000) dataUri else raw
        } catch (_: Exception) {
            raw
        }
    }

    suspend fun isStoreEmptyForImport(storeId: String): Boolean {
        val database = db
        if (storeId == "demo_store" || database == null) {
            val onlyDefaultAccount = _demoAccounts.value.all { it.id == "cash_in_hand" && it.openingBalance == 0.0 }
            return _demoProducts.value.isEmpty() &&
                _demoCustomers.value.isEmpty() &&
                _demoSuppliers.value.isEmpty() &&
                _demoSales.value.isEmpty() &&
                _demoPurchases.value.isEmpty() &&
                _demoPurchaseReturns.value.isEmpty() &&
                _demoPayments.value.isEmpty() &&
                _demoSupplierPayments.value.isEmpty() &&
                _demoAdjustments.value.isEmpty() &&
                _demoExpenses.value.isEmpty() &&
                _demoOtherIncomes.value.isEmpty() &&
                _demoNotes.value.isEmpty() &&
                onlyDefaultAccount
        }

        val storeRef = database.collection("stores").document(storeId)
        val collectionsToCheck = listOf(
            "products",
            "customers",
            "suppliers",
            "sales",
            "purchases",
            "purchaseReturns",
            "payments",
            "supplierPayments",
            "accountAdjustments",
            "expenses",
            "otherIncomes",
            "notes"
        )
        for (col in collectionsToCheck) {
            val snap = storeRef.collection(col).limit(1).get().await()
            if (!snap.isEmpty) return false
        }
        val accountsSnap = storeRef.collection("accounts").get().await()
        val hasCustomAccount = accountsSnap.documents.any { doc ->
            doc.id != "cash_in_hand" || ((doc.getDouble("openingBalance") ?: 0.0) != 0.0)
        }
        return !hasCustomAccount
    }

    suspend fun exportStoreBackup(
        storeId: String,
        photoStorageMode: PhotoStorageMode = PhotoStorageMode.SYNC,
        isSimpleMode: Boolean = false,
        actingUserId: String? = null
    ): StoreBackupPayload = withContext(Dispatchers.IO) {
        val (store, _) = requireStoreOwnerForBackup(storeId, actingUserId)
        val database = db
        val isDemo = (storeId == "demo_store" || database == null)

        val rawProducts: List<Product>
        val customers: List<Customer>
        val suppliers: List<Supplier>
        val sales: List<Sale>
        val purchases: List<Purchase>
        val purchaseReturns: List<PurchaseReturn>
        val payments: List<Payment>
        val supplierPayments: List<SupplierPayment>
        val accounts: List<Account>
        val adjustments: List<AccountAdjustment>
        val expenses: List<Expense>
        val otherIncomes: List<OtherIncome>
        val categories: List<StoreCategory>
        val notes: List<StoreNote>
        val docInvoiceSeq: Long
        val docPaymentSeq: Long
        val basePurchaseSeq: Long
        val baseReturnSeq: Long

        if (isDemo) {
            rawProducts = _demoProducts.value
            customers = _demoCustomers.value
            suppliers = _demoSuppliers.value
            sales = _demoSales.value
            purchases = _demoPurchases.value
            purchaseReturns = _demoPurchaseReturns.value
            payments = _demoPayments.value
            supplierPayments = _demoSupplierPayments.value
            accounts = _demoAccounts.value
            adjustments = _demoAdjustments.value
            expenses = _demoExpenses.value
            otherIncomes = _demoOtherIncomes.value
            categories = _demoCategories.value
            notes = _demoNotes.value
            docInvoiceSeq = demoInvoiceCounter.get()
            docPaymentSeq = demoPaymentCounter.get()
            basePurchaseSeq = demoPurchaseCounter.get()
            baseReturnSeq = demoReturnCounter.get()
        } else {
            val storeRef = database!!.collection("stores").document(storeId)
            rawProducts = storeRef.collection("products").get().await().documents
                .mapNotNull { doc -> doc.toObject(Product::class.java)?.copy(id = doc.id, storeId = storeId) }
                .sortedBy { it.name.lowercase() }
            customers = storeRef.collection("customers").get().await().documents
                .mapNotNull { doc -> doc.toObject(Customer::class.java)?.copy(id = doc.id, storeId = storeId) }
            suppliers = storeRef.collection("suppliers").get().await().documents
                .mapNotNull { doc -> doc.toObject(Supplier::class.java)?.copy(id = doc.id, storeId = storeId) }
            sales = storeRef.collection("sales").get().await().documents
                .mapNotNull { doc -> doc.toObject(Sale::class.java)?.copy(id = doc.id, storeId = storeId) }
            purchases = storeRef.collection("purchases").get().await().documents
                .mapNotNull { doc -> doc.toObject(Purchase::class.java)?.copy(id = doc.id, storeId = storeId) }
            purchaseReturns = storeRef.collection("purchaseReturns").get().await().documents
                .mapNotNull { doc -> doc.toObject(PurchaseReturn::class.java)?.copy(id = doc.id, storeId = storeId) }
            payments = storeRef.collection("payments").get().await().documents
                .mapNotNull { doc -> doc.toObject(Payment::class.java)?.copy(id = doc.id, storeId = storeId) }
            supplierPayments = storeRef.collection("supplierPayments").get().await().documents
                .mapNotNull { doc -> doc.toObject(SupplierPayment::class.java)?.copy(id = doc.id, storeId = storeId) }
            val loadedAccounts = storeRef.collection("accounts").get().await().documents
                .mapNotNull { doc -> doc.toObject(Account::class.java)?.copy(id = doc.id, storeId = storeId) }
            accounts = if (loadedAccounts.none { it.isDefault || it.id == "cash_in_hand" }) {
                listOf(
                    Account(
                        id = "cash_in_hand",
                        storeId = storeId,
                        name = "Cash in Hand",
                        type = "Cash",
                        accountNumber = "",
                        openingBalance = 0.0,
                        isDefault = true,
                        isArchived = false
                    )
                ) + loadedAccounts
            } else {
                loadedAccounts
            }
            adjustments = storeRef.collection("accountAdjustments").get().await().documents
                .mapNotNull { doc -> doc.toObject(AccountAdjustment::class.java)?.copy(id = doc.id, storeId = storeId) }
            expenses = storeRef.collection("expenses").get().await().documents
                .mapNotNull { doc -> doc.toObject(Expense::class.java)?.copy(id = doc.id, storeId = storeId) }
            otherIncomes = storeRef.collection("otherIncomes").get().await().documents
                .mapNotNull { doc -> doc.toObject(OtherIncome::class.java)?.copy(id = doc.id, storeId = storeId) }
            categories = storeRef.collection("categories").get().await().documents
                .mapNotNull { doc -> doc.toObject(StoreCategory::class.java)?.copy(id = doc.id, storeId = storeId) }
            notes = storeRef.collection("notes").get().await().documents
                .mapNotNull { doc -> doc.toObject(StoreNote::class.java)?.copy(id = doc.id, storeId = storeId) }

            val counterSnap = runCatching {
                storeRef.collection("meta").document("counters").get().await()
            }.getOrNull()
            docInvoiceSeq = counterSnap?.getLong("invoiceSeq") ?: 100L
            docPaymentSeq = counterSnap?.getLong("paymentSeq") ?: 0L
            basePurchaseSeq = 0L
            baseReturnSeq = 0L
        }

        val productsWithThumbnails = rawProducts.map { p ->
            val resolvedPhoto = resolveProductPhotoThumbnailForExport(storeId, p)
            if (resolvedPhoto != p.photoUri) p.copy(photoUri = resolvedPhoto) else p
        }

        val maxSaleSeq = sales.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.invoiceNo, "INV-") } ?: 0L
        val maxPaymentSeq = payments.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.paymentNo, "PAY-") } ?: 0L
        val maxPurchaseSeq = purchases.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.purchaseNo, "PUR-") } ?: 0L
        val maxReturnSeq = purchaseReturns.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.returnNo, "RET-") } ?: 0L

        val counters = StoreBackupCounters(
            invoiceSeq = max(docInvoiceSeq, maxSaleSeq),
            paymentSeq = max(docPaymentSeq, maxPaymentSeq),
            purchaseSeq = max(basePurchaseSeq, maxPurchaseSeq),
            returnSeq = max(baseReturnSeq, maxReturnSeq)
        )

        val resolvedShopType = ShopType.fromId(store.shopType) ?: ShopType.GENERAL
        val mergedProductCategories = (store.productCategories + rawProducts.map { it.category })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(50)

        val unitPresets = ShopTypeCatalog.unitPresets(resolvedShopType)
        val allUnits = buildList {
            unitPresets.forEach { preset ->
                add(preset.stockUnit)
                if (preset.buyUnit.isNotBlank()) add(preset.buyUnit)
                preset.sellUnits.keys.forEach { add(it) }
            }
            rawProducts.forEach { prod ->
                add(prod.unit)
                if (prod.buyUnit.isNotBlank()) add(prod.buyUnit)
                prod.sellUnits.keys.forEach { add(it) }
            }
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        val settings = StoreBackupSettings(
            name = store.name,
            phone = store.phone,
            address = store.address,
            shopType = store.shopType,
            language = _currentLanguage.value.code,
            isSimpleMode = isSimpleMode,
            photoStorageMode = photoStorageMode.prefValue
        )

        StoreBackupPayload(
            format = StoreBackupPayload.FORMAT_ID,
            version = StoreBackupPayload.CURRENT_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            store = store.copy(productCategories = mergedProductCategories),
            storeSettings = settings,
            shopType = store.shopType,
            productCategories = mergedProductCategories,
            categoryRecords = categories,
            units = StoreBackupUnits(presets = unitPresets, allUnits = allUnits),
            counters = counters,
            products = productsWithThumbnails,
            customers = customers,
            suppliers = suppliers,
            sales = sales,
            purchases = purchases,
            purchaseReturns = purchaseReturns,
            payments = payments,
            supplierPayments = supplierPayments,
            accounts = accounts,
            accountAdjustments = adjustments,
            expenses = expenses,
            otherIncomes = otherIncomes,
            notes = notes
        )
    }

    private fun normalizeDocId(
        rawId: String,
        fallbackPrefix: String,
        index: Int,
        usedIds: MutableSet<String>
    ): String {
        val validRegex = Regex("^[a-zA-Z0-9_\\-]+$")
        var candidate = rawId.trim()
        if (candidate.isBlank() || candidate.length > 128 || !validRegex.matches(candidate)) {
            val cleaned = candidate.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
            candidate = if (cleaned.isNotBlank()) cleaned else "${fallbackPrefix}_${index + 1}"
        }
        var unique = candidate.take(128)
        var counter = 1
        while (!usedIds.add(unique)) {
            val suffix = "_$counter"
            unique = candidate.take(128 - suffix.length) + suffix
            counter++
        }
        return unique
    }

    private fun safeRestoredTimestamp(ts: Timestamp?): Any {
        if (ts == null) return FieldValue.serverTimestamp()
        val rawMillis = ts.toDate().time
        if (rawMillis <= 0L) return FieldValue.serverTimestamp()
        val safeUpper = System.currentTimeMillis() - 1_000L
        val clamped = min(rawMillis, safeUpper).coerceAtLeast(1_000L)
        return Timestamp(Date(clamped))
    }

    suspend fun importStoreBackup(
        storeId: String,
        rawJson: String,
        mode: BackupImportMode,
        photoStorageMode: PhotoStorageMode = PhotoStorageMode.SYNC,
        isSimpleMode: Boolean = false,
        actingUserId: String? = null
    ): BackupImportSummary = withContext(Dispatchers.IO) {
        val lang = _currentLanguage.value
        val (targetStore, ownerUid) = requireStoreOwnerForBackup(storeId, actingUserId)
        val parsed = StoreBackupCodec.decodeAndValidateJson(rawJson, lang)

        if (mode == BackupImportMode.ADD_TO_EMPTY_STORE && !isStoreEmptyForImport(storeId)) {
            throw IllegalStateException(ShopLocale.get("backup_err_store_not_empty", lang))
        }

        // Step 1: Mandatory automatic safety backup of current store before any write
        val preImportPayload = exportStoreBackup(
            storeId = storeId,
            photoStorageMode = photoStorageMode,
            isSimpleMode = isSimpleMode,
            actingUserId = ownerUid
        )
        val autoBackupFileName = StoreBackupCodec.buildAutoBackupFileName(targetStore.name)
        val autoBackupPath = try {
            val preJson = StoreBackupCodec.encodeToJson(preImportPayload)
            val ctx = appContext
            if (ctx != null) {
                val backupDir = File(ctx.filesDir, "auto_backups").apply { mkdirs() }
                val outFile = File(backupDir, autoBackupFileName)
                outFile.writeText(preJson, Charsets.UTF_8)
                outFile.absolutePath
            } else {
                "memory://$autoBackupFileName"
            }
        } catch (e: Exception) {
            val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "write error"
            throw IllegalStateException(ShopLocale.format("backup_err_auto_backup_failed", lang, detail), e)
        }

        // Step 2: Normalize IDs & build foreign-key remapping tables
        val prodIdMap = mutableMapOf<String, String>()
        val usedProdIds = mutableSetOf<String>()
        val normalizedProducts = parsed.products.mapIndexed { idx, p ->
            val rawId = p.id
            val newId = normalizeDocId(rawId, "prod", idx, usedProdIds)
            if (rawId.isNotBlank()) prodIdMap[rawId] = newId
            p.copy(id = newId, storeId = storeId)
        }
        val productsById = normalizedProducts.associateBy { it.id }

        val custIdMap = mutableMapOf<String, String>()
        val usedCustIds = mutableSetOf<String>()
        val normalizedCustomers = parsed.customers.mapIndexed { idx, c ->
            val rawId = c.id
            val newId = normalizeDocId(rawId, "cust", idx, usedCustIds)
            if (rawId.isNotBlank()) custIdMap[rawId] = newId
            c.copy(id = newId, storeId = storeId)
        }

        val supIdMap = mutableMapOf<String, String>()
        val usedSupIds = mutableSetOf<String>()
        val normalizedSuppliers = parsed.suppliers.mapIndexed { idx, s ->
            val rawId = s.id
            val newId = normalizeDocId(rawId, "sup", idx, usedSupIds)
            if (rawId.isNotBlank()) supIdMap[rawId] = newId
            s.copy(id = newId, storeId = storeId)
        }

        val accIdMap = mutableMapOf<String, String>()
        val usedAccIds = mutableSetOf<String>()
        val normalizedAccounts = parsed.accounts.mapIndexed { idx, a ->
            val rawId = a.id
            val newId = if (a.id == "cash_in_hand") {
                usedAccIds.add("cash_in_hand")
                "cash_in_hand"
            } else {
                normalizeDocId(rawId, "acc", idx, usedAccIds)
            }
            if (rawId.isNotBlank()) accIdMap[rawId] = newId
            a.copy(id = newId, storeId = storeId)
        }

        val usedSaleIds = mutableSetOf<String>()
        val normalizedSales = parsed.sales.mapIndexed { idx, rawSale ->
            val newId = normalizeDocId(rawSale.id, "sale", idx, usedSaleIds)
            val mappedCustId = rawSale.customerId.takeIf { it.isNotBlank() }?.let { custIdMap[it] ?: it } ?: ""
            val mappedAccId = rawSale.accountId.takeIf { it.isNotBlank() }?.let { accIdMap[it] ?: it } ?: ""
            val remappedItems = rawSale.items.take(200).map { it.copy(productId = prodIdMap[it.productId] ?: it.productId) }
            val recomputed = CalculationEngine.recomputeSaleForImport(
                rawSale.copy(
                    id = newId,
                    storeId = storeId,
                    customerId = mappedCustId,
                    accountId = mappedAccId,
                    items = remappedItems
                ),
                productsById
            )
            recomputed
        }

        val purIdMap = mutableMapOf<String, String>()
        val usedPurIds = mutableSetOf<String>()
        val normalizedPurchases = parsed.purchases.mapIndexed { idx, rawPur ->
            val rawId = rawPur.id
            val newId = normalizeDocId(rawId, "pur", idx, usedPurIds)
            if (rawId.isNotBlank()) purIdMap[rawId] = newId
            val mappedSupId = rawPur.supplierId.takeIf { it.isNotBlank() }?.let { supIdMap[it] ?: it } ?: ""
            val mappedAccId = rawPur.accountId.takeIf { it.isNotBlank() }?.let { accIdMap[it] ?: it } ?: "cash_in_hand"
            val remappedItems = rawPur.items.take(200).map { it.copy(productId = prodIdMap[it.productId] ?: it.productId) }
            CalculationEngine.recomputePurchaseForImport(
                rawPur.copy(
                    id = newId,
                    storeId = storeId,
                    supplierId = mappedSupId,
                    accountId = mappedAccId,
                    items = remappedItems
                )
            )
        }

        val usedReturnIds = mutableSetOf<String>()
        val normalizedReturns = parsed.purchaseReturns.mapIndexed { idx, ret ->
            val newId = normalizeDocId(ret.id, "ret", idx, usedReturnIds)
            ret.copy(
                id = newId,
                storeId = storeId,
                purchaseId = ret.purchaseId.takeIf { it.isNotBlank() }?.let { purIdMap[it] ?: it } ?: "",
                supplierId = ret.supplierId.takeIf { it.isNotBlank() }?.let { supIdMap[it] ?: it } ?: ""
            )
        }

        val usedPaymentIds = mutableSetOf<String>()
        val normalizedPayments = parsed.payments.mapIndexed { idx, pay ->
            val newId = normalizeDocId(pay.id, "pay", idx, usedPaymentIds)
            pay.copy(
                id = newId,
                storeId = storeId,
                customerId = pay.customerId.takeIf { it.isNotBlank() }?.let { custIdMap[it] ?: it } ?: "",
                accountId = accIdMap[pay.accountId] ?: pay.accountId.ifBlank { "cash_in_hand" }
            )
        }

        val usedSupPaymentIds = mutableSetOf<String>()
        val normalizedSupPayments = parsed.supplierPayments.mapIndexed { idx, sp ->
            val newId = normalizeDocId(sp.id, "sp", idx, usedSupPaymentIds)
            sp.copy(
                id = newId,
                storeId = storeId,
                supplierId = supIdMap[sp.supplierId] ?: sp.supplierId,
                accountId = accIdMap[sp.accountId] ?: sp.accountId.ifBlank { "cash_in_hand" }
            )
        }

        val usedAdjIds = mutableSetOf<String>()
        val normalizedAdjustments = parsed.accountAdjustments.mapIndexed { idx, adj ->
            val newId = normalizeDocId(adj.id, "adj", idx, usedAdjIds)
            adj.copy(
                id = newId,
                storeId = storeId,
                accountId = accIdMap[adj.accountId] ?: adj.accountId.ifBlank { "cash_in_hand" }
            )
        }

        val usedExpIds = mutableSetOf<String>()
        val normalizedExpenses = parsed.expenses.mapIndexed { idx, exp ->
            val newId = normalizeDocId(exp.id, "exp", idx, usedExpIds)
            exp.copy(
                id = newId,
                storeId = storeId,
                accountId = accIdMap[exp.accountId] ?: exp.accountId.ifBlank { "cash_in_hand" }
            )
        }

        val usedIncIds = mutableSetOf<String>()
        val normalizedIncomes = parsed.otherIncomes.mapIndexed { idx, inc ->
            val newId = normalizeDocId(inc.id, "inc", idx, usedIncIds)
            inc.copy(
                id = newId,
                storeId = storeId,
                accountId = accIdMap[inc.accountId] ?: inc.accountId.ifBlank { "cash_in_hand" }
            )
        }

        val usedCatIds = mutableSetOf<String>()
        val normalizedCategories = parsed.categoryRecords.mapIndexed { idx, cat ->
            val newId = normalizeDocId(cat.id, "cat", idx, usedCatIds)
            cat.copy(id = newId, storeId = storeId)
        }

        val usedNoteIds = mutableSetOf<String>()
        val normalizedNotes = parsed.notes.mapIndexed { idx, note ->
            val newId = normalizeDocId(note.id, "note", idx, usedNoteIds)
            note.copy(id = newId, storeId = storeId)
        }

        val mergedProductCats = if (mode == BackupImportMode.ADD_TO_EMPTY_STORE) {
            (parsed.productCategories + normalizedProducts.map { it.category })
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(50)
        } else {
            (targetStore.productCategories + parsed.productCategories + normalizedProducts.map { it.category })
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(50)
        }

        val resolvedShopType = when {
            mode == BackupImportMode.ADD_TO_EMPTY_STORE && parsed.shopType.isNotBlank() -> parsed.shopType.take(40)
            targetStore.shopType.isBlank() && parsed.shopType.isNotBlank() -> parsed.shopType.take(40)
            else -> targetStore.shopType
        }
        val resolvedName = when {
            mode == BackupImportMode.ADD_TO_EMPTY_STORE && parsed.store.name.isNotBlank() -> parsed.store.name.trim().take(160)
            targetStore.name.isBlank() && parsed.store.name.isNotBlank() -> parsed.store.name.trim().take(160)
            else -> targetStore.name
        }
        val resolvedPhone = when {
            mode == BackupImportMode.ADD_TO_EMPTY_STORE && parsed.store.phone.isNotBlank() -> parsed.store.phone.trim().take(64)
            targetStore.phone.isBlank() && parsed.store.phone.isNotBlank() -> parsed.store.phone.trim().take(64)
            else -> targetStore.phone
        }
        val resolvedAddress = when {
            mode == BackupImportMode.ADD_TO_EMPTY_STORE && parsed.store.address.isNotBlank() -> parsed.store.address.trim().take(300)
            targetStore.address.isBlank() && parsed.store.address.isNotBlank() -> parsed.store.address.trim().take(300)
            else -> targetStore.address
        }

        val maxSaleSeq = normalizedSales.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.invoiceNo, "INV-") } ?: 0L
        val maxPaymentSeq = normalizedPayments.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.paymentNo, "PAY-") } ?: 0L
        val maxPurchaseSeq = normalizedPurchases.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.purchaseNo, "PUR-") } ?: 0L
        val maxReturnSeq = normalizedReturns.maxOfOrNull { CalculationEngine.extractSequenceNumber(it.returnNo, "RET-") } ?: 0L

        val finalInvoiceSeq = maxOf(preImportPayload.counters.invoiceSeq, parsed.counters.invoiceSeq, maxSaleSeq, 100L)
        val finalPaymentSeq = maxOf(preImportPayload.counters.paymentSeq, parsed.counters.paymentSeq, maxPaymentSeq)
        val finalPurchaseSeq = maxOf(preImportPayload.counters.purchaseSeq, parsed.counters.purchaseSeq, maxPurchaseSeq)
        val finalReturnSeq = maxOf(preImportPayload.counters.returnSeq, parsed.counters.returnSeq, maxReturnSeq)

        val database = db
        if (storeId == "demo_store" || database == null) {
            fun <T> applyList(
                existing: List<T>,
                incoming: List<T>,
                idSelector: (T) -> String
            ): Triple<List<T>, Int, Int> {
                val existingIds = existing.mapTo(mutableSetOf()) { idSelector(it) }
                val toAdd = mutableListOf<T>()
                var skipped = 0
                for (item in incoming) {
                    if (idSelector(item) in existingIds) {
                        skipped++
                    } else {
                        toAdd.add(item)
                    }
                }
                val merged = if (mode == BackupImportMode.ADD_TO_EMPTY_STORE) incoming else (existing + toAdd)
                val added = if (mode == BackupImportMode.ADD_TO_EMPTY_STORE) incoming.size else toAdd.size
                return Triple(merged, added, skipped)
            }

            val (nextProds, prodAdded, prodSkipped) = applyList(_demoProducts.value, normalizedProducts) { it.id }
            val (nextCusts, custAdded, custSkipped) = applyList(_demoCustomers.value, normalizedCustomers) { it.id }
            val (nextSups, supAdded, supSkipped) = applyList(_demoSuppliers.value, normalizedSuppliers) { it.id }
            val (nextSales, saleAdded, saleSkipped) = applyList(_demoSales.value, normalizedSales) { it.id }
            val (nextPurs, purAdded, purSkipped) = applyList(_demoPurchases.value, normalizedPurchases) { it.id }
            val (nextRets, retAdded, retSkipped) = applyList(_demoPurchaseReturns.value, normalizedReturns) { it.id }
            val (nextPays, payAdded, paySkipped) = applyList(_demoPayments.value, normalizedPayments) { it.id }
            val (nextSupPays, spAdded, spSkipped) = applyList(_demoSupplierPayments.value, normalizedSupPayments) { it.id }

            // Accounts in empty demo store might have default cash_in_hand with 0 opening balance
            val existingAccIds = _demoAccounts.value.mapTo(mutableSetOf()) { it.id }
            var accAdded = 0
            var accSkipped = 0
            val nextAccounts = if (mode == BackupImportMode.ADD_TO_EMPTY_STORE) {
                accAdded = normalizedAccounts.size
                normalizedAccounts.ifEmpty { _demoAccounts.value }
            } else {
                val additions = normalizedAccounts.filter { acc ->
                    if (acc.id in existingAccIds) {
                        accSkipped++
                        false
                    } else {
                        accAdded++
                        true
                    }
                }
                _demoAccounts.value + additions
            }

            val (nextAdjs, adjAdded, adjSkipped) = applyList(_demoAdjustments.value, normalizedAdjustments) { it.id }
            val (nextExps, expAdded, expSkipped) = applyList(_demoExpenses.value, normalizedExpenses) { it.id }
            val (nextIncs, incAdded, incSkipped) = applyList(_demoOtherIncomes.value, normalizedIncomes) { it.id }
            val (nextCats, catAdded, catSkipped) = applyList(_demoCategories.value, normalizedCategories) { it.id }
            val (nextNotes, noteAdded, noteSkipped) = applyList(_demoNotes.value, normalizedNotes) { it.id }

            _demoStore.value = targetStore.copy(
                name = resolvedName,
                phone = resolvedPhone,
                address = resolvedAddress,
                shopType = resolvedShopType,
                productCategories = mergedProductCats,
                updatedAt = Timestamp.now()
            )
            _demoProducts.value = nextProds.sortedBy { it.name.lowercase() }
            _demoCustomers.value = nextCusts
            _demoSuppliers.value = nextSups
            _demoSales.value = nextSales
            _demoPurchases.value = nextPurs
            _demoPurchaseReturns.value = nextRets
            _demoPayments.value = nextPays
            _demoSupplierPayments.value = nextSupPays
            _demoAccounts.value = nextAccounts
            _demoAdjustments.value = nextAdjs
            _demoExpenses.value = nextExps
            _demoOtherIncomes.value = nextIncs
            _demoCategories.value = nextCats
            _demoNotes.value = nextNotes

            demoInvoiceCounter.set(finalInvoiceSeq)
            demoPaymentCounter.set(finalPaymentSeq)
            demoPurchaseCounter.set(finalPurchaseSeq)
            demoReturnCounter.set(finalReturnSeq)

            return@withContext BackupImportSummary(
                mode = mode,
                backupVersion = parsed.version,
                autoBackupFileName = autoBackupFileName,
                autoBackupPath = autoBackupPath,
                productsAdded = prodAdded,
                productsSkipped = prodSkipped,
                customersAdded = custAdded,
                customersSkipped = custSkipped,
                suppliersAdded = supAdded,
                suppliersSkipped = supSkipped,
                salesAdded = saleAdded,
                salesSkipped = saleSkipped,
                purchasesAdded = purAdded,
                purchasesSkipped = purSkipped,
                purchaseReturnsAdded = retAdded,
                purchaseReturnsSkipped = retSkipped,
                paymentsAdded = payAdded,
                paymentsSkipped = paySkipped,
                supplierPaymentsAdded = spAdded,
                supplierPaymentsSkipped = spSkipped,
                accountsAdded = accAdded,
                accountsSkipped = accSkipped,
                adjustmentsAdded = adjAdded,
                adjustmentsSkipped = adjSkipped,
                expensesAdded = expAdded,
                expensesSkipped = expSkipped,
                incomeAdded = incAdded,
                incomeSkipped = incSkipped,
                notesAdded = noteAdded,
                notesSkipped = noteSkipped,
                categoriesAdded = catAdded,
                categoriesSkipped = catSkipped,
                restoredSettings = parsed.storeSettings
            )
        }

        // Firestore write path
        val storeRef = database.collection("stores").document(storeId)
        val batchWrites = mutableListOf<BackupBatchWrite>()

        suspend fun existingDocIds(colName: String): Set<String> {
            return storeRef.collection(colName).get().await().documents.mapTo(mutableSetOf()) { it.id }
        }

        val existingProdIds = existingDocIds("products")
        val existingCustIds = existingDocIds("customers")
        val existingSupIds = existingDocIds("suppliers")
        val existingSaleIds = existingDocIds("sales")
        val existingPurIds = existingDocIds("purchases")
        val existingRetIds = existingDocIds("purchaseReturns")
        val existingPayIds = existingDocIds("payments")
        val existingSpIds = existingDocIds("supplierPayments")
        val existingAccIds = existingDocIds("accounts")
        val existingAdjIds = existingDocIds("accountAdjustments")
        val existingExpIds = existingDocIds("expenses")
        val existingIncIds = existingDocIds("otherIncomes")
        val existingCatIds = existingDocIds("categories")
        val existingNoteIds = existingDocIds("notes")

        var prodAdded = 0; var prodSkipped = 0
        for (p in normalizedProducts) {
            if (p.id in existingProdIds) {
                prodSkipped++
                continue
            }
            val stockUnit = p.unit.trim().ifBlank { "pcs" }.take(50)
            val cleanBuyUnit = p.buyUnit.trim().ifBlank { stockUnit }.take(50)
            val cleanBuyConv = CalculationEngine.effectiveBuyConversion(p.copy(unit = stockUnit, buyUnit = cleanBuyUnit))
            val cleanSellUnits = p.sellUnits.filter { (k, v) ->
                k.isNotBlank() && !k.trim().equals(stockUnit, ignoreCase = true) && v > 0.0
            }.mapKeys { it.key.trim() }
            val payload = mapOf(
                "storeId" to storeId,
                "name" to p.name.trim().take(200),
                "nameBn" to p.nameBn.trim().take(200),
                "barcode" to p.barcode.trim().take(128),
                "category" to p.category.trim().ifBlank { "General" }.take(100),
                "unit" to stockUnit,
                "buyUnit" to cleanBuyUnit,
                "buyConversion" to cleanBuyConv,
                "sellUnits" to cleanSellUnits,
                "buyPrice" to p.buyPrice.coerceAtLeast(0.0),
                "sellPrice" to p.sellPrice.coerceAtLeast(0.0),
                "wholesalePrice" to p.wholesalePrice.coerceAtLeast(0.0),
                "mrp" to p.mrp.coerceAtLeast(0.0),
                "itemLocation" to p.itemLocation.trim().take(120),
                "partyRate" to p.partyRate.coerceAtLeast(0.0),
                "stock" to CalculationEngine.roundStock(p.stock.coerceAtLeast(0.0)),
                "lowStockLimit" to CalculationEngine.roundStock(p.lowStockLimit.coerceAtLeast(0.0)),
                "photoUri" to p.photoUri.take(99_500),
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(p.createdAt),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("products").document(p.id), payload))
            prodAdded++
        }

        var custAdded = 0; var custSkipped = 0
        for (c in normalizedCustomers) {
            if (c.id in existingCustIds) {
                custSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "name" to c.name.trim().take(160),
                "mobile" to c.mobile.trim().take(64),
                "address" to c.address.trim().take(300),
                "notes" to c.notes.trim().take(500),
                "currentBalance" to CalculationEngine.roundMoney(c.currentBalance.coerceAtLeast(0.0)),
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(c.createdAt),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("customers").document(c.id), payload))
            custAdded++
        }

        var supAdded = 0; var supSkipped = 0
        for (s in normalizedSuppliers) {
            if (s.id in existingSupIds) {
                supSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "name" to s.name.trim().take(160),
                "mobile" to s.mobile.trim().take(64),
                "company" to s.company.trim().take(160),
                "address" to s.address.trim().take(300),
                "balance" to CalculationEngine.roundMoney(s.balance.coerceAtLeast(0.0)),
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(s.createdAt),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("suppliers").document(s.id), payload))
            supAdded++
        }

        var accAdded = 0; var accSkipped = 0
        for (a in normalizedAccounts) {
            val accRef = storeRef.collection("accounts").document(a.id)
            if (a.id in existingAccIds) {
                if (mode == BackupImportMode.ADD_TO_EMPTY_STORE && a.id == "cash_in_hand") {
                    val updatePayload = mapOf(
                        "name" to a.name.trim().take(120),
                        "type" to a.type,
                        "accountNumber" to a.accountNumber.trim().take(100),
                        "openingBalance" to a.openingBalance,
                        "isArchived" to a.isArchived,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batchWrites.add(BackupBatchWrite(accRef, updatePayload, isUpdate = true))
                    accAdded++
                } else {
                    accSkipped++
                }
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "name" to a.name.trim().take(120),
                "type" to a.type,
                "accountNumber" to a.accountNumber.trim().take(100),
                "openingBalance" to a.openingBalance,
                "isDefault" to (a.isDefault || a.id == "cash_in_hand"),
                "isArchived" to a.isArchived,
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(a.createdAt),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batchWrites.add(BackupBatchWrite(accRef, payload))
            accAdded++
        }

        var saleAdded = 0; var saleSkipped = 0
        for (sale in normalizedSales) {
            if (sale.id in existingSaleIds) {
                saleSkipped++
                continue
            }
            val itemMaps = sale.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "qty" to item.qty,
                    "unit" to item.unit,
                    "stockQty" to item.stockQty,
                    "buyPrice" to item.buyPrice,
                    "sellPrice" to item.sellPrice,
                    "discount" to item.discount,
                    "lineTotal" to item.lineTotal,
                    "profit" to item.profit
                )
            }
            val payload = mapOf(
                "storeId" to storeId,
                "invoiceNo" to sale.invoiceNo.trim().take(64),
                "items" to itemMaps,
                "totalItems" to sale.totalItems,
                "subtotal" to sale.subtotal,
                "discount" to sale.discount,
                "grandTotal" to sale.grandTotal,
                "profit" to sale.profit,
                "paymentType" to sale.paymentType,
                "customerId" to sale.customerId.take(128),
                "customerName" to sale.customerName.trim().take(160),
                "customerPhone" to sale.customerPhone.trim().take(64),
                "accountId" to sale.accountId.take(128),
                "accountName" to sale.accountName.trim().take(120),
                "notes" to sale.notes.trim().take(500),
                "soldBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(sale.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("sales").document(sale.id), payload))
            saleAdded++
        }

        var purAdded = 0; var purSkipped = 0
        for (pur in normalizedPurchases) {
            if (pur.id in existingPurIds) {
                purSkipped++
                continue
            }
            val itemMaps = pur.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "qty" to item.qty,
                    "unit" to item.unit,
                    "stockQty" to item.stockQty,
                    "conversionFactor" to item.conversionFactor,
                    "costPerStockUnit" to item.costPerStockUnit,
                    "buyPrice" to item.buyPrice,
                    "lineTotal" to item.lineTotal
                )
            }
            val payload = mapOf(
                "storeId" to storeId,
                "purchaseNo" to pur.purchaseNo.trim().take(64),
                "supplierId" to pur.supplierId.take(128),
                "supplierName" to pur.supplierName.trim().take(160),
                "supplierMobile" to pur.supplierMobile.trim().take(64),
                "items" to itemMaps,
                "totalItems" to pur.totalItems,
                "totalAmount" to pur.totalAmount,
                "paidAmount" to pur.paidAmount,
                "dueAmount" to pur.dueAmount,
                "paymentType" to pur.paymentType.take(64),
                "accountId" to pur.accountId.ifBlank { "cash_in_hand" }.take(128),
                "accountName" to pur.accountName.ifBlank { "Cash in Hand" }.take(120),
                "note" to pur.note.trim().take(500),
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(pur.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("purchases").document(pur.id), payload))
            purAdded++
        }

        var retAdded = 0; var retSkipped = 0
        for (ret in normalizedReturns) {
            if (ret.id in existingRetIds) {
                retSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "returnNo" to ret.returnNo.trim().take(64),
                "purchaseId" to ret.purchaseId.take(128),
                "supplierId" to ret.supplierId.take(128),
                "supplierName" to ret.supplierName.trim().take(160),
                "amount" to ret.amount,
                "reason" to ret.reason.trim().take(500),
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(ret.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("purchaseReturns").document(ret.id), payload))
            retAdded++
        }

        var payAdded = 0; var paySkipped = 0
        for (pay in normalizedPayments) {
            if (pay.id in existingPayIds) {
                paySkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "paymentNo" to pay.paymentNo.trim().take(64),
                "customerId" to pay.customerId.take(128),
                "customerName" to pay.customerName.trim().take(160),
                "customerMobile" to pay.customerMobile.trim().take(64),
                "amount" to pay.amount,
                "paymentMethod" to pay.paymentMethod.ifBlank { "cash" }.take(64),
                "accountId" to pay.accountId.ifBlank { "cash_in_hand" }.take(128),
                "accountName" to pay.accountName.ifBlank { "Cash in Hand" }.take(120),
                "paymentDate" to pay.paymentDate,
                "note" to pay.note.trim().take(500),
                "receivedBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(pay.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("payments").document(pay.id), payload))
            payAdded++
        }

        var spAdded = 0; var spSkipped = 0
        for (sp in normalizedSupPayments) {
            if (sp.id in existingSpIds) {
                spSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "paymentNo" to sp.paymentNo.trim().take(64),
                "supplierId" to sp.supplierId.take(128),
                "supplierName" to sp.supplierName.trim().take(160),
                "amount" to sp.amount,
                "accountId" to sp.accountId.ifBlank { "cash_in_hand" }.take(128),
                "accountName" to sp.accountName.ifBlank { "Cash in Hand" }.take(120),
                "paymentDate" to sp.paymentDate,
                "note" to sp.note.trim().take(500),
                "paidBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(sp.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("supplierPayments").document(sp.id), payload))
            spAdded++
        }

        var adjAdded = 0; var adjSkipped = 0
        for (adj in normalizedAdjustments) {
            if (adj.id in existingAdjIds) {
                adjSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "accountId" to adj.accountId.take(128),
                "accountName" to adj.accountName.trim().take(120),
                "oldBalance" to adj.oldBalance,
                "newBalance" to adj.newBalance,
                "difference" to adj.difference,
                "note" to adj.note.trim().ifBlank { "Imported adjustment" }.take(500),
                "adjustedBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(adj.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("accountAdjustments").document(adj.id), payload))
            adjAdded++
        }

        var expAdded = 0; var expSkipped = 0
        for (exp in normalizedExpenses) {
            if (exp.id in existingExpIds) {
                expSkipped++
                continue
            }
            val payload = buildMap<String, Any> {
                put("storeId", storeId)
                put("amount", exp.amount)
                put("category", exp.category.trim().take(120))
                put("accountId", exp.accountId.ifBlank { "cash_in_hand" }.take(128))
                put("accountName", exp.accountName.ifBlank { "Cash in Hand" }.take(120))
                put("date", exp.date)
                put("note", exp.note.trim().take(500))
                put("receiptUri", exp.receiptUri.take(1000))
                put("createdBy", ownerUid)
                put("isDeleted", exp.isDeleted)
                if (exp.isDeleted) {
                    put("deletedNote", exp.deletedNote.trim().take(500))
                    put("deletedBy", exp.deletedBy.ifBlank { ownerUid }.take(128))
                    if (exp.deletedAt != null) {
                        put("deletedAt", safeRestoredTimestamp(exp.deletedAt))
                    }
                }
                put("createdAt", safeRestoredTimestamp(exp.createdAt))
            }
            batchWrites.add(BackupBatchWrite(storeRef.collection("expenses").document(exp.id), payload))
            expAdded++
        }

        var incAdded = 0; var incSkipped = 0
        for (inc in normalizedIncomes) {
            if (inc.id in existingIncIds) {
                incSkipped++
                continue
            }
            val payload = buildMap<String, Any> {
                put("storeId", storeId)
                put("amount", inc.amount)
                put("category", inc.category.trim().take(120))
                put("accountId", inc.accountId.ifBlank { "cash_in_hand" }.take(128))
                put("accountName", inc.accountName.ifBlank { "Cash in Hand" }.take(120))
                put("date", inc.date)
                put("note", inc.note.trim().take(500))
                put("createdBy", ownerUid)
                put("isDeleted", inc.isDeleted)
                if (inc.isDeleted) {
                    put("deletedNote", inc.deletedNote.trim().take(500))
                    put("deletedBy", inc.deletedBy.ifBlank { ownerUid }.take(128))
                    if (inc.deletedAt != null) {
                        put("deletedAt", safeRestoredTimestamp(inc.deletedAt))
                    }
                }
                put("createdAt", safeRestoredTimestamp(inc.createdAt))
            }
            batchWrites.add(BackupBatchWrite(storeRef.collection("otherIncomes").document(inc.id), payload))
            incAdded++
        }

        var catAdded = 0; var catSkipped = 0
        for (cat in normalizedCategories) {
            if (cat.id in existingCatIds) {
                catSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "type" to cat.type,
                "name" to cat.name.trim().take(120),
                "isArchived" to cat.isArchived,
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(cat.createdAt)
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("categories").document(cat.id), payload))
            catAdded++
        }

        var noteAdded = 0; var noteSkipped = 0
        for (note in normalizedNotes) {
            if (note.id in existingNoteIds) {
                noteSkipped++
                continue
            }
            val payload = mapOf(
                "storeId" to storeId,
                "title" to note.title.trim().take(200),
                "text" to note.text.take(5000),
                "date" to note.date,
                "createdBy" to ownerUid,
                "createdAt" to safeRestoredTimestamp(note.createdAt),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batchWrites.add(BackupBatchWrite(storeRef.collection("notes").document(note.id), payload))
            noteAdded++
        }

        // Update store metadata (only owner-allowed update fields) & counters
        val storeMetaUpdate = mapOf(
            "name" to resolvedName,
            "phone" to resolvedPhone,
            "address" to resolvedAddress,
            "shopType" to resolvedShopType,
            "productCategories" to mergedProductCats,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        batchWrites.add(BackupBatchWrite(storeRef, storeMetaUpdate, isUpdate = true))

        val counterRef = storeRef.collection("meta").document("counters")
        val counterSnap = runCatching { counterRef.get().await() }.getOrNull()
        val counterPayload = mapOf(
            "invoiceSeq" to finalInvoiceSeq,
            "paymentSeq" to finalPaymentSeq
        )
        batchWrites.add(
            BackupBatchWrite(
                ref = counterRef,
                data = counterPayload,
                isUpdate = (counterSnap != null && counterSnap.exists())
            )
        )

        // Execute in chunks of 400 ops (safely below Firestore's 500-op WriteBatch cap)
        for (chunk in batchWrites.chunked(400)) {
            val batch = database.batch()
            for (op in chunk) {
                if (op.isUpdate) {
                    batch.update(op.ref, op.data)
                } else {
                    batch.set(op.ref, op.data)
                }
            }
            batch.commit().await()
        }

        BackupImportSummary(
            mode = mode,
            backupVersion = parsed.version,
            autoBackupFileName = autoBackupFileName,
            autoBackupPath = autoBackupPath,
            productsAdded = prodAdded,
            productsSkipped = prodSkipped,
            customersAdded = custAdded,
            customersSkipped = custSkipped,
            suppliersAdded = supAdded,
            suppliersSkipped = supSkipped,
            salesAdded = saleAdded,
            salesSkipped = saleSkipped,
            purchasesAdded = purAdded,
            purchasesSkipped = purSkipped,
            purchaseReturnsAdded = retAdded,
            purchaseReturnsSkipped = retSkipped,
            paymentsAdded = payAdded,
            paymentsSkipped = paySkipped,
            supplierPaymentsAdded = spAdded,
            supplierPaymentsSkipped = spSkipped,
            accountsAdded = accAdded,
            accountsSkipped = accSkipped,
            adjustmentsAdded = adjAdded,
            adjustmentsSkipped = adjSkipped,
            expensesAdded = expAdded,
            expensesSkipped = expSkipped,
            incomeAdded = incAdded,
            incomeSkipped = incSkipped,
            notesAdded = noteAdded,
            notesSkipped = noteSkipped,
            categoriesAdded = catAdded,
            categoriesSkipped = catSkipped,
            restoredSettings = parsed.storeSettings
        )
    }

    // -------------------------------------------------------------
    // BUSINESS STRATEGY & OWNER-ONLY PRIVATE GEMINI KEY
    // -------------------------------------------------------------

    fun observeAiKeySummary(
        storeId: String,
        isOwner: Boolean,
        isFatherMode: Boolean
    ): Flow<AiKeyUiSummary> {
        val database = db
        if (!isOwner || isFatherMode || storeId.isBlank() || storeId == "demo_store" || database == null) {
            return flowOf(AiKeyUiSummary(hasSavedKey = false, keyLast4 = ""))
        }
        return database.collection("stores").document(storeId)
            .collection("private").document("aiKey")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) {
                    AiKeyUiSummary(hasSavedKey = false, keyLast4 = "")
                } else {
                    val last4 = (snap.getString("keyLast4") ?: "").takeLast(4)
                    val raw = snap.getString("apiKey") ?: ""
                    val hasKey = raw.length >= 10 && last4.length == 4
                    AiKeyUiSummary(hasSavedKey = hasKey, keyLast4 = if (hasKey) last4 else "")
                }
            }
            .catch {
                emit(AiKeyUiSummary(hasSavedKey = false, keyLast4 = ""))
            }
    }

    suspend fun getPrivateAiKeyOnce(storeId: String): String? {
        if (storeId.isBlank() || storeId == "demo_store") return null
        val database = db ?: return null
        val snap = runCatching {
            database.collection("stores").document(storeId)
                .collection("private").document("aiKey")
                .get()
                .await()
        }.getOrNull() ?: return null
        if (!snap.exists()) return null
        return snap.getString("apiKey")?.trim()?.takeIf { it.length >= 10 }
    }

    suspend fun savePrivateAiKey(
        storeId: String,
        rawApiKey: String,
        actingUserId: String? = null
    ): AiKeyUiSummary {
        val database = db
        if (storeId == "demo_store" || database == null) {
            throw IllegalStateException("Cloud AI key management is unavailable in local demo mode.")
        }
        val cleanKey = rawApiKey.trim()
        require(cleanKey.length in 10..256) { "Invalid API key length" }
        val last4 = cleanKey.takeLast(4)
        val uid = actingUserId?.ifBlank { null } ?: requireUserId()
        val payload = mapOf(
            "storeId" to storeId,
            "apiKey" to cleanKey,
            "keyLast4" to last4,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        database.collection("stores").document(storeId)
            .collection("private").document("aiKey")
            .set(payload)
            .await()
        return AiKeyUiSummary(hasSavedKey = true, keyLast4 = last4)
    }

    suspend fun removePrivateAiKey(storeId: String) {
        if (storeId.isBlank() || storeId == "demo_store") {
            _demoPrivateAiKey.value = null
            return
        }
        val database = db ?: return
        database.collection("stores").document(storeId)
            .collection("private").document("aiKey")
            .delete()
            .await()
    }

    suspend fun testGeminiApiKey(rawApiKey: String): Result<Unit> {
        if (db == null) {
            return Result.failure(IllegalStateException("Gemini API key check requires an active online store session."))
        }
        return strategyAiService.testApiKey(rawApiKey.trim())
    }

    suspend fun generateStrategyWithAi(
        apiKey: String,
        storeId: String,
        facts: BusinessStrategyFacts,
        generatedAtMs: Long,
        refreshCountToday: Int,
        updatedBy: String
    ): Result<SavedBusinessStrategy> {
        if (storeId == "demo_store" || db == null) {
            return Result.failure(IllegalStateException("AI strategy generation is unavailable in local demo mode."))
        }
        return strategyAiService.generateBanglaStrategy(
            apiKey = apiKey,
            storeId = storeId,
            facts = facts,
            generatedAtMs = generatedAtMs,
            refreshCountToday = refreshCountToday,
            updatedBy = updatedBy
        )
    }

    fun observeSavedBusinessStrategy(storeId: String): Flow<SavedBusinessStrategy?> {
        if (storeId.isBlank()) return flowOf(null)
        val database = db
        if (storeId == "demo_store" || database == null) {
            return _demoBusinessStrategy.asStateFlow()
        }
        return database.collection("stores").document(storeId)
            .collection("strategy").document("today")
            .snapshots()
            .map { snap -> parseStrategySnapshot(storeId, snap) }
            .catch { emit(null) }
    }

    private fun parseStrategySnapshot(storeId: String, snap: DocumentSnapshot): SavedBusinessStrategy? {
        if (!snap.exists()) return null
        val diagnosis = snap.getString("diagnosis")?.trim().orEmpty()
        if (diagnosis.isBlank()) return null
        val focusLine = snap.getString("focusLine")?.trim().orEmpty()
        val dateKey = snap.getString("dateKey")?.trim().orEmpty()
        val generatedAtMs = snap.getLong("generatedAtMs") ?: 0L
        val refreshCountToday = (snap.getLong("refreshCountToday") ?: 0L).toInt()
        val source = snap.getString("source")?.trim().orEmpty().ifBlank { "fixed" }
        val updatedBy = snap.getString("updatedBy")?.trim().orEmpty()
        val rawActions = snap.get("actions") as? List<*>
        val parsedActions = rawActions.orEmpty().mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            val text = (m["text"] as? String)?.trim().orEmpty()
            if (text.isBlank()) return@mapNotNull null
            val targetWire = StrategyActionTarget.fromWireKey((m["target"] as? String) ?: "STOCK").wireKey
            val buttonLabel = (m["buttonLabel"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: BusinessStrategyBanglaBuilder.actionButtonLabel(StrategyActionTarget.fromWireKey(targetWire))
            StrategyActionAdvice(
                text = text,
                target = targetWire,
                buttonLabel = buttonLabel
            )
        }
        if (parsedActions.isEmpty()) return null
        return SavedBusinessStrategy(
            id = snap.id,
            storeId = snap.getString("storeId") ?: storeId,
            dateKey = dateKey,
            diagnosis = diagnosis,
            focusLine = focusLine,
            actions = parsedActions,
            generatedAtMs = generatedAtMs,
            refreshCountToday = refreshCountToday,
            source = source,
            updatedBy = updatedBy,
            updatedAt = snap.getTimestamp("updatedAt")
        )
    }

    suspend fun saveBusinessStrategy(
        storeId: String,
        strategy: SavedBusinessStrategy,
        actingUserId: String? = null
    ) {
        if (storeId.isBlank()) return
        val database = db
        if (storeId == "demo_store" || database == null) {
            val uid = actingUserId?.ifBlank { null } ?: "demo_user"
            val sanitized = strategy.copy(
                id = "today",
                storeId = storeId,
                refreshCountToday = strategy.refreshCountToday.coerceIn(0, 5),
                updatedBy = uid,
                updatedAt = Timestamp.now()
            )
            _demoBusinessStrategy.value = sanitized
            return
        }
        val uid = actingUserId?.ifBlank { null } ?: requireUserId()
        val sanitized = strategy.copy(
            id = "today",
            storeId = storeId,
            refreshCountToday = strategy.refreshCountToday.coerceIn(0, 5),
            updatedBy = uid
        )
        val actionMaps = sanitized.actions.take(5).map { act ->
            val target = StrategyActionTarget.fromWireKey(act.target)
            mapOf(
                "text" to act.text.trim().take(240),
                "target" to target.wireKey,
                "buttonLabel" to act.buttonLabel.ifBlank {
                    BusinessStrategyBanglaBuilder.actionButtonLabel(target)
                }.take(40)
            )
        }
        val payload = mapOf(
            "storeId" to storeId,
            "dateKey" to sanitized.dateKey.take(10),
            "diagnosis" to sanitized.diagnosis.take(600),
            "focusLine" to sanitized.focusLine.take(400),
            "actions" to actionMaps,
            "generatedAtMs" to sanitized.generatedAtMs,
            "refreshCountToday" to sanitized.refreshCountToday.coerceIn(0, 5),
            "source" to if (sanitized.source == "ai") "ai" else "fixed",
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        database.collection("stores").document(storeId)
            .collection("strategy").document("today")
            .set(payload)
            .await()
    }

    // -------------------------------------------------------------
    // MONTHLY REPORT DATE-RANGE LOADING
    // -------------------------------------------------------------

    data class MonthlyReportInputs(
        val year: Int,
        val month: Int,
        val startMillis: Long,
        val endMillis: Long,
        val sales: List<Sale> = emptyList(),
        val purchases: List<Purchase> = emptyList(),
        val purchaseReturns: List<PurchaseReturn> = emptyList(),
        val payments: List<Payment> = emptyList(),
        val supplierPayments: List<SupplierPayment> = emptyList(),
        val expenses: List<Expense> = emptyList(),
        val otherIncomes: List<OtherIncome> = emptyList(),
        val adjustments: List<AccountAdjustment> = emptyList()
    )

    private val monthlyInputsCache = ConcurrentHashMap<String, MonthlyReportInputs>()

    fun invalidateMonthlyReportCache() {
        monthlyInputsCache.clear()
    }

    fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val calStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calEnd = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return calStart.timeInMillis to calEnd.timeInMillis
    }

    fun filterMonthlyInputsByDateRange(
        year: Int,
        month: Int,
        allSales: List<Sale>,
        allPurchases: List<Purchase>,
        allPurchaseReturns: List<PurchaseReturn>,
        allPayments: List<Payment>,
        allSupplierPayments: List<SupplierPayment>,
        allExpenses: List<Expense>,
        allOtherIncomes: List<OtherIncome>,
        allAdjustments: List<AccountAdjustment>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyReportInputs {
        val (startMillis, endMillis) = monthBounds(year, month)
        val isHistoricalMonth = endMillis < nowMillis

        fun saleTs(s: Sale): Long = s.createdAt?.toDate()?.time ?: 0L
        fun purchaseTs(p: Purchase): Long = p.createdAt?.toDate()?.time ?: 0L
        fun returnTs(r: PurchaseReturn): Long = r.createdAt?.toDate()?.time ?: 0L
        fun paymentTs(p: Payment): Long = if (p.paymentDate > 0L) p.paymentDate else (p.createdAt?.toDate()?.time ?: 0L)
        fun supPaymentTs(sp: SupplierPayment): Long = if (sp.paymentDate > 0L) sp.paymentDate else (sp.createdAt?.toDate()?.time ?: 0L)
        fun expenseTs(e: Expense): Long = if (e.date > 0L) e.date else (e.createdAt?.toDate()?.time ?: 0L)
        fun incomeTs(i: OtherIncome): Long = if (i.date > 0L) i.date else (i.createdAt?.toDate()?.time ?: 0L)
        fun adjTs(a: AccountAdjustment): Long = a.createdAt?.toDate()?.time ?: 0L

        // For active/future months, keep records <= endMillis; for historical months, keep up to nowMillis
        // so opening/closing tie-outs can back out post-month changes accurately without pulling future out-of-range noise.
        val upperLimit = if (isHistoricalMonth) nowMillis else endMillis

        return MonthlyReportInputs(
            year = year,
            month = month,
            startMillis = startMillis,
            endMillis = endMillis,
            sales = allSales.filter { saleTs(it) <= upperLimit },
            purchases = allPurchases.filter { purchaseTs(it) <= upperLimit },
            purchaseReturns = allPurchaseReturns.filter { returnTs(it) <= upperLimit },
            payments = allPayments.filter { paymentTs(it) <= upperLimit },
            supplierPayments = allSupplierPayments.filter { supPaymentTs(it) <= upperLimit },
            expenses = allExpenses.filter { !it.isDeleted && expenseTs(it) <= upperLimit },
            otherIncomes = allOtherIncomes.filter { !it.isDeleted && incomeTs(it) <= upperLimit },
            adjustments = allAdjustments.filter { adjTs(it) <= upperLimit }
        )
    }

    suspend fun loadMonthlyReportInputs(
        storeId: String,
        year: Int,
        month: Int,
        forceRefresh: Boolean = false
    ): MonthlyReportInputs? {
        if (storeId.isBlank() || storeId == "demo_store" || db == null) {
            return null
        }
        val (startMillis, endMillis) = monthBounds(year, month)
        val cacheKey = "$storeId:$year:$month"
        if (!forceRefresh) {
            monthlyInputsCache[cacheKey]?.let { return it }
        }
        val database = db ?: return null
        val storeRef = database.collection("stores").document(storeId)
        val nowMillis = System.currentTimeMillis()
        val queryUpperMillis = maxOf(endMillis, nowMillis)
        val upperTs = Timestamp(Date(queryUpperMillis))

        return withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    val salesDeferred = async {
                        storeRef.collection("sales")
                            .whereLessThanOrEqualTo("createdAt", upperTs)
                            .get().await()
                    }
                    val purDeferred = async {
                        storeRef.collection("purchases")
                            .whereLessThanOrEqualTo("createdAt", upperTs)
                            .get().await()
                    }
                    val retDeferred = async {
                        storeRef.collection("purchaseReturns")
                            .whereLessThanOrEqualTo("createdAt", upperTs)
                            .get().await()
                    }
                    val adjDeferred = async {
                        storeRef.collection("accountAdjustments")
                            .whereLessThanOrEqualTo("createdAt", upperTs)
                            .get().await()
                    }
                    val payDeferred = async {
                        storeRef.collection("payments")
                            .whereLessThanOrEqualTo("paymentDate", queryUpperMillis)
                            .get().await()
                    }
                    val spDeferred = async {
                        storeRef.collection("supplierPayments")
                            .whereLessThanOrEqualTo("paymentDate", queryUpperMillis)
                            .get().await()
                    }
                    val expDeferred = async {
                        storeRef.collection("expenses")
                            .whereLessThanOrEqualTo("date", queryUpperMillis)
                            .get().await()
                    }
                    val incDeferred = async {
                        storeRef.collection("otherIncomes")
                            .whereLessThanOrEqualTo("date", queryUpperMillis)
                            .get().await()
                    }

                    val salesSnap = salesDeferred.await()
                    val purSnap = purDeferred.await()
                    val retSnap = retDeferred.await()
                    val adjSnap = adjDeferred.await()
                    val paySnap = payDeferred.await()
                    val spSnap = spDeferred.await()
                    val expSnap = expDeferred.await()
                    val incSnap = incDeferred.await()

                    val result = filterMonthlyInputsByDateRange(
                        year = year,
                        month = month,
                        allSales = salesSnap.documents.mapNotNull { it.toObject(Sale::class.java) },
                        allPurchases = purSnap.documents.mapNotNull { it.toObject(Purchase::class.java) },
                        allPurchaseReturns = retSnap.documents.mapNotNull { it.toObject(PurchaseReturn::class.java) },
                        allPayments = paySnap.documents.mapNotNull { it.toObject(Payment::class.java) },
                        allSupplierPayments = spSnap.documents.mapNotNull { it.toObject(SupplierPayment::class.java) },
                        allExpenses = expSnap.documents.mapNotNull { it.toObject(Expense::class.java) },
                        allOtherIncomes = incSnap.documents.mapNotNull { it.toObject(OtherIncome::class.java) },
                        allAdjustments = adjSnap.documents.mapNotNull { it.toObject(AccountAdjustment::class.java) },
                        nowMillis = nowMillis
                    )
                    monthlyInputsCache[cacheKey] = result
                    result
                }
            }.getOrNull()
        }
    }
}

