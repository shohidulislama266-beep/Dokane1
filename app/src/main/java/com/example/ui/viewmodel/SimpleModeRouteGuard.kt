package com.example.ui.viewmodel

import android.content.Intent
import com.example.ui.components.ShopNavTab

/**
 * Centralized Simple Mode route guard.
 *
 * Enforces that when `isSimpleMode && !isFatherMode`:
 * - Only HOME, SELL, BAKI, and STOCK tabs can be displayed (when permitted).
 * - All hidden Business / More / History / Settings / AI key / Backup / Data Health / Reports /
 *   Staff / Categories / Cash & Bank routes are blocked and redirected to Simple Home (`ShopNavTab.HOME`).
 * - Detailed Mode and Father Mode retain their existing routing behavior unchanged.
 */
object SimpleModeRouteGuard {

    const val EXTRA_NAV_TAB = "extra_nav_tab"
    const val EXTRA_NAV_DESTINATION = "extra_nav_destination"
    const val EXTRA_OPEN_SETTINGS = "extra_open_settings"
    const val EXTRA_OPEN_AI_KEY = "extra_open_ai_key"
    const val EXTRA_OPEN_BACKUP = "extra_open_backup"
    const val EXTRA_OPEN_LOW_STOCK = "extra_open_low_stock"
    const val EXTRA_OPEN_BAKI = "extra_open_baki"

    enum class GuardedModal {
        NONE,
        NOTIFICATIONS,
        PAYMENT_IN,
        PAYMENT_OUT,
        PURCHASE,
        EXPENSE,
        ADD_PARTY,
        CONFIRM_DETAILED_MODE,
        FATHER_LOCK_INFO,
        SETTINGS,
        AI_KEY,
        BACKUP
    }

    sealed class GuardedIntentAction {
        data class Tab(val tab: ShopNavTab, val lowStockFilter: Boolean = false) : GuardedIntentAction()
        data class Destination(val destination: BusinessNavDestination) : GuardedIntentAction()
        data class Modal(val modal: GuardedModal) : GuardedIntentAction()
    }

    val ALLOWED_SIMPLE_TABS: Set<ShopNavTab> = setOf(
        ShopNavTab.HOME,
        ShopNavTab.SELL,
        ShopNavTab.BAKI,
        ShopNavTab.STOCK
    )

    fun isSimpleRestricted(isSimpleMode: Boolean, isFatherMode: Boolean): Boolean =
        isSimpleMode && !isFatherMode

    fun isTabAllowedInSimpleMode(
        tab: ShopNavTab,
        canSell: Boolean,
        canViewStock: Boolean,
        canAccessBaki: Boolean
    ): Boolean = when (tab) {
        ShopNavTab.HOME -> true
        ShopNavTab.SELL -> canSell
        ShopNavTab.BAKI -> canAccessBaki
        ShopNavTab.STOCK -> canViewStock
        ShopNavTab.HISTORY,
        ShopNavTab.BUSINESS,
        ShopNavTab.MORE -> false
    }

    fun resolveTab(
        requested: ShopNavTab,
        isSimpleMode: Boolean,
        isFatherMode: Boolean,
        canSell: Boolean,
        canViewStock: Boolean,
        canAccessBaki: Boolean
    ): ShopNavTab {
        if (!isSimpleRestricted(isSimpleMode, isFatherMode)) {
            return requested
        }
        return if (isTabAllowedInSimpleMode(requested, canSell, canViewStock, canAccessBaki)) {
            requested
        } else {
            ShopNavTab.HOME
        }
    }

    fun isDestinationAllowedInSimpleMode(
        @Suppress("UNUSED_PARAMETER") destination: BusinessNavDestination
    ): Boolean = false

    fun sanitizeBackStack(
        stack: List<BusinessNavDestination>,
        isSimpleMode: Boolean,
        isFatherMode: Boolean
    ): List<BusinessNavDestination> {
        return if (isSimpleRestricted(isSimpleMode, isFatherMode)) {
            listOf(BusinessNavDestination.DASHBOARD)
        } else {
            stack.ifEmpty { listOf(BusinessNavDestination.DASHBOARD) }
        }
    }

