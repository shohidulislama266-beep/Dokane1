package com.example.util

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReceiptImageStyle(val prefValue: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPrefValue(raw: String?): ReceiptImageStyle {
            return if (raw.equals(DARK.prefValue, ignoreCase = true)) DARK else LIGHT
        }
    }
}

class StockPreferences(
    context: Context,
    prefsName: String = PREFS_NAME,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    data class PinVerifyOutcome(
        val success: Boolean,
        val isLocked: Boolean,
        val remainingLockSeconds: Int,
        val failedAttempts: Int
    )

    private val _photoStorageMode = MutableStateFlow(loadPhotoStorageMode())
    val photoStorageMode: StateFlow<PhotoStorageMode> = _photoStorageMode.asStateFlow()

    private val _receiptImageStyle = MutableStateFlow(loadReceiptImageStyle())
    val receiptImageStyle: StateFlow<ReceiptImageStyle> = _receiptImageStyle.asStateFlow()

    private val _hasOwnerPin = MutableStateFlow(hasOwnerPin())
    val hasOwnerPinFlow: StateFlow<Boolean> = _hasOwnerPin.asStateFlow()

    private val _lastUsedAccountId = MutableStateFlow(getLastUsedAccountId())
    val lastUsedAccountIdFlow: StateFlow<String> = _lastUsedAccountId.asStateFlow()

    private val _showProfitInSimpleMode = MutableStateFlow(getShowProfitInSimpleMode())
    val showProfitInSimpleModeFlow: StateFlow<Boolean> = _showProfitInSimpleMode.asStateFlow()

    private val _pinLockoutUntilMs = MutableStateFlow(prefs.getLong(KEY_PIN_LOCKOUT_UNTIL_MS, 0L))
    val pinLockoutUntilMsFlow: StateFlow<Long> = _pinLockoutUntilMs.asStateFlow()

    private fun loadPhotoStorageMode(): PhotoStorageMode {
        val raw = prefs.getString(KEY_PHOTO_STORAGE_MODE, PhotoStorageMode.SYNC.prefValue)
        return PhotoStorageMode.entries.firstOrNull { it.prefValue == raw } ?: PhotoStorageMode.SYNC
    }

    fun getPhotoStorageMode(): PhotoStorageMode = _photoStorageMode.value

    fun setPhotoStorageMode(mode: PhotoStorageMode) {
        prefs.edit().putString(KEY_PHOTO_STORAGE_MODE, mode.prefValue).apply()
        _photoStorageMode.value = mode
    }

    private fun loadReceiptImageStyle(): ReceiptImageStyle {
        val raw = prefs.getString(KEY_RECEIPT_IMAGE_STYLE, ReceiptImageStyle.LIGHT.prefValue)
        return ReceiptImageStyle.fromPrefValue(raw)
    }

    fun getReceiptImageStyle(): ReceiptImageStyle = _receiptImageStyle.value

    fun setReceiptImageStyle(style: ReceiptImageStyle) {
        prefs.edit().putString(KEY_RECEIPT_IMAGE_STYLE, style.prefValue).apply()
        _receiptImageStyle.value = style
    }

    fun getLastUsedAccountId(): String =
        prefs.getString(KEY_LAST_ACCOUNT_ID, "cash_in_hand").orEmpty().ifBlank { "cash_in_hand" }

    fun setLastUsedAccountId(accountId: String) {
        val clean = accountId.trim().ifBlank { "cash_in_hand" }
        prefs.edit().putString(KEY_LAST_ACCOUNT_ID, clean).apply()
        _lastUsedAccountId.value = clean
    }

    fun getShowProfitInSimpleMode(): Boolean =
        prefs.getBoolean(KEY_SHOW_PROFIT_IN_SIMPLE_MODE, false)

    fun setShowProfitInSimpleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PROFIT_IN_SIMPLE_MODE, enabled).apply()
        _showProfitInSimpleMode.value = enabled
    }

    fun hasOwnerPin(): Boolean = !prefs.getString(KEY_OWNER_PIN_HASH, null).isNullOrBlank()

    fun setOwnerPin(pin: String) {
        require(isValidPinFormat(pin)) { "PIN must be 4 to 6 digits" }
        prefs.edit()
            .putString(KEY_OWNER_PIN_HASH, hashPin(pin))
            .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
            .putLong(KEY_PIN_LOCKOUT_UNTIL_MS, 0L)
            .apply()
        _hasOwnerPin.value = true
        _pinLockoutUntilMs.value = 0L
    }

    fun verifyOwnerPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_OWNER_PIN_HASH, null) ?: return false
        return saved == hashPin(pin)
    }

    fun getRemainingPinLockoutSeconds(nowMs: Long = clock()): Int {
        val until = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL_MS, 0L)
        if (until <= 0L) return 0
        if (until <= nowMs) {
            prefs.edit().putLong(KEY_PIN_LOCKOUT_UNTIL_MS, 0L).apply()
            _pinLockoutUntilMs.value = 0L
            return 0
        }
        return ((until - nowMs + 999L) / 1000L).toInt().coerceIn(1, 30)
    }

    fun verifyOwnerPinWithLockout(pin: String): PinVerifyOutcome {
        val now = clock()
        val remaining = getRemainingPinLockoutSeconds(now)
        if (remaining > 0) {
            return PinVerifyOutcome(
                success = false,
                isLocked = true,
                remainingLockSeconds = remaining,
                failedAttempts = MAX_PIN_ATTEMPTS
            )
        }
        if (verifyOwnerPin(pin)) {
            prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKOUT_UNTIL_MS, 0L)
                .apply()
            _pinLockoutUntilMs.value = 0L
            return PinVerifyOutcome(
                success = true,
                isLocked = false,
                remainingLockSeconds = 0,
                failedAttempts = 0
            )
        }
        val nextAttempts = prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0) + 1
        return if (nextAttempts >= MAX_PIN_ATTEMPTS) {
            val lockoutUntil = now + PIN_LOCKOUT_DURATION_MS
            prefs.edit()
                .putInt(KEY_PIN_FAILED_ATTEMPTS, 0)
                .putLong(KEY_PIN_LOCKOUT_UNTIL_MS, lockoutUntil)
                .apply()
            _pinLockoutUntilMs.value = lockoutUntil
            PinVerifyOutcome(
                success = false,
                isLocked = true,
                remainingLockSeconds = 30,
                failedAttempts = MAX_PIN_ATTEMPTS
            )
        } else {
            prefs.edit().putInt(KEY_PIN_FAILED_ATTEMPTS, nextAttempts).apply()
            PinVerifyOutcome(
                success = false,
                isLocked = false,
                remainingLockSeconds = 0,
                failedAttempts = nextAttempts
            )
        }
    }

    fun changeOwnerPin(oldPin: String, newPin: String): Boolean {
        if (!verifyOwnerPin(oldPin)) return false
        if (!isValidPinFormat(newPin)) return false
        setOwnerPin(newPin)
        return true
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("storeos_pin_salt:$pin").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "storeos_stock_prefs"
        private const val DEMO_PREFS_NAME = "storeos_demo_stock_prefs"
        private const val KEY_PHOTO_STORAGE_MODE = "photo_storage_mode"
        private const val KEY_RECEIPT_IMAGE_STYLE = "receipt_image_style"
        private const val KEY_LAST_ACCOUNT_ID = "last_used_account_id"
        private const val KEY_SHOW_PROFIT_IN_SIMPLE_MODE = "show_profit_in_simple_mode"
        private const val KEY_OWNER_PIN_HASH = "owner_pin_hash"
        private const val KEY_PIN_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL_MS = "pin_lockout_until_ms"

        const val MAX_PIN_ATTEMPTS = 5
        const val PIN_LOCKOUT_DURATION_MS = 30_000L

        fun prefsNameForStore(storeId: String): String =
            if (storeId == "demo_store") DEMO_PREFS_NAME else PREFS_NAME

        fun forStore(
            context: Context,
            storeId: String,
            clock: () -> Long = { System.currentTimeMillis() }
        ): StockPreferences = StockPreferences(
            context = context,
            clock = clock,
            prefsName = prefsNameForStore(storeId)
        )

        fun clearDemoPrefs(context: Context) {
            context.applicationContext
                .getSharedPreferences(DEMO_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        fun isValidPinFormat(pin: String): Boolean =
            pin.length in 4..6 && pin.all { it.isDigit() }
    }
}
