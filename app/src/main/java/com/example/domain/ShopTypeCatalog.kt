package com.example.domain

import com.example.data.model.Account
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.Store
import com.example.data.model.StoreCategory
import com.example.data.model.StoreMember
import com.example.data.model.StoreNote
import com.example.data.model.StorePermissions
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.util.AppLanguage
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

enum class ShopType(val id: String) {
    GROCERY("grocery"),
    HARDWARE("hardware"),
    PHARMACY("pharmacy"),
    CLOTHING("clothing"),
    ELECTRONICS("electronics"),
    STATIONERY("stationery"),
    GENERAL("general");

    companion object {
        fun fromId(raw: String?): ShopType? {
            val clean = raw?.trim()?.lowercase().orEmpty()
            if (clean.isEmpty()) return null
            return entries.firstOrNull { it.id == clean }
        }
    }
}

data class ShopUnitPreset(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val category: String,
    val stockUnit: String,
    val buyUnit: String,
    val buyConversion: Double,
    val sellUnits: Map<String, Double> = emptyMap()
)

object ShopTypeCatalog {

    fun displayName(type: ShopType, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.EN -> when (type) {
                ShopType.GROCERY -> "Grocery"
                ShopType.HARDWARE -> "Hardware"
                ShopType.PHARMACY -> "Pharmacy"
                ShopType.CLOTHING -> "Clothing"
                ShopType.ELECTRONICS -> "Electronics"
                ShopType.STATIONERY -> "Stationery"
                ShopType.GENERAL -> "General"
            }
            AppLanguage.BN -> when (type) {
                ShopType.GROCERY -> "মুদি দোকান"
                ShopType.HARDWARE -> "হার্ডওয়্যার"
                ShopType.PHARMACY -> "ফার্মেসি"
                ShopType.CLOTHING -> "কাপড়ের দোকান"
                ShopType.ELECTRONICS -> "ইলেকট্রনিক্স"
                ShopType.STATIONERY -> "স্টেশনারি"
                ShopType.GENERAL -> "সাধারণ দোকান"
            }
        }
    }

    fun subtitle(type: ShopType, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.EN -> when (type) {
                ShopType.GROCERY -> "sack → kg, carton → litre"
                ShopType.HARDWARE -> "box → kg, roll → meter"
                ShopType.PHARMACY -> "box → pack → pcs"
                ShopType.CLOTHING -> "dozen → pcs, roll → meter"
                ShopType.ELECTRONICS -> "roll → meter, carton → pcs"
                ShopType.STATIONERY -> "box → pcs, carton → pack"
                ShopType.GENERAL -> "pcs, box → pcs, sack → kg"
            }
            AppLanguage.BN -> when (type) {
                ShopType.GROCERY -> "বস্তা → কেজি, কার্টন → লিটার"
                ShopType.HARDWARE -> "বক্স → কেজি, রোল → মিটার"
                ShopType.PHARMACY -> "বক্স → প্যাক → পিস"
                ShopType.CLOTHING -> "ডজন → পিস, রোল → মিটার"
                ShopType.ELECTRONICS -> "রোল → মিটার, কার্টন → পিস"
                ShopType.STATIONERY -> "বক্স → পিস, কার্টন → প্যাক"
                ShopType.GENERAL -> "পিস, বক্স → পিস, বস্তা → কেজি"
            }
        }
    }

    fun startingCategories(type: ShopType): List<String> {
        return when (type) {
            ShopType.HARDWARE -> listOf(
                "Hand Tools",
                "Power Tools",
                "Nuts & Bolts",
                "Electrical",
                "Paint",
                "Plumbing"
            )
            ShopType.GROCERY -> listOf(
                "Rice & Grains",
                "Oil & Ghee",
                "Spices",
                "Dairy",
                "Beverages",
                "Snacks",
                "Personal Care"
            )
            ShopType.PHARMACY -> listOf(
                "Tablets & Capsules",
                "Syrup & Drops",
                "First Aid",
                "Personal Care",
                "Baby Care",
                "Devices"
            )
            ShopType.CLOTHING -> listOf(
                "Men's Wear",
                "Women's Wear",
                "Kids Wear",
                "Fabrics",
                "Footwear",
                "Accessories"
            )
            ShopType.ELECTRONICS -> listOf(
                "Mobile Accessories",
                "Cables & Wires",
                "Lighting",
                "Appliances",
                "Batteries",
                "Audio"
            )
            ShopType.STATIONERY -> listOf(
                "Notebooks & Paper",
                "Pens & Pencils",
                "Office Supplies",
                "Art & Craft",
                "Files & Folders",
                "School Bags"
            )
            ShopType.GENERAL -> listOf(
                "Daily Essentials",
                "Packaged Goods",
                "Beverages",
                "Household",
                "Personal Care",
                "General"
            )
        }
    }

    fun unitPresets(type: ShopType): List<ShopUnitPreset> {
        return when (type) {
            ShopType.HARDWARE -> listOf(
                ShopUnitPreset(
                    id = "hw_nails",
                    labelEn = "Nails: box → kg",
                    labelBn = "পেরেক: বক্স → কেজি",
                    category = "Nuts & Bolts",
                    stockUnit = "kg",
                    buyUnit = "box",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "hw_wire",
                    labelEn = "Wire: roll → meter",
                    labelBn = "তার: রোল → মিটার",
                    category = "Electrical",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 90.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "hw_paint",
                    labelEn = "Paint: carton → litre",
                    labelBn = "রং: কার্টন → লিটার",
                    category = "Paint",
                    stockUnit = "litre",
                    buyUnit = "carton",
                    buyConversion = 4.0,
                    sellUnits = mapOf("ml" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "hw_tools",
                    labelEn = "Tools: box → pcs",
                    labelBn = "টুলস: বক্স → পিস",
                    category = "Hand Tools",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 10.0
                )
            )
            ShopType.GROCERY -> listOf(
                ShopUnitPreset(
                    id = "gr_rice",
                    labelEn = "Rice: sack → kg",
                    labelBn = "চাল: বস্তা → কেজি",
                    category = "Rice & Grains",
                    stockUnit = "kg",
                    buyUnit = "sack",
                    buyConversion = 50.0,
                    sellUnits = mapOf("gram" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "gr_oil",
                    labelEn = "Oil: carton → litre",
                    labelBn = "তেল: কার্টন → লিটার",
                    category = "Oil & Ghee",
                    stockUnit = "litre",
                    buyUnit = "carton",
                    buyConversion = 12.0,
                    sellUnits = mapOf("ml" to 1000.0)
                ),
                ShopUnitPreset(
                    id = "gr_pack",
                    labelEn = "Packs: box → pcs",
                    labelBn = "প্যাক: বক্স → পিস",
                    category = "Snacks",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                )
            )
            ShopType.PHARMACY -> listOf(
                ShopUnitPreset(
                    id = "ph_tab",
                    labelEn = "Tablets: box → pack",
                    labelBn = "ট্যাবলেট: বক্স → প্যাক",
                    category = "Tablets & Capsules",
                    stockUnit = "pack",
                    buyUnit = "box",
                    buyConversion = 10.0,
                    sellUnits = mapOf("pcs" to 10.0)
                ),
                ShopUnitPreset(
                    id = "ph_syr",
                    labelEn = "Syrup: carton → pcs",
                    labelBn = "সিরাপ: কার্টন → পিস",
                    category = "Syrup & Drops",
                    stockUnit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "ph_aid",
                    labelEn = "First Aid: pack → pcs",
                    labelBn = "ফাস্ট এইড: প্যাক → পিস",
                    category = "First Aid",
                    stockUnit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 12.0
                )
            )
            ShopType.CLOTHING -> listOf(
                ShopUnitPreset(
                    id = "cl_pcs",
                    labelEn = "Wear: dozen → pcs",
                    labelBn = "পোশাক: ডজন → পিস",
                    category = "Men's Wear",
                    stockUnit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "cl_fab",
                    labelEn = "Fabric: roll → meter",
                    labelBn = "কাপড়: রোল → মিটার",
                    category = "Fabrics",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 25.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "cl_pack",
                    labelEn = "Items: pack → pcs",
                    labelBn = "আইটেম: প্যাক → পিস",
                    category = "Accessories",
                    stockUnit = "pcs",
                    buyUnit = "pack",
                    buyConversion = 6.0
                )
            )
            ShopType.ELECTRONICS -> listOf(
                ShopUnitPreset(
                    id = "el_wire",
                    labelEn = "Cable: roll → meter",
                    labelBn = "কেবল: রোল → মিটার",
                    category = "Cables & Wires",
                    stockUnit = "meter",
                    buyUnit = "roll",
                    buyConversion = 100.0,
                    sellUnits = mapOf("feet" to 3.2808)
                ),
                ShopUnitPreset(
                    id = "el_bulb",
                    labelEn = "Bulbs: carton → pcs",
                    labelBn = "বাল্ব: কার্টন → পিস",
                    category = "Lighting",
                    stockUnit = "pcs",
                    buyUnit = "carton",
                    buyConversion = 20.0
                ),
                ShopUnitPreset(
                    id = "el_bat",
                    labelEn = "Cells: box → pcs",
                    labelBn = "ব্যাটারি: বক্স → পিস",
                    category = "Batteries",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 24.0
                )
            )
            ShopType.STATIONERY -> listOf(
                ShopUnitPreset(
                    id = "st_pen",
                    labelEn = "Pens: box → pcs",
                    labelBn = "কলম: বক্স → পিস",
                    category = "Pens & Pencils",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "st_nb",
                    labelEn = "Books: dozen → pcs",
                    labelBn = "খাতা: ডজন → পিস",
                    category = "Notebooks & Paper",
                    stockUnit = "pcs",
                    buyUnit = "dozen",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "st_paper",
                    labelEn = "Paper: carton → pack",
                    labelBn = "কাগজ: কার্টন → প্যাক",
                    category = "Office Supplies",
                    stockUnit = "pack",
                    buyUnit = "carton",
                    buyConversion = 5.0
                )
            )
            ShopType.GENERAL -> listOf(
                ShopUnitPreset(
                    id = "gn_pcs",
                    labelEn = "Standard: pcs",
                    labelBn = "সাধারণ: পিস",
                    category = "Daily Essentials",
                    stockUnit = "pcs",
                    buyUnit = "pcs",
                    buyConversion = 1.0
                ),
                ShopUnitPreset(
                    id = "gn_box",
                    labelEn = "Boxed: box → pcs",
                    labelBn = "বক্স: বক্স → পিস",
                    category = "Packaged Goods",
                    stockUnit = "pcs",
                    buyUnit = "box",
                    buyConversion = 12.0
                ),
                ShopUnitPreset(
                    id = "gn_sack",
                    labelEn = "Bulk: sack → kg",
                    labelBn = "বস্তা: বস্তা → কেজি",
                    category = "Daily Essentials",
                    stockUnit = "kg",
                    buyUnit = "sack",
                    buyConversion = 25.0,
                    sellUnits = mapOf("gram" to 1000.0)
                )
            )
        }
    }

    fun presetForCategory(type: ShopType, category: String): ShopUnitPreset? {
        val clean = category.trim()
        val presets = unitPresets(type)
        return presets.firstOrNull { it.category.equals(clean, ignoreCase = true) }
            ?: when (type) {
                ShopType.HARDWARE -> presets.firstOrNull { it.id == "hw_tools" }
                else -> presets.firstOrNull()
            }
    }

    fun mergeSuggestedCategories(existing: List<String>, type: ShopType): List<String> {
        val result = existing.map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        for (cat in startingCategories(type)) {
            if (result.none { it.equals(cat, ignoreCase = true) }) {
                result.add(cat)
            }
        }
        return result
    }

    fun demoStoreName(type: ShopType): String = when (type) {
        ShopType.GROCERY -> "Bismillah Grocery Demo (মুদি)"
        ShopType.HARDWARE -> "Bismillah Hardware Demo (হার্ডওয়্যার)"
        ShopType.PHARMACY -> "Shifa Pharmacy Demo (ফার্মেসি)"
        ShopType.CLOTHING -> "Aarong Fabrics Demo (কাপড়)"
        ShopType.ELECTRONICS -> "TechPoint Electronics Demo"
        ShopType.STATIONERY -> "Boimela Stationery Demo"
        ShopType.GENERAL -> "Bismillah General Demo (সাধারণ)"
    }

    data class ShopDemoSeed(
        val shopType: ShopType,
        val store: Store,
        val products: List<Product>,
        val customers: List<Customer>,
        val suppliers: List<Supplier>,
        val sales: List<Sale>,
        val purchases: List<Purchase>,
        val purchaseReturns: List<PurchaseReturn>,
        val payments: List<Payment>,
        val supplierPayments: List<SupplierPayment>,
        val accounts: List<Account>,
        val expenses: List<Expense>,
        val otherIncomes: List<OtherIncome>,
        val categories: List<StoreCategory>,
        val notes: List<StoreNote>,
        val members: List<StoreMember>,
        val nextInvoiceSeq: Long = 105L,
        val nextPaymentSeq: Long = 1L,
        val nextPurchaseSeq: Long = 103L,
        val nextReturnSeq: Long = 1L
    )

    private data class SupplierSeedSpec(
        val idSuffix: String,
        val name: String,
        val mobile: String,
        val company: String,
        val address: String,
        val balance: Double
    )

    private fun monthAnchoredTime(nowMs: Long, deltaMs: Long): Long {
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val candidate = (nowMs - deltaMs).coerceAtMost(nowMs)
        return if (candidate >= monthStart) candidate else monthStart
    }

    fun demoProducts(type: ShopType): List<Product> = buildDemoSeed(type).products

    fun buildDemoSeed(type: ShopType, nowMs: Long = System.currentTimeMillis()): ShopDemoSeed {
        val t1 = monthAnchoredTime(nowMs, 3_600_000L * 48)
        val t2 = monthAnchoredTime(nowMs, 3_600_000L * 32)
        val t3 = monthAnchoredTime(nowMs, 3_600_000L * 20)
        val t4 = monthAnchoredTime(nowMs, 3_600_000L * 6)
        val t5 = monthAnchoredTime(nowMs, 1_800_000L)

        val products = seedProductsFor(type)
        val customers = listOf(
            Customer(
                id = "cust_1",
                storeId = "demo_store",
                name = "Mohammad Rahim",
                mobile = "01711223344",
                address = "House 12, Road 4, Mirpur",
                notes = "Regular buyer",
                currentBalance = 850.0,
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 5))
            ),
            Customer(
                id = "cust_2",
                storeId = "demo_store",
                name = "Kabir Hossain",
                mobile = "01819887766",
                address = "Shop 9, Market Lane",
                notes = "Clears every Thursday",
                currentBalance = 1420.0,
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 4))
            ),
            Customer(
                id = "cust_3",
                storeId = "demo_store",
                name = "Salma Akter",
                mobile = "01912334455",
                address = "Sector 3, Uttara",
                notes = "Monthly credit account",
                currentBalance = 320.0,
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 3))
            ),
            Customer(
                id = "cust_4",
                storeId = "demo_store",
                name = "Fatema Begum",
                mobile = "01677889900",
                address = "Apartment 5B",
                notes = "Pays cash",
                currentBalance = 0.0,
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 2))
            )
        )

        val supSpecs = seedSuppliersFor(type)
        val suppliers = supSpecs.mapIndexed { idx, s ->
            Supplier(
                id = "sup_${idx + 1}",
                storeId = "demo_store",
                name = s.name,
                mobile = s.mobile,
                company = s.company,
                address = s.address,
                balance = s.balance,
                createdBy = "demo_user",
                createdAt = Timestamp(Date(nowMs - 86_400_000L * (10 - idx * 2)))
            )
        }

        fun saleItem(p: Product, qty: Double, discount: Double = 0.0): SaleItem {
            val gross = CalculationEngine.roundMoney(p.sellPrice * qty)
            val boundD = discount.coerceIn(0.0, gross)
            val net = CalculationEngine.roundMoney(gross - boundD)
            val profit = CalculationEngine.roundMoney((p.sellPrice - p.buyPrice) * qty - boundD)
            return SaleItem(
                productId = p.id,
                name = p.name,
                qty = qty,
                unit = p.unit,
                stockQty = qty,
                buyPrice = p.buyPrice,
                sellPrice = p.sellPrice,
                discount = boundD,
                lineTotal = net,
                profit = profit
            )
        }

        val p0 = products[0]
        val p1 = products[1]
        val p2 = products[2]
        val p3 = products[3]
        val p4 = products[4]

        val s1Items = listOf(saleItem(p0, 2.0, discount = 10.0), saleItem(p3, 1.0))
        val s1Sub = CalculationEngine.roundMoney(s1Items.sumOf { it.sellPrice * it.qty })
        val s1Disc = 10.0
        val s1Total = CalculationEngine.roundMoney(s1Sub - s1Disc)
        val s1Profit = CalculationEngine.roundMoney(s1Items.sumOf { it.profit })

        val s2Items = listOf(saleItem(p1, 2.0, discount = 5.0))
        val s2Sub = CalculationEngine.roundMoney(s2Items.sumOf { it.sellPrice * it.qty })
        val s2Total = CalculationEngine.roundMoney(s2Sub - 5.0)
        val s2Profit = CalculationEngine.roundMoney(s2Items.sumOf { it.profit })

        val s3Items = listOf(saleItem(p0, 1.0), saleItem(p2, 2.0))
        val s3Sub = CalculationEngine.roundMoney(s3Items.sumOf { it.sellPrice * it.qty })
        val s3Profit = CalculationEngine.roundMoney(s3Items.sumOf { it.profit })

        val s4Items = listOf(saleItem(p4, 2.0))
        val s4Sub = CalculationEngine.roundMoney(s4Items.sumOf { it.sellPrice * it.qty })
        val s4Paid = CalculationEngine.roundMoney(s4Sub * 0.5)
        val s4Due = CalculationEngine.roundMoney(s4Sub - s4Paid)
        val s4Profit = CalculationEngine.roundMoney(s4Items.sumOf { it.profit })

        val s5Items = listOf(saleItem(p2, 1.0), saleItem(p3, 1.0))
        val s5Sub = CalculationEngine.roundMoney(s5Items.sumOf { it.sellPrice * it.qty })
        val s5Profit = CalculationEngine.roundMoney(s5Items.sumOf { it.profit })

        val sales = listOf(
            Sale(
                id = "sale_5",
                storeId = "demo_store",
                invoiceNo = "INV-0105",
                items = s5Items,
                totalItems = 2.0,
                subtotal = s5Sub,
                discount = 0.0,
                grandTotal = s5Sub,
                paidAmount = s5Sub,
                dueAmount = 0.0,
                profit = s5Profit,
                paymentType = "cash",
                customerName = "Cash Sale",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                soldBy = "demo_user",
                createdAt = Timestamp(Date(t5))
            ),
            Sale(
                id = "sale_4",
                storeId = "demo_store",
                invoiceNo = "INV-0104",
                items = s4Items,
                totalItems = 2.0,
                subtotal = s4Sub,
                discount = 0.0,
                grandTotal = s4Sub,
                paidAmount = s4Paid,
                dueAmount = s4Due,
                profit = s4Profit,
                paymentType = "baki",
                customerId = customers[2].id,
                customerName = customers[2].name,
                customerPhone = customers[2].mobile,
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                soldBy = "demo_user",
                createdAt = Timestamp(Date(t4))
            ),
            Sale(
                id = "sale_3",
                storeId = "demo_store",
                invoiceNo = "INV-0103",
                items = s3Items,
                totalItems = 3.0,
                subtotal = s3Sub,
                discount = 0.0,
                grandTotal = s3Sub,
                paidAmount = 0.0,
                dueAmount = s3Sub,
                profit = s3Profit,
                paymentType = "baki",
                customerId = customers[1].id,
                customerName = customers[1].name,
                customerPhone = customers[1].mobile,
                soldBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            ),
            Sale(
                id = "sale_2",
                storeId = "demo_store",
                invoiceNo = "INV-0102",
                items = s2Items,
                totalItems = 2.0,
                subtotal = s2Sub,
                discount = 5.0,
                grandTotal = s2Total,
                paidAmount = s2Total,
                dueAmount = 0.0,
                profit = s2Profit,
                paymentType = "cash",
                customerName = "Cash Sale",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                soldBy = "demo_user",
                createdAt = Timestamp(Date(t2))
            ),
            Sale(
                id = "sale_1",
                storeId = "demo_store",
                invoiceNo = "INV-0101",
                items = s1Items,
                totalItems = 3.0,
                subtotal = s1Sub,
                discount = s1Disc,
                grandTotal = s1Total,
                paidAmount = 0.0,
                dueAmount = s1Total,
                profit = s1Profit,
                paymentType = "baki",
                customerId = customers[0].id,
                customerName = customers[0].name,
                customerPhone = customers[0].mobile,
                soldBy = "demo_user",
                createdAt = Timestamp(Date(t1))
            )
        )

        val pur1Total = CalculationEngine.roundMoney(p1.buyPrice * 10.0 + p2.buyPrice * 15.0)
        val pur2Total = CalculationEngine.roundMoney(p0.buyPrice * 20.0)
        val pur3Total = CalculationEngine.roundMoney(p4.buyPrice * 12.0)

        val purchases = listOf(
            Purchase(
                id = "pur_1",
                storeId = "demo_store",
                purchaseNo = "PUR-0101",
                supplierId = suppliers[0].id,
                supplierName = suppliers[0].name,
                supplierMobile = suppliers[0].mobile,
                items = listOf(
                    PurchaseItem(
                        productId = p1.id,
                        name = p1.name,
                        qty = 10.0,
                        unit = p1.unit,
                        stockQty = 10.0,
                        buyPrice = p1.buyPrice,
                        costPerStockUnit = p1.buyPrice,
                        lineTotal = CalculationEngine.roundMoney(p1.buyPrice * 10.0)
                    ),
                    PurchaseItem(
                        productId = p2.id,
                        name = p2.name,
                        qty = 15.0,
                        unit = p2.unit,
                        stockQty = 15.0,
                        buyPrice = p2.buyPrice,
                        costPerStockUnit = p2.buyPrice,
                        lineTotal = CalculationEngine.roundMoney(p2.buyPrice * 15.0)
                    )
                ),
                totalItems = 25.0,
                totalAmount = pur1Total,
                paidAmount = pur1Total,
                dueAmount = 0.0,
                paymentType = "paid",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                note = "Supplier stock delivery",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t1))
            ),
            Purchase(
                id = "pur_2",
                storeId = "demo_store",
                purchaseNo = "PUR-0102",
                supplierId = suppliers[1].id,
                supplierName = suppliers[1].name,
                supplierMobile = suppliers[1].mobile,
                items = listOf(
                    PurchaseItem(
                        productId = p0.id,
                        name = p0.name,
                        qty = 20.0,
                        unit = p0.unit,
                        stockQty = 20.0,
                        buyPrice = p0.buyPrice,
                        costPerStockUnit = p0.buyPrice,
                        lineTotal = pur2Total
                    )
                ),
                totalItems = 20.0,
                totalAmount = pur2Total,
                paidAmount = CalculationEngine.roundMoney(pur2Total * 0.5),
                dueAmount = CalculationEngine.roundMoney(pur2Total * 0.5),
                paymentType = "credit",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                note = "Batch #B14",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t2))
            ),
            Purchase(
                id = "pur_3",
                storeId = "demo_store",
                purchaseNo = "PUR-0103",
                supplierId = suppliers[2].id,
                supplierName = suppliers[2].name,
                supplierMobile = suppliers[2].mobile,
                items = listOf(
                    PurchaseItem(
                        productId = p4.id,
                        name = p4.name,
                        qty = 12.0,
                        unit = p4.unit,
                        stockQty = 12.0,
                        buyPrice = p4.buyPrice,
                        costPerStockUnit = p4.buyPrice,
                        lineTotal = pur3Total
                    )
                ),
                totalItems = 12.0,
                totalAmount = pur3Total,
                paidAmount = pur3Total,
                dueAmount = 0.0,
                paymentType = "paid",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                note = "Cash on delivery",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            )
        )

        val purchaseReturns = listOf(
            PurchaseReturn(
                id = "ret_1",
                storeId = "demo_store",
                returnNo = "RET-0001",
                purchaseId = "pur_1",
                supplierId = suppliers[0].id,
                supplierName = suppliers[0].name,
                amount = CalculationEngine.roundMoney(p1.buyPrice * 2.0),
                reason = "Damaged items returned on delivery",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t2))
            )
        )

        val payments = listOf(
            Payment(
                id = "pay_1",
                storeId = "demo_store",
                paymentNo = "PAY-0001",
                customerId = customers[0].id,
                customerName = customers[0].name,
                customerMobile = customers[0].mobile,
                amount = 300.0,
                paymentMethod = "cash",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                paymentDate = t3,
                note = "Partial baki settlement",
                receivedBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            )
        )

        val supplierPayments = listOf(
            SupplierPayment(
                id = "sp_1",
                storeId = "demo_store",
                paymentNo = "PAY-OUT-0001",
                supplierId = suppliers[1].id,
                supplierName = suppliers[1].name,
                amount = 1000.0,
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                paymentDate = t3,
                note = "Installment payment",
                paidBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            )
        )

        val accounts = listOf(
            Account(
                id = "cash_in_hand",
                storeId = "demo_store",
                name = "Cash in Hand",
                type = "Cash",
                accountNumber = "",
                openingBalance = 2500.0,
                isDefault = true,
                isArchived = false,
                createdBy = "system",
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 15))
            ),
            Account(
                id = "acc_bkash",
                storeId = "demo_store",
                name = "bKash Merchant",
                type = "Wallet",
                accountNumber = "01712-345678",
                openingBalance = 3500.0,
                isDefault = false,
                isArchived = false,
                createdBy = "demo_user",
                createdAt = Timestamp(Date(nowMs - 86_400_000L * 10))
            )
        )

        val expenses = listOf(
            Expense(
                id = "exp_1",
                storeId = "demo_store",
                amount = 1200.0,
                category = "Electricity",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                date = t2,
                note = "Monthly electric bill",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t2))
            ),
            Expense(
                id = "exp_2",
                storeId = "demo_store",
                amount = 150.0,
                category = "Tea & Snacks",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                date = t4,
                note = "Tea & snacks for visitors",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t4))
            )
        )

        val otherIncomes = listOf(
            OtherIncome(
                id = "inc_1",
                storeId = "demo_store",
                amount = 450.0,
                category = "Scrap Sale",
                accountId = "cash_in_hand",
                accountName = "Cash in Hand",
                date = t3,
                note = "Sold empty cartons",
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            )
        )

        val categories = listOf(
            StoreCategory(id = "cat_exp_1", storeId = "demo_store", type = "expense", name = "Rent"),
            StoreCategory(id = "cat_exp_2", storeId = "demo_store", type = "expense", name = "Electricity"),
            StoreCategory(id = "cat_exp_3", storeId = "demo_store", type = "expense", name = "Staff Salary"),
            StoreCategory(id = "cat_exp_4", storeId = "demo_store", type = "expense", name = "Transport"),
            StoreCategory(id = "cat_exp_5", storeId = "demo_store", type = "expense", name = "Packaging"),
            StoreCategory(id = "cat_exp_6", storeId = "demo_store", type = "expense", name = "Tea & Snacks"),
            StoreCategory(id = "cat_exp_7", storeId = "demo_store", type = "expense", name = "Maintenance"),
            StoreCategory(id = "cat_exp_8", storeId = "demo_store", type = "expense", name = "Other"),
            StoreCategory(id = "cat_inc_1", storeId = "demo_store", type = "income", name = "Scrap Sale"),
            StoreCategory(id = "cat_inc_2", storeId = "demo_store", type = "income", name = "Owner Deposit"),
            StoreCategory(id = "cat_inc_3", storeId = "demo_store", type = "income", name = "Bank Interest"),
            StoreCategory(id = "cat_inc_4", storeId = "demo_store", type = "income", name = "Other")
        )

        val notes = listOf(
            StoreNote(
                id = "note_1",
                storeId = "demo_store",
                title = "Supplier Delivery Schedule",
                text = "${suppliers[0].name} delivers every Saturday before 10 AM.",
                date = t1,
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t1))
            ),
            StoreNote(
                id = "note_2",
                storeId = "demo_store",
                title = "Low Stock Restock Reminder",
                text = "Reorder ${products[6].name} and ${products[7].name} from ${suppliers[1].name}.",
                date = t3,
                createdBy = "demo_user",
                createdAt = Timestamp(Date(t3))
            )
        )

        val ownerMember = StoreMember(
            userId = "demo_user",
            displayName = "Shop Owner",
            email = "owner@store.com",
            role = "owner",
            roleLabel = "Owner",
            permissionLevel = "Full",
            permissions = StorePermissions.defaultOwnerPermissions(),
            status = "Active",
            joinedTime = nowMs - 86_400_000L * 30,
            invitedBy = ""
        )

        val store = Store(
            id = "demo_store",
            name = demoStoreName(type),
            phone = "+880 1712-345678",
            address = "Shop 14, New Market, Dhaka",
            shopType = type.id,
            productCategories = startingCategories(type),
            ownerId = "demo_user",
            memberUids = listOf("demo_user"),
            members = listOf(ownerMember),
            createdAt = Timestamp(Date(nowMs - 86_400_000L * 30)),
            updatedAt = Timestamp(Date(nowMs))
        )

        return ShopDemoSeed(
            shopType = type,
            store = store,
            products = products,
            customers = customers,
            suppliers = suppliers,
            sales = sales,
            purchases = purchases,
            purchaseReturns = purchaseReturns,
            payments = payments,
            supplierPayments = supplierPayments,
            accounts = accounts,
            expenses = expenses,
            otherIncomes = otherIncomes,
            categories = categories,
            notes = notes,
            members = listOf(ownerMember)
        )
    }

    private fun seedSuppliersFor(type: ShopType): List<SupplierSeedSpec> = when (type) {
        ShopType.GROCERY -> listOf(
            SupplierSeedSpec("1", "Abul Bashar Traders", "01722334455", "Bashar Oil & Spices Co.", "Khatunganj, Chattogram", 6500.0),
            SupplierSeedSpec("2", "Meghna Flour & Rice Mills", "01833445566", "Meghna Group", "Tejgaon I/A, Dhaka", 4200.0),
            SupplierSeedSpec("3", "Unilever Bangladesh Agency", "01944556677", "Unilever Hub", "Kawran Bazar, Dhaka", 0.0)
        )
        ShopType.HARDWARE -> listOf(
            SupplierSeedSpec("1", "Nawabpur Fasteners", "01722334455", "Bengal Hardware Mills", "Nawabpur Road, Dhaka", 8500.0),
            SupplierSeedSpec("2", "BRB Cable Distributor", "01833445566", "BRB Cable Agency", "Mirpur-10, Dhaka", 4200.0),
            SupplierSeedSpec("3", "Berger Paint Depot", "01944556677", "Berger Color Point", "Tejgaon, Dhaka", 0.0)
        )
        ShopType.PHARMACY -> listOf(
            SupplierSeedSpec("1", "Square Pharma Depot", "01722334455", "Square Pharmaceuticals", "Mohakhali, Dhaka", 5400.0),
            SupplierSeedSpec("2", "Incepta Distributors", "01833445566", "Incepta Pharma", "Savar, Dhaka", 3100.0),
            SupplierSeedSpec("3", "MediEquip Supply House", "01944556677", "MediEquip BD", "Mitford, Dhaka", 0.0)
        )
        ShopType.CLOTHING -> listOf(
            SupplierSeedSpec("1", "Narsingdi Textile Mills", "01722334455", "Madhabdi Fabrics", "Narsingdi", 7200.0),
            SupplierSeedSpec("2", "Islampur Cloth Agency", "01833445566", "Islampur Knitwear", "Islampur, Dhaka", 3900.0),
            SupplierSeedSpec("3", "Chawk Bazaar Garments", "01944556677", "Metro Apparels", "Chawk Bazaar, Dhaka", 0.0)
        )
        ShopType.ELECTRONICS -> listOf(
            SupplierSeedSpec("1", "Walton Electric Depot", "01722334455", "Walton Hi-Tech", "Gazipur", 6800.0),
            SupplierSeedSpec("2", "Multiplan Tech Hub", "01833445566", "Daffodil Imports", "Elephant Road, Dhaka", 3500.0),
            SupplierSeedSpec("3", "Olympic Battery Distributors", "01944556677", "Olympic Traders", "Kaptan Bazar, Dhaka", 0.0)
        )
        ShopType.STATIONERY -> listOf(
            SupplierSeedSpec("1", "Banglabazar Paper House", "01722334455", "Bashundhara Paper Co.", "Banglabazar, Dhaka", 4600.0),
            SupplierSeedSpec("2", "Matador Stationery Agency", "01833445566", "Matador Industries", "Chawk Bazar, Dhaka", 2800.0),
            SupplierSeedSpec("3", "Faber Craft Imports", "01944556677", "Artline Wholesale", "Nilkhet, Dhaka", 0.0)
        )
        ShopType.GENERAL -> listOf(
            SupplierSeedSpec("1", "Kamal Wholesale Mart", "01722334455", "Kamal Distributors", "Moulvibazar, Dhaka", 5500.0),
            SupplierSeedSpec("2", "Padma Consumer Goods", "01833445566", "Padma Trading Co.", "Tejgaon, Dhaka", 3200.0),
            SupplierSeedSpec("3", "City Household Agency", "01944556677", "City Supplies", "Jatrabari, Dhaka", 0.0)
        )
    }

    private fun seedProductsFor(type: ShopType): List<Product> = when (type) {
        ShopType.GROCERY -> listOf(
            Product(id = "demo_gr_1", storeId = "demo_store", name = "Miniket Rice", nameBn = "মিনিকেট চাল", barcode = "890101", category = "Rice & Grains", unit = "kg", buyUnit = "sack", buyConversion = 50.0, sellUnits = mapOf("gram" to 1000.0), buyPrice = 68.0, sellPrice = 78.0, stock = 125.0, lowStockLimit = 25.0, isDemo = true),
            Product(id = "demo_gr_2", storeId = "demo_store", name = "Rupchanda Soybean Oil", nameBn = "রূপচাঁদা সয়াবিন তেল", barcode = "890102", category = "Oil & Ghee", unit = "litre", buyUnit = "carton", buyConversion = 12.0, sellUnits = mapOf("ml" to 1000.0), buyPrice = 165.0, sellPrice = 185.0, stock = 36.0, lowStockLimit = 8.0, isDemo = true),
            Product(id = "demo_gr_3", storeId = "demo_store", name = "Turmeric Powder 200g", nameBn = "হলুদ গুঁড়া ২০০ গ্রাম", barcode = "890103", category = "Spices", unit = "pack", buyUnit = "box", buyConversion = 20.0, buyPrice = 70.0, sellPrice = 85.0, stock = 40.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_gr_4", storeId = "demo_store", name = "Aarong Butter 200g", nameBn = "আড়ং মাখন ২০০ গ্রাম", barcode = "890104", category = "Dairy", unit = "pack", buyUnit = "box", buyConversion = 12.0, buyPrice = 180.0, sellPrice = 210.0, stock = 18.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_gr_5", storeId = "demo_store", name = "Mango Juice 1L", nameBn = "ম্যাঙ্গো জুস ১ লিটার", barcode = "890105", category = "Beverages", unit = "pcs", buyUnit = "carton", buyConversion = 12.0, buyPrice = 90.0, sellPrice = 110.0, stock = 24.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_gr_6", storeId = "demo_store", name = "Red Lentils (Masoor)", nameBn = "মসুর ডাল", barcode = "890106", category = "Rice & Grains", unit = "kg", buyUnit = "sack", buyConversion = 25.0, sellUnits = mapOf("gram" to 1000.0), buyPrice = 115.0, sellPrice = 135.0, stock = 30.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_gr_7", storeId = "demo_store", name = "Lifebuoy Soap 100g", nameBn = "লাইফবয় সাবান", barcode = "890107", category = "Personal Care", unit = "pcs", buyUnit = "box", buyConversion = 24.0, buyPrice = 48.0, sellPrice = 60.0, stock = 4.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_gr_8", storeId = "demo_store", name = "ACI Pure Salt 1kg", nameBn = "এসিআই লবণ ১ কেজি", barcode = "890108", category = "Spices", unit = "pack", buyUnit = "sack", buyConversion = 25.0, buyPrice = 35.0, sellPrice = 42.0, stock = 0.0, lowStockLimit = 10.0, isDemo = true)
        )
        ShopType.HARDWARE -> listOf(
            Product(id = "demo_hw_1", storeId = "demo_store", name = "Wire Nails 2 inch", nameBn = "তারকাঁটা পেরেক ২ ইঞ্চি", barcode = "891101", category = "Nuts & Bolts", unit = "kg", buyUnit = "box", buyConversion = 25.0, sellUnits = mapOf("gram" to 1000.0), buyPrice = 120.0, sellPrice = 160.0, stock = 50.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_hw_2", storeId = "demo_store", name = "BRB Copper Wire 1.5mm", nameBn = "বিআরবি কপার তার ১.৫ মিমি", barcode = "891102", category = "Electrical", unit = "meter", buyUnit = "roll", buyConversion = 90.0, sellUnits = mapOf("feet" to 3.2808), buyPrice = 32.0, sellPrice = 42.0, stock = 180.0, lowStockLimit = 30.0, isDemo = true),
            Product(id = "demo_hw_3", storeId = "demo_store", name = "Berger Interior Paint", nameBn = "বার্জার ইন্টেরিয়র পেইন্ট", barcode = "891103", category = "Paint", unit = "litre", buyUnit = "carton", buyConversion = 4.0, sellUnits = mapOf("ml" to 1000.0), buyPrice = 420.0, sellPrice = 490.0, stock = 24.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_hw_4", storeId = "demo_store", name = "Claw Hammer 16oz", nameBn = "ক্ল হ্যামার ১৬ আউন্স", barcode = "891104", category = "Hand Tools", unit = "pcs", buyUnit = "box", buyConversion = 10.0, buyPrice = 240.0, sellPrice = 310.0, stock = 16.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_hw_5", storeId = "demo_store", name = "PVC Elbow 1 inch", nameBn = "পিভিসি এলবো ১ ইঞ্চি", barcode = "891105", category = "Plumbing", unit = "pcs", buyUnit = "pack", buyConversion = 20.0, buyPrice = 22.0, sellPrice = 32.0, stock = 60.0, lowStockLimit = 15.0, isDemo = true),
            Product(id = "demo_hw_6", storeId = "demo_store", name = "Flat Steel Screw 1.5in", nameBn = "স্টিল স্ক্রু ১.৫ ইঞ্চি", barcode = "891106", category = "Nuts & Bolts", unit = "box", buyUnit = "carton", buyConversion = 20.0, sellUnits = mapOf("pcs" to 100.0), buyPrice = 95.0, sellPrice = 130.0, stock = 25.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_hw_7", storeId = "demo_store", name = "Angle Grinder 4 inch", nameBn = "অ্যাঙ্গেল গ্রাইন্ডার", barcode = "891107", category = "Power Tools", unit = "pcs", buyUnit = "pcs", buyConversion = 1.0, buyPrice = 2450.0, sellPrice = 2950.0, stock = 2.0, lowStockLimit = 3.0, isDemo = true),
            Product(id = "demo_hw_8", storeId = "demo_store", name = "Paint Roller 9 inch", nameBn = "পেইন্ট রোলার ৯ ইঞ্চি", barcode = "891108", category = "Paint", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 85.0, sellPrice = 120.0, stock = 0.0, lowStockLimit = 6.0, isDemo = true)
        )
        ShopType.PHARMACY -> listOf(
            Product(id = "demo_ph_1", storeId = "demo_store", name = "Napa 500mg Tablet", nameBn = "নাপা ৫০০ মি.গ্রা.", barcode = "892101", category = "Tablets & Capsules", unit = "pack", buyUnit = "box", buyConversion = 50.0, sellUnits = mapOf("pcs" to 10.0), buyPrice = 10.0, sellPrice = 12.0, stock = 80.0, lowStockLimit = 15.0, isDemo = true),
            Product(id = "demo_ph_2", storeId = "demo_store", name = "Cough Relief Syrup 100ml", nameBn = "কাফ সিরাপ ১০০ মি.লি.", barcode = "892102", category = "Syrup & Drops", unit = "pcs", buyUnit = "carton", buyConversion = 12.0, buyPrice = 65.0, sellPrice = 80.0, stock = 22.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_ph_3", storeId = "demo_store", name = "Elastic Bandage Roll", nameBn = "ব্যান্ডেজ রোল", barcode = "892103", category = "First Aid", unit = "pcs", buyUnit = "pack", buyConversion = 12.0, buyPrice = 30.0, sellPrice = 45.0, stock = 30.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_ph_4", storeId = "demo_store", name = "Digital Thermometer", nameBn = "ডিজিটাল থার্মোমিটার", barcode = "892104", category = "Devices", unit = "pcs", buyUnit = "box", buyConversion = 10.0, buyPrice = 140.0, sellPrice = 190.0, stock = 16.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_ph_5", storeId = "demo_store", name = "ORS Rehydration Sachet", nameBn = "ওআরএস স্যালাইন", barcode = "892105", category = "First Aid", unit = "pack", buyUnit = "box", buyConversion = 20.0, buyPrice = 4.5, sellPrice = 6.0, stock = 100.0, lowStockLimit = 20.0, isDemo = true),
            Product(id = "demo_ph_6", storeId = "demo_store", name = "Seclo 20mg Capsule", nameBn = "সেকলো ২০ মি.গ্রা.", barcode = "892106", category = "Tablets & Capsules", unit = "pack", buyUnit = "box", buyConversion = 10.0, sellUnits = mapOf("pcs" to 10.0), buyPrice = 48.0, sellPrice = 60.0, stock = 45.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_ph_7", storeId = "demo_store", name = "Antiseptic Solution 100ml", nameBn = "অ্যান্টিসেপটিক সলিউশন", barcode = "892107", category = "Personal Care", unit = "pcs", buyUnit = "carton", buyConversion = 12.0, buyPrice = 55.0, sellPrice = 70.0, stock = 3.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_ph_8", storeId = "demo_store", name = "Surgical Face Mask Box", nameBn = "সার্জিক্যাল মাস্ক", barcode = "892108", category = "Devices", unit = "box", buyUnit = "carton", buyConversion = 20.0, sellUnits = mapOf("pcs" to 50.0), buyPrice = 90.0, sellPrice = 130.0, stock = 0.0, lowStockLimit = 5.0, isDemo = true)
        )
        ShopType.CLOTHING -> listOf(
            Product(id = "demo_cl_1", storeId = "demo_store", name = "Cotton Polo T-Shirt", nameBn = "কটন পোলো টি-শার্ট", barcode = "893101", category = "Men's Wear", unit = "pcs", buyUnit = "dozen", buyConversion = 12.0, buyPrice = 280.0, sellPrice = 380.0, stock = 36.0, lowStockLimit = 8.0, isDemo = true),
            Product(id = "demo_cl_2", storeId = "demo_store", name = "Linen Cotton Fabric", nameBn = "লিনেন কটন কাপড়", barcode = "893102", category = "Fabrics", unit = "meter", buyUnit = "roll", buyConversion = 25.0, sellUnits = mapOf("feet" to 3.2808), buyPrice = 180.0, sellPrice = 240.0, stock = 50.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_cl_3", storeId = "demo_store", name = "Ladies Printed Kurti", nameBn = "লেডিস প্রিন্টেড কুর্তি", barcode = "893103", category = "Women's Wear", unit = "pcs", buyUnit = "pack", buyConversion = 6.0, buyPrice = 620.0, sellPrice = 850.0, stock = 18.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_cl_4", storeId = "demo_store", name = "Leather Belt Classic", nameBn = "চামড়ার বেল্ট", barcode = "893104", category = "Accessories", unit = "pcs", buyUnit = "dozen", buyConversion = 12.0, buyPrice = 220.0, sellPrice = 320.0, stock = 20.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_cl_5", storeId = "demo_store", name = "Kids Cotton Panjabi", nameBn = "শিশুদের কটন পাঞ্জাবি", barcode = "893105", category = "Kids Wear", unit = "pcs", buyUnit = "pack", buyConversion = 6.0, buyPrice = 420.0, sellPrice = 580.0, stock = 24.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_cl_6", storeId = "demo_store", name = "Stretch Denim Jeans", nameBn = "ডেনিম জিন্স", barcode = "893106", category = "Men's Wear", unit = "pcs", buyUnit = "dozen", buyConversion = 12.0, buyPrice = 750.0, sellPrice = 1050.0, stock = 24.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_cl_7", storeId = "demo_store", name = "Cotton Scarf Dupatta", nameBn = "সুতির ওড়না", barcode = "893107", category = "Women's Wear", unit = "pcs", buyUnit = "dozen", buyConversion = 12.0, buyPrice = 160.0, sellPrice = 230.0, stock = 3.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_cl_8", storeId = "demo_store", name = "Ankle Sports Socks", nameBn = "স্পোর্টস মোজা", barcode = "893108", category = "Accessories", unit = "pack", buyUnit = "dozen", buyConversion = 12.0, sellUnits = mapOf("pcs" to 3.0), buyPrice = 110.0, sellPrice = 160.0, stock = 0.0, lowStockLimit = 5.0, isDemo = true)
        )
        ShopType.ELECTRONICS -> listOf(
            Product(id = "demo_el_1", storeId = "demo_store", name = "LED Bulb 12W Cool White", nameBn = "এলইডি বাল্ব ১২ ওয়াট", barcode = "894101", category = "Lighting", unit = "pcs", buyUnit = "carton", buyConversion = 20.0, buyPrice = 110.0, sellPrice = 150.0, stock = 40.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_el_2", storeId = "demo_store", name = "Two-Core Power Cable", nameBn = "টু-কোর পাওয়ার কেবল", barcode = "894102", category = "Cables & Wires", unit = "meter", buyUnit = "roll", buyConversion = 100.0, sellUnits = mapOf("feet" to 3.2808), buyPrice = 28.0, sellPrice = 38.0, stock = 150.0, lowStockLimit = 25.0, isDemo = true),
            Product(id = "demo_el_3", storeId = "demo_store", name = "USB-C Fast Charger 20W", nameBn = "ইউএসবি-সি ফাস্ট চার্জার", barcode = "894103", category = "Mobile Accessories", unit = "pcs", buyUnit = "box", buyConversion = 10.0, buyPrice = 340.0, sellPrice = 480.0, stock = 18.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_el_4", storeId = "demo_store", name = "AA Alkaline Battery Pack", nameBn = "এএ ব্যাটারি", barcode = "894104", category = "Batteries", unit = "pcs", buyUnit = "box", buyConversion = 24.0, buyPrice = 25.0, sellPrice = 35.0, stock = 48.0, lowStockLimit = 12.0, isDemo = true),
            Product(id = "demo_el_5", storeId = "demo_store", name = "4-Socket Extension Board", nameBn = "মাল্টিপ্লাগ এক্সটেনশন", barcode = "894105", category = "Switches & Sockets", unit = "pcs", buyUnit = "carton", buyConversion = 10.0, buyPrice = 220.0, sellPrice = 310.0, stock = 20.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_el_6", storeId = "demo_store", name = "Wired Earphones 3.5mm", nameBn = "ইয়ারফোন", barcode = "894106", category = "Mobile Accessories", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 145.0, sellPrice = 220.0, stock = 22.0, lowStockLimit = 6.0, isDemo = true),
            Product(id = "demo_el_7", storeId = "demo_store", name = "LED Rechargeable Torch", nameBn = "রিচার্জেবল টর্চ", barcode = "894107", category = "Lighting", unit = "pcs", buyUnit = "box", buyConversion = 6.0, buyPrice = 260.0, sellPrice = 360.0, stock = 2.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_el_8", storeId = "demo_store", name = "USB Type-C Data Cable", nameBn = "টাইপ-সি ডাটা কেবল", barcode = "894108", category = "Cables & Wires", unit = "pcs", buyUnit = "box", buyConversion = 20.0, buyPrice = 95.0, sellPrice = 150.0, stock = 0.0, lowStockLimit = 8.0, isDemo = true)
        )
        ShopType.STATIONERY -> listOf(
            Product(id = "demo_st_1", storeId = "demo_store", name = "Gel Ballpoint Pen", nameBn = "জেল বলপেন", barcode = "895101", category = "Pens & Pencils", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 8.0, sellPrice = 12.0, stock = 120.0, lowStockLimit = 24.0, isDemo = true),
            Product(id = "demo_st_2", storeId = "demo_store", name = "A4 Copy Paper 500 Sheets", nameBn = "এ৪ কাগজ রিম", barcode = "895102", category = "Office Supplies", unit = "pack", buyUnit = "carton", buyConversion = 5.0, sellUnits = mapOf("pcs" to 500.0), buyPrice = 420.0, sellPrice = 480.0, stock = 20.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_st_3", storeId = "demo_store", name = "Hardcover Notebook 200p", nameBn = "হার্ডকভার খাতা", barcode = "895103", category = "Notebooks & Paper", unit = "pcs", buyUnit = "dozen", buyConversion = 12.0, buyPrice = 65.0, sellPrice = 90.0, stock = 36.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_st_4", storeId = "demo_store", name = "Color Pencil 24 Shades", nameBn = "রং পেন্সিল ২৪ রঙ", barcode = "895104", category = "Art & Craft", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 135.0, sellPrice = 180.0, stock = 18.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_st_5", storeId = "demo_store", name = "Scientific Calculator", nameBn = "সায়েন্টিফিক ক্যালকুলেটর", barcode = "895105", category = "School Supplies", unit = "pcs", buyUnit = "box", buyConversion = 10.0, buyPrice = 480.0, sellPrice = 620.0, stock = 15.0, lowStockLimit = 4.0, isDemo = true),
            Product(id = "demo_st_6", storeId = "demo_store", name = "Whiteboard Marker Pack", nameBn = "হোয়াইটবোর্ড মার্কার", barcode = "895106", category = "Pens & Pencils", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 28.0, sellPrice = 40.0, stock = 42.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_st_7", storeId = "demo_store", name = "Stapler Pin Box No.10", nameBn = "স্ট্যাপলার পিন বক্স", barcode = "895107", category = "Office Supplies", unit = "box", buyUnit = "pack", buyConversion = 20.0, buyPrice = 18.0, sellPrice = 28.0, stock = 4.0, lowStockLimit = 8.0, isDemo = true),
            Product(id = "demo_st_8", storeId = "demo_store", name = "Glue Stick 25g", nameBn = "গ্লু স্টিক", barcode = "895108", category = "Art & Craft", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 32.0, sellPrice = 45.0, stock = 0.0, lowStockLimit = 6.0, isDemo = true)
        )
        ShopType.GENERAL -> listOf(
            Product(id = "demo_gn_1", storeId = "demo_store", name = "Basmati Rice 1kg", nameBn = "বাসমতি চাল", barcode = "896101", category = "Daily Essentials", unit = "kg", buyUnit = "sack", buyConversion = 25.0, sellUnits = mapOf("gram" to 1000.0), buyPrice = 110.0, sellPrice = 135.0, stock = 50.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_gn_2", storeId = "demo_store", name = "Mineral Water 500ml", nameBn = "মিনারেল ওয়াটার", barcode = "896102", category = "Beverages", unit = "pcs", buyUnit = "carton", buyConversion = 24.0, buyPrice = 14.0, sellPrice = 20.0, stock = 48.0, lowStockLimit = 12.0, isDemo = true),
            Product(id = "demo_gn_3", storeId = "demo_store", name = "Dishwash Liquid 500ml", nameBn = "ডিশওয়াশ লিকুইড", barcode = "896103", category = "Household", unit = "pcs", buyUnit = "box", buyConversion = 12.0, buyPrice = 115.0, sellPrice = 140.0, stock = 22.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_gn_4", storeId = "demo_store", name = "Toilet Tissue Roll", nameBn = "টিস্যু রোল", barcode = "896104", category = "Personal Care", unit = "pcs", buyUnit = "pack", buyConversion = 10.0, buyPrice = 28.0, sellPrice = 38.0, stock = 30.0, lowStockLimit = 8.0, isDemo = true),
            Product(id = "demo_gn_5", storeId = "demo_store", name = "Cream Biscuit Family Pack", nameBn = "ক্রিম বিস্কুট", barcode = "896105", category = "Snacks", unit = "pack", buyUnit = "carton", buyConversion = 20.0, buyPrice = 42.0, sellPrice = 55.0, stock = 35.0, lowStockLimit = 10.0, isDemo = true),
            Product(id = "demo_gn_6", storeId = "demo_store", name = "Cooking Coconut Oil 200ml", nameBn = "নারিকেল তেল ২০০ মি.লি.", barcode = "896106", category = "Personal Care", unit = "pcs", buyUnit = "box", buyConversion = 12.0, sellUnits = mapOf("ml" to 200.0), buyPrice = 95.0, sellPrice = 120.0, stock = 20.0, lowStockLimit = 5.0, isDemo = true),
            Product(id = "demo_gn_7", storeId = "demo_store", name = "Detergent Powder 500g", nameBn = "ডিটারজেন্ট ৫০০ গ্রাম", barcode = "896107", category = "Household", unit = "pack", buyUnit = "sack", buyConversion = 20.0, buyPrice = 68.0, sellPrice = 85.0, stock = 3.0, lowStockLimit = 8.0, isDemo = true),
            Product(id = "demo_gn_8", storeId = "demo_store", name = "AA Dry Cell Battery", nameBn = "ব্যাটারি", barcode = "896108", category = "Daily Essentials", unit = "pcs", buyUnit = "box", buyConversion = 24.0, buyPrice = 20.0, sellPrice = 30.0, stock = 0.0, lowStockLimit = 10.0, isDemo = true)
        )
    }
}