    fun isModalAllowed(
        modal: GuardedModal,
        isSimpleMode: Boolean,
        isFatherMode: Boolean
    ): Boolean {
        if (!isSimpleRestricted(isSimpleMode, isFatherMode)) return true
        return when (modal) {
            GuardedModal.NONE,
            GuardedModal.NOTIFICATIONS,
            GuardedModal.PAYMENT_IN -> true
            GuardedModal.PAYMENT_OUT,
            GuardedModal.PURCHASE,
            GuardedModal.EXPENSE,
            GuardedModal.ADD_PARTY,
            GuardedModal.CONFIRM_DETAILED_MODE,
            GuardedModal.FATHER_LOCK_INFO,
            GuardedModal.SETTINGS,
            GuardedModal.AI_KEY,
            GuardedModal.BACKUP -> false
        }
    }

    fun parseAppIntent(intent: Intent?): GuardedIntentAction? {
        if (intent == null) return null

        if (intent.getBooleanExtra(EXTRA_OPEN_AI_KEY, false)) {
            return GuardedIntentAction.Modal(GuardedModal.AI_KEY)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_BACKUP, false)) {
            return GuardedIntentAction.Modal(GuardedModal.BACKUP)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)) {
            return GuardedIntentAction.Modal(GuardedModal.SETTINGS)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_LOW_STOCK, false)) {
            return GuardedIntentAction.Tab(ShopNavTab.STOCK, lowStockFilter = true)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_BAKI, false)) {
            return GuardedIntentAction.Tab(ShopNavTab.BAKI)
        }

        intent.getStringExtra(EXTRA_NAV_DESTINATION)?.let { rawDest ->
            val dest = BusinessNavDestination.entries.firstOrNull {
                it.name.equals(rawDest.trim(), ignoreCase = true)
            }
            if (dest != null) return GuardedIntentAction.Destination(dest)
        }

        intent.getStringExtra(EXTRA_NAV_TAB)?.let { rawTab ->
            val tab = ShopNavTab.entries.firstOrNull {
                it.name.equals(rawTab.trim(), ignoreCase = true)
            }
            if (tab != null) return GuardedIntentAction.Tab(tab)
        }

        val dataUri = intent.data ?: return null
        val routeSegment = (dataUri.pathSegments?.firstOrNull() ?: dataUri.host)
            ?.trim()
            ?.lowercase()
            .orEmpty()

        return when (routeSegment) {
            "home" -> GuardedIntentAction.Tab(ShopNavTab.HOME)
            "sell" -> GuardedIntentAction.Tab(ShopNavTab.SELL)
            "baki", "customers" -> GuardedIntentAction.Tab(ShopNavTab.BAKI)
            "stock", "inventory" -> GuardedIntentAction.Tab(ShopNavTab.STOCK)
            "low-stock", "low_stock" -> GuardedIntentAction.Tab(ShopNavTab.STOCK, lowStockFilter = true)
            "history" -> GuardedIntentAction.Tab(ShopNavTab.HISTORY)
            "business" -> GuardedIntentAction.Tab(ShopNavTab.BUSINESS)
            "more" -> GuardedIntentAction.Tab(ShopNavTab.MORE)
            "settings" -> GuardedIntentAction.Modal(GuardedModal.SETTINGS)
            "ai-key", "ai_key" -> GuardedIntentAction.Modal(GuardedModal.AI_KEY)
            "backup", "checklist" -> GuardedIntentAction.Modal(GuardedModal.BACKUP)
            "staff" -> GuardedIntentAction.Destination(BusinessNavDestination.STAFF_MANAGEMENT_SCREEN)
            "categories" -> GuardedIntentAction.Destination(BusinessNavDestination.MANAGE_CATEGORIES_SCREEN)
            "cash-bank", "cash_bank" -> GuardedIntentAction.Destination(BusinessNavDestination.CASH_BANK_SCREEN)
            "reports" -> GuardedIntentAction.Destination(BusinessNavDestination.FINANCIAL_REPORTS_HUB)
            "monthly-report", "monthly_report" -> GuardedIntentAction.Destination(BusinessNavDestination.MONTHLY_REPORT)
            "data-health", "data_health" -> GuardedIntentAction.Destination(BusinessNavDestination.DATA_HEALTH_CHECK)
            else -> {
                val destMatch = BusinessNavDestination.entries.firstOrNull {
                    it.name.equals(routeSegment.replace("-", "_"), ignoreCase = true)
                }
                destMatch?.let { GuardedIntentAction.Destination(it) }
            }
        }
    }
}
