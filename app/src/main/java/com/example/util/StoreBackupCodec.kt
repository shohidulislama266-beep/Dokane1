package com.example.util

import com.example.data.model.Account
import com.example.data.model.AccountAdjustment
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
import com.example.data.model.StoreBackupCounters
import com.example.data.model.StoreBackupPayload
import com.example.data.model.StoreBackupSettings
import com.example.data.model.StoreBackupUnits
import com.example.data.model.StoreCategory
import com.example.data.model.StoreNote
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.domain.ShopUnitPreset
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object StoreBackupCodec {

    fun buildAutoBackupFileName(storeName: String): String {
        val safeName = storeName
            .trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "StoreOS" }
            .take(24)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${safeName}_autobackup_${stamp}.json"
    }

    fun encodeToJson(payload: StoreBackupPayload): String {
        val root = JSONObject()
        root.put("format", payload.format)
        root.put("version", payload.version)
        root.put("exportedAtMillis", payload.exportedAtMillis)
        root.put("shopType", payload.shopType)

        root.put("store", JSONObject().apply {
            put("id", payload.store.id)
            put("name", payload.store.name)
            put("phone", payload.store.phone)
            put("address", payload.store.address)
            put("shopType", payload.store.shopType)
            put("ownerId", payload.store.ownerId)
            put("productCategories", JSONArray(payload.store.productCategories))
        })

        root.put("storeSettings", JSONObject().apply {
            put("name", payload.storeSettings.name)
            put("phone", payload.storeSettings.phone)
            put("address", payload.storeSettings.address)
            put("shopType", payload.storeSettings.shopType)
            put("language", payload.storeSettings.language)
            put("isSimpleMode", payload.storeSettings.isSimpleMode)
            put("photoStorageMode", payload.storeSettings.photoStorageMode)
        })

        root.put("productCategories", JSONArray(payload.productCategories))
        root.put("categoryRecords", JSONArray().apply {
            payload.categoryRecords.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("storeId", c.storeId)
                    put("type", c.type)
                    put("name", c.name)
                    put("isArchived", c.isArchived)
                    put("createdBy", c.createdBy)
                })
            }
        })

        root.put("units", JSONObject().apply {
            put("presets", JSONArray().apply {
                payload.units.presets.forEach { preset ->
                    put(JSONObject().apply {
                        put("id", preset.id)
                        put("labelEn", preset.labelEn)
                        put("labelBn", preset.labelBn)
                        put("category", preset.category)
                        put("stockUnit", preset.stockUnit)
                        put("buyUnit", preset.buyUnit)
                        put("buyConversion", preset.buyConversion)
                        put("sellUnits", JSONObject().apply {
                            preset.sellUnits.forEach { (k, v) -> put(k, v) }
                        })
                    })
                }
            })
            put("allUnits", JSONArray(payload.units.allUnits))
        })

        root.put("counters", JSONObject().apply {
            put("invoiceSeq", payload.counters.invoiceSeq)
            put("paymentSeq", payload.counters.paymentSeq)
            put("purchaseSeq", payload.counters.purchaseSeq)
            put("returnSeq", payload.counters.returnSeq)
        })

        root.put("products", JSONArray().apply {
            payload.products.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("storeId", p.storeId)
                    put("name", p.name)
                    put("nameBn", p.nameBn)
                    put("barcode", p.barcode)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("buyUnit", p.buyUnit)
                    put("buyConversion", p.buyConversion)
                    put("sellUnits", JSONObject().apply {
                        p.sellUnits.forEach { (k, v) -> put(k, v) }
                    })
                    put("buyPrice", p.buyPrice)
                    put("sellPrice", p.sellPrice)
                    put("wholesalePrice", p.wholesalePrice)
                    put("mrp", p.mrp)
                    put("itemLocation", p.itemLocation)
                    put("partyRate", p.partyRate)
                    put("stock", p.stock)
                    put("lowStockLimit", p.lowStockLimit)
                    put("photoUri", p.photoUri)
                    put("createdBy", p.createdBy)
                })
            }
        })

        root.put("customers", JSONArray().apply {
            payload.customers.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("storeId", c.storeId)
                    put("name", c.name)
                    put("mobile", c.mobile)
                    put("address", c.address)
                    put("notes", c.notes)
                    put("currentBalance", c.currentBalance)
                    put("createdBy", c.createdBy)
                })
            }
        })

        root.put("suppliers", JSONArray().apply {
            payload.suppliers.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("storeId", s.storeId)
                    put("name", s.name)
                    put("mobile", s.mobile)
                    put("company", s.company)
                    put("address", s.address)
                    put("balance", s.balance)
                    put("createdBy", s.createdBy)
                })
            }
        })

        root.put("sales", JSONArray().apply {
            payload.sales.forEach { sale ->
                put(JSONObject().apply {
                    put("id", sale.id)
                    put("storeId", sale.storeId)
                    put("invoiceNo", sale.invoiceNo)
                    put("totalItems", sale.totalItems)
                    put("subtotal", sale.subtotal)
                    put("discount", sale.discount)
                    put("grandTotal", sale.grandTotal)
                    put("profit", sale.profit)
                    put("paymentType", sale.paymentType)
                    put("customerId", sale.customerId)
                    put("customerName", sale.customerName)
                    put("customerPhone", sale.customerPhone)
                    put("accountId", sale.accountId)
                    put("accountName", sale.accountName)
                    put("notes", sale.notes)
                    put("soldBy", sale.soldBy)
                    put("createdAtMillis", sale.createdAt?.toDate()?.time ?: 0L)
                    put("items", JSONArray().apply {
                        sale.items.forEach { item ->
                            put(JSONObject().apply {
                                put("productId", item.productId)
                                put("name", item.name)
                                put("qty", item.qty)
                                put("unit", item.unit)
                                put("stockQty", item.stockQty)
                                put("buyPrice", item.buyPrice)
                                put("sellPrice", item.sellPrice)
                                put("discount", item.discount)
                                put("lineTotal", item.lineTotal)
                                put("profit", item.profit)
                            })
                        }
                    })
                })
            }
        })

        root.put("purchases", JSONArray().apply {
            payload.purchases.forEach { pur ->
                put(JSONObject().apply {
                    put("id", pur.id)
                    put("storeId", pur.storeId)
                    put("purchaseNo", pur.purchaseNo)
                    put("supplierId", pur.supplierId)
                    put("supplierName", pur.supplierName)
                    put("supplierMobile", pur.supplierMobile)
                    put("totalItems", pur.totalItems)
                    put("totalAmount", pur.totalAmount)
                    put("paidAmount", pur.paidAmount)
                    put("dueAmount", pur.dueAmount)
                    put("paymentType", pur.paymentType)
                    put("accountId", pur.accountId)
                    put("accountName", pur.accountName)
                    put("note", pur.note)
                    put("createdBy", pur.createdBy)
                    put("createdAtMillis", pur.createdAt?.toDate()?.time ?: 0L)
                    put("items", JSONArray().apply {
                        pur.items.forEach { item ->
                            put(JSONObject().apply {
                                put("productId", item.productId)
                                put("name", item.name)
                                put("qty", item.qty)
                                put("unit", item.unit)
                                put("stockQty", item.stockQty)
                                put("conversionFactor", item.conversionFactor)
                                put("costPerStockUnit", item.costPerStockUnit)
                                put("buyPrice", item.buyPrice)
                                put("lineTotal", item.lineTotal)
                            })
                        }
                    })
                })
            }
        })

        root.put("purchaseReturns", JSONArray().apply {
            payload.purchaseReturns.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("storeId", r.storeId)
                    put("returnNo", r.returnNo)
                    put("purchaseId", r.purchaseId)
                    put("supplierId", r.supplierId)
                    put("supplierName", r.supplierName)
                    put("amount", r.amount)
                    put("reason", r.reason)
                    put("createdBy", r.createdBy)
                    put("createdAtMillis", r.createdAt?.toDate()?.time ?: 0L)
                })
            }
        })

        root.put("payments", JSONArray().apply {
            payload.payments.forEach { pay ->
                put(JSONObject().apply {
                    put("id", pay.id)
                    put("storeId", pay.storeId)
                    put("paymentNo", pay.paymentNo)
                    put("customerId", pay.customerId)
                    put("customerName", pay.customerName)
                    put("customerMobile", pay.customerMobile)
                    put("amount", pay.amount)
                    put("paymentMethod", pay.paymentMethod)
                    put("accountId", pay.accountId)
                    put("accountName", pay.accountName)
                    put("paymentDate", pay.paymentDate)
                    put("note", pay.note)
                    put("receivedBy", pay.receivedBy)
                    put("createdAtMillis", pay.createdAt?.toDate()?.time ?: pay.paymentDate)
                })
            }
        })

        root.put("supplierPayments", JSONArray().apply {
            payload.supplierPayments.forEach { sp ->
                put(JSONObject().apply {
                    put("id", sp.id)
                    put("storeId", sp.storeId)
                    put("paymentNo", sp.paymentNo)
                    put("supplierId", sp.supplierId)
                    put("supplierName", sp.supplierName)
                    put("amount", sp.amount)
                    put("accountId", sp.accountId)
                    put("accountName", sp.accountName)
                    put("paymentDate", sp.paymentDate)
                    put("note", sp.note)
                    put("paidBy", sp.paidBy)
                    put("createdAtMillis", sp.createdAt?.toDate()?.time ?: sp.paymentDate)
                })
            }
        })

        root.put("accounts", JSONArray().apply {
            payload.accounts.forEach { acc ->
                put(JSONObject().apply {
                    put("id", acc.id)
                    put("storeId", acc.storeId)
                    put("name", acc.name)
                    put("type", acc.type)
                    put("accountNumber", acc.accountNumber)
                    put("openingBalance", acc.openingBalance)
                    put("isDefault", acc.isDefault)
                    put("isArchived", acc.isArchived)
                    put("createdBy", acc.createdBy)
                })
            }
        })

        root.put("accountAdjustments", JSONArray().apply {
            payload.accountAdjustments.forEach { adj ->
                put(JSONObject().apply {
                    put("id", adj.id)
                    put("storeId", adj.storeId)
                    put("accountId", adj.accountId)
                    put("accountName", adj.accountName)
                    put("oldBalance", adj.oldBalance)
                    put("newBalance", adj.newBalance)
                    put("difference", adj.difference)
                    put("note", adj.note)
                    put("adjustedBy", adj.adjustedBy)
                    put("createdAtMillis", adj.createdAt?.toDate()?.time ?: 0L)
                })
            }
        })

        root.put("expenses", JSONArray().apply {
            payload.expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("storeId", e.storeId)
                    put("amount", e.amount)
                    put("category", e.category)
                    put("accountId", e.accountId)
                    put("accountName", e.accountName)
                    put("date", e.date)
                    put("note", e.note)
                    put("receiptUri", e.receiptUri)
                    put("createdBy", e.createdBy)
                    put("isDeleted", e.isDeleted)
                    put("deletedNote", e.deletedNote)
                    put("deletedBy", e.deletedBy)
                    put("createdAtMillis", e.createdAt?.toDate()?.time ?: e.date)
                })
            }
        })

        root.put("otherIncomes", JSONArray().apply {
            payload.otherIncomes.forEach { inc ->
                put(JSONObject().apply {
                    put("id", inc.id)
                    put("storeId", inc.storeId)
                    put("amount", inc.amount)
                    put("category", inc.category)
                    put("accountId", inc.accountId)
                    put("accountName", inc.accountName)
                    put("date", inc.date)
                    put("note", inc.note)
                    put("createdBy", inc.createdBy)
                    put("isDeleted", inc.isDeleted)
                    put("deletedNote", inc.deletedNote)
                    put("deletedBy", inc.deletedBy)
                    put("createdAtMillis", inc.createdAt?.toDate()?.time ?: inc.date)
                })
            }
        })

        root.put("notes", JSONArray().apply {
            payload.notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("storeId", n.storeId)
                    put("title", n.title)
                    put("text", n.text)
                    put("date", n.date)
                    put("createdBy", n.createdBy)
                })
            }
        })

        return root.toString()
    }

    fun decodeAndValidateJson(rawJson: String, lang: AppLanguage): StoreBackupPayload {
        if (rawJson.isBlank()) {
            throw IllegalArgumentException(ShopLocale.get("backup_err_empty_file", lang))
        }
        val root = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            throw IllegalArgumentException(ShopLocale.get("backup_err_damaged_json", lang))
        }

        val format = root.optString("format", "")
        if (format != StoreBackupPayload.FORMAT_ID) {
            throw IllegalArgumentException(ShopLocale.get("backup_err_wrong_file", lang))
        }
        if (!root.has("version")) {
            throw IllegalArgumentException(ShopLocale.get("backup_err_missing_version", lang))
        }
        val version = root.optInt("version", 0)
        if (version <= 0) {
            throw IllegalArgumentException(ShopLocale.get("backup_err_missing_version", lang))
        }
        if (version > StoreBackupPayload.CURRENT_VERSION) {
            throw IllegalArgumentException(ShopLocale.format("backup_err_unsupported_version", lang, version))
        }
        val storeObj = root.optJSONObject("store")
            ?: throw IllegalArgumentException(ShopLocale.get("backup_err_damaged_structure", lang))

        val settingsObj = root.optJSONObject("storeSettings") ?: JSONObject()
        val countersObj = root.optJSONObject("counters") ?: JSONObject()
        val unitsObj = root.optJSONObject("units") ?: JSONObject()

        val storeCategories = stringListFromJson(storeObj.optJSONArray("productCategories"))
        val topCategories = stringListFromJson(root.optJSONArray("productCategories"))
            .ifEmpty { storeCategories }

        val store = Store(
            id = storeObj.optString("id", ""),
            name = storeObj.optString("name", settingsObj.optString("name", "")),
            phone = storeObj.optString("phone", settingsObj.optString("phone", "")),
            address = storeObj.optString("address", settingsObj.optString("address", "")),
            shopType = storeObj.optString("shopType", root.optString("shopType", "")),
            productCategories = storeCategories,
            ownerId = storeObj.optString("ownerId", "")
        )

        val settings = StoreBackupSettings(
            name = settingsObj.optString("name", store.name),
            phone = settingsObj.optString("phone", store.phone),
            address = settingsObj.optString("address", store.address),
            shopType = settingsObj.optString("shopType", store.shopType),
            language = settingsObj.optString("language", AppLanguage.EN.code),
            isSimpleMode = settingsObj.optBoolean("isSimpleMode", false),
            photoStorageMode = settingsObj.optString("photoStorageMode", PhotoStorageMode.SYNC.prefValue)
        )

        val presets = mutableListOf<ShopUnitPreset>()
        unitsObj.optJSONArray("presets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val sellUnitsObj = o.optJSONObject("sellUnits")
                val sellUnitsMap = mutableMapOf<String, Double>()
                sellUnitsObj?.keys()?.forEach { k ->
                    val v = sellUnitsObj.optDouble(k, 1.0)
                    if (v > 0.0) sellUnitsMap[k] = v
                }
                val legacySellUnit = o.optString("sellUnit", "")
                if (sellUnitsMap.isEmpty() && legacySellUnit.isNotBlank()) {
                    sellUnitsMap[legacySellUnit] = o.optDouble("factor", 1.0)
                }
                val stockUnit = o.optString("stockUnit", "").ifBlank { legacySellUnit.ifBlank { "pcs" } }
                val buyUnit = o.optString("buyUnit", stockUnit)
                presets.add(
                    ShopUnitPreset(
                        id = o.optString("id", "preset_${i + 1}"),
                        labelEn = o.optString("labelEn", "$buyUnit -> $stockUnit"),
                        labelBn = o.optString("labelBn", "$buyUnit -> $stockUnit"),
                        category = o.optString("category", "General"),
                        stockUnit = stockUnit,
                        buyUnit = buyUnit,
                        buyConversion = o.optDouble("buyConversion", o.optDouble("factor", 1.0)),
                        sellUnits = sellUnitsMap
                    )
                )
            }
        }
        val units = StoreBackupUnits(
            presets = presets,
            allUnits = stringListFromJson(unitsObj.optJSONArray("allUnits"))
        )

        val counters = StoreBackupCounters(
            invoiceSeq = countersObj.optLong("invoiceSeq", 100L),
            paymentSeq = countersObj.optLong("paymentSeq", 0L),
            purchaseSeq = countersObj.optLong("purchaseSeq", 0L),
            returnSeq = countersObj.optLong("returnSeq", 0L)
        )

        val categoryRecords = parseSection(root, "categoryRecords", lang) { o ->
            StoreCategory(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                type = o.optString("type", "expense"),
                name = o.optString("name", ""),
                isArchived = o.optBoolean("isArchived", false),
                createdBy = o.optString("createdBy", "")
            )
        }

        val products = parseSection(root, "products", lang) { o ->
            val sellUnitsObj = o.optJSONObject("sellUnits")
            val sellUnitsMap = buildMap<String, Double> {
                sellUnitsObj?.keys()?.forEach { k ->
                    put(k, sellUnitsObj.optDouble(k, 0.0))
                }
            }
            Product(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                name = o.optString("name", ""),
                nameBn = o.optString("nameBn", ""),
                barcode = o.optString("barcode", ""),
                category = o.optString("category", "General"),
                unit = o.optString("unit", "pcs"),
                buyUnit = o.optString("buyUnit", ""),
                buyConversion = o.optDouble("buyConversion", 1.0),
                sellUnits = sellUnitsMap,
                buyPrice = o.optDouble("buyPrice", 0.0),
                sellPrice = o.optDouble("sellPrice", 0.0),
                wholesalePrice = o.optDouble("wholesalePrice", 0.0),
                mrp = o.optDouble("mrp", 0.0),
                itemLocation = o.optString("itemLocation", ""),
                partyRate = o.optDouble("partyRate", 0.0),
                stock = o.optDouble("stock", 0.0),
                lowStockLimit = o.optDouble("lowStockLimit", 5.0),
                photoUri = o.optString("photoUri", ""),
                createdBy = o.optString("createdBy", "")
            )
        }

        val customers = parseSection(root, "customers", lang) { o ->
            Customer(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                name = o.optString("name", ""),
                mobile = o.optString("mobile", ""),
                address = o.optString("address", ""),
                notes = o.optString("notes", ""),
                currentBalance = o.optDouble("currentBalance", 0.0),
                createdBy = o.optString("createdBy", "")
            )
        }

        val suppliers = parseSection(root, "suppliers", lang) { o ->
            Supplier(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                name = o.optString("name", ""),
                mobile = o.optString("mobile", ""),
                company = o.optString("company", ""),
                address = o.optString("address", ""),
                balance = o.optDouble("balance", 0.0),
                createdBy = o.optString("createdBy", "")
            )
        }

        val sales = parseSection(root, "sales", lang) { o ->
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsArr.length()).mapNotNull { idx ->
                val itObj = itemsArr.optJSONObject(idx) ?: return@mapNotNull null
                SaleItem(
                    productId = itObj.optString("productId", ""),
                    name = itObj.optString("name", ""),
                    qty = itObj.optDouble("qty", 0.0),
                    unit = itObj.optString("unit", "pcs"),
                    stockQty = itObj.optDouble("stockQty", 0.0),
                    buyPrice = itObj.optDouble("buyPrice", 0.0),
                    sellPrice = itObj.optDouble("sellPrice", 0.0),
                    discount = itObj.optDouble("discount", 0.0),
                    lineTotal = itObj.optDouble("lineTotal", 0.0),
                    profit = itObj.optDouble("profit", 0.0)
                )
            }
            val createdMs = o.optLong("createdAtMillis", 0L)
            Sale(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                invoiceNo = o.optString("invoiceNo", ""),
                items = items,
                totalItems = o.optDouble("totalItems", 0.0),
                subtotal = o.optDouble("subtotal", 0.0),
                discount = o.optDouble("discount", 0.0),
                grandTotal = o.optDouble("grandTotal", 0.0),
                profit = o.optDouble("profit", 0.0),
                paymentType = o.optString("paymentType", "cash"),
                customerId = o.optString("customerId", ""),
                customerName = o.optString("customerName", "Cash Sale"),
                customerPhone = o.optString("customerPhone", ""),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                notes = o.optString("notes", ""),
                soldBy = o.optString("soldBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val purchases = parseSection(root, "purchases", lang) { o ->
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsArr.length()).mapNotNull { idx ->
                val itObj = itemsArr.optJSONObject(idx) ?: return@mapNotNull null
                PurchaseItem(
                    productId = itObj.optString("productId", ""),
                    name = itObj.optString("name", ""),
                    qty = itObj.optDouble("qty", 0.0),
                    unit = itObj.optString("unit", "pcs"),
                    stockQty = itObj.optDouble("stockQty", 0.0),
                    conversionFactor = itObj.optDouble("conversionFactor", 1.0),
                    costPerStockUnit = itObj.optDouble("costPerStockUnit", 0.0),
                    buyPrice = itObj.optDouble("buyPrice", 0.0),
                    lineTotal = itObj.optDouble("lineTotal", 0.0)
                )
            }
            val createdMs = o.optLong("createdAtMillis", 0L)
            Purchase(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                purchaseNo = o.optString("purchaseNo", ""),
                supplierId = o.optString("supplierId", ""),
                supplierName = o.optString("supplierName", ""),
                supplierMobile = o.optString("supplierMobile", ""),
                items = items,
                totalItems = o.optDouble("totalItems", 0.0),
                totalAmount = o.optDouble("totalAmount", 0.0),
                paidAmount = o.optDouble("paidAmount", 0.0),
                dueAmount = o.optDouble("dueAmount", 0.0),
                paymentType = o.optString("paymentType", "paid"),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                note = o.optString("note", ""),
                createdBy = o.optString("createdBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val purchaseReturns = parseSection(root, "purchaseReturns", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            PurchaseReturn(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                returnNo = o.optString("returnNo", ""),
                purchaseId = o.optString("purchaseId", ""),
                supplierId = o.optString("supplierId", ""),
                supplierName = o.optString("supplierName", ""),
                amount = o.optDouble("amount", 0.0),
                reason = o.optString("reason", ""),
                createdBy = o.optString("createdBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val payments = parseSection(root, "payments", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            Payment(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                paymentNo = o.optString("paymentNo", ""),
                customerId = o.optString("customerId", ""),
                customerName = o.optString("customerName", ""),
                customerMobile = o.optString("customerMobile", ""),
                amount = o.optDouble("amount", 0.0),
                paymentMethod = o.optString("paymentMethod", "cash"),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                paymentDate = o.optLong("paymentDate", 0L),
                note = o.optString("note", ""),
                receivedBy = o.optString("receivedBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val supplierPayments = parseSection(root, "supplierPayments", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            SupplierPayment(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                paymentNo = o.optString("paymentNo", ""),
                supplierId = o.optString("supplierId", ""),
                supplierName = o.optString("supplierName", ""),
                amount = o.optDouble("amount", 0.0),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                paymentDate = o.optLong("paymentDate", 0L),
                note = o.optString("note", ""),
                paidBy = o.optString("paidBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val accounts = parseSection(root, "accounts", lang) { o ->
            Account(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                name = o.optString("name", ""),
                type = o.optString("type", "Cash"),
                accountNumber = o.optString("accountNumber", ""),
                openingBalance = o.optDouble("openingBalance", 0.0),
                isDefault = o.optBoolean("isDefault", false),
                isArchived = o.optBoolean("isArchived", false),
                createdBy = o.optString("createdBy", "")
            )
        }

        val accountAdjustments = parseSection(root, "accountAdjustments", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            AccountAdjustment(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                accountId = o.optString("accountId", ""),
                accountName = o.optString("accountName", ""),
                oldBalance = o.optDouble("oldBalance", 0.0),
                newBalance = o.optDouble("newBalance", 0.0),
                difference = o.optDouble("difference", 0.0),
                note = o.optString("note", ""),
                adjustedBy = o.optString("adjustedBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val expenses = parseSection(root, "expenses", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            Expense(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                amount = o.optDouble("amount", 0.0),
                category = o.optString("category", "Other"),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                date = o.optLong("date", 0L),
                note = o.optString("note", ""),
                receiptUri = o.optString("receiptUri", ""),
                createdBy = o.optString("createdBy", ""),
                isDeleted = o.optBoolean("isDeleted", false),
                deletedNote = o.optString("deletedNote", ""),
                deletedBy = o.optString("deletedBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val otherIncomes = parseSection(root, "otherIncomes", lang) { o ->
            val createdMs = o.optLong("createdAtMillis", 0L)
            OtherIncome(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                amount = o.optDouble("amount", 0.0),
                category = o.optString("category", "Other"),
                accountId = o.optString("accountId", "cash_in_hand"),
                accountName = o.optString("accountName", "Cash in Hand"),
                date = o.optLong("date", 0L),
                note = o.optString("note", ""),
                createdBy = o.optString("createdBy", ""),
                isDeleted = o.optBoolean("isDeleted", false),
                deletedNote = o.optString("deletedNote", ""),
                deletedBy = o.optString("deletedBy", ""),
                createdAt = createdMs.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
            )
        }

        val notes = parseSection(root, "notes", lang) { o ->
            StoreNote(
                id = o.optString("id", ""),
                storeId = o.optString("storeId", ""),
                title = o.optString("title", ""),
                text = o.optString("text", ""),
                date = o.optLong("date", 0L),
                createdBy = o.optString("createdBy", "")
            )
        }

        return StoreBackupPayload(
            format = format,
            version = version,
            exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis()),
            store = store,
            storeSettings = settings,
            shopType = root.optString("shopType", store.shopType),
            productCategories = topCategories,
            categoryRecords = categoryRecords,
            units = units,
            counters = counters,
            products = products,
            customers = customers,
            suppliers = suppliers,
            sales = sales,
            purchases = purchases,
            purchaseReturns = purchaseReturns,
            payments = payments,
            supplierPayments = supplierPayments,
            accounts = accounts,
            accountAdjustments = accountAdjustments,
            expenses = expenses,
            otherIncomes = otherIncomes,
            notes = notes
        )
    }

    private fun stringListFromJson(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx, "").trim().takeIf { it.isNotEmpty() }
        }
    }

    private inline fun <T> parseSection(
        root: JSONObject,
        key: String,
        lang: AppLanguage,
        mapper: (JSONObject) -> T
    ): List<T> {
        if (!root.has(key)) return emptyList()
        val arr = root.optJSONArray(key)
            ?: throw IllegalArgumentException(ShopLocale.format("backup_err_corrupt_section", lang, key))
        return try {
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                mapper(obj)
            }
        } catch (_: Exception) {
            throw IllegalArgumentException(ShopLocale.format("backup_err_corrupt_section", lang, key))
        }
    }
}
