package com.example

import com.example.data.model.SavedBusinessStrategy
import com.example.ui.components.ShopNavTab
import com.example.ui.screens.extractSimpleTipLine
import com.example.ui.screens.isStrategyFromToday
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimpleModeTest {

    private fun dateKey(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))

    @Test
    fun `isStrategyFromToday returns true only for today's dateKey or timestamp`() {
        val nowMs = 1_760_000_000_000L
        val yesterdayMs = nowMs - 24 * 60 * 60 * 1000L

        assertFalse(isStrategyFromToday(null, nowMs))
        assertTrue(
            isStrategyFromToday(
                SavedBusinessStrategy(dateKey = dateKey(nowMs), focusLine = "Test"),
                nowMs
            )
        )
        assertFalse(
            isStrategyFromToday(
                SavedBusinessStrategy(dateKey = dateKey(yesterdayMs), focusLine = "Old"),
                nowMs
            )
        )
        assertTrue(
            isStrategyFromToday(
                SavedBusinessStrategy(dateKey = "", generatedAtMs = nowMs, focusLine = "Test"),
                nowMs
            )
        )
        assertFalse(
            isStrategyFromToday(
                SavedBusinessStrategy(dateKey = "", generatedAtMs = yesterdayMs, focusLine = "Old"),
                nowMs
            )
        )
    }

    @Test
    fun `extractSimpleTipLine hides old tips and caps today's tip at 25 words`() {
        val nowMs = 1_760_000_000_000L
        val yesterdayMs = nowMs - 24 * 60 * 60 * 1000L

        val oldStrategy = SavedBusinessStrategy(
            dateKey = dateKey(yesterdayMs),
            focusLine = "এই টিপসটি গতকালের তাই দেখাবে না"
        )
        assertNull(extractSimpleTipLine(oldStrategy, maxWords = 25, nowMs = nowMs))

        val longTip = (1..35).joinToString(" ") { "শব্দ$it" }
        val todayLongStrategy = SavedBusinessStrategy(
            dateKey = dateKey(nowMs),
            focusLine = longTip
        )
        val extracted = extractSimpleTipLine(todayLongStrategy, maxWords = 25, nowMs = nowMs)
        assertNotNull(extracted)
        val wordCount = extracted!!.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        assertEquals(25, wordCount)

        val shortTip = "আজকে নগদ বিক্রি বাড়ানোর জন্য নিয়মিত স্টক ঠিক রাখুন।"
        val todayShortStrategy = SavedBusinessStrategy(
            dateKey = dateKey(nowMs),
            diagnosis = shortTip
        )
        assertEquals(shortTip, extractSimpleTipLine(todayShortStrategy, maxWords = 25, nowMs = nowMs))
    }

    @Test
    fun `ShopLocale has non-blank EN and BN strings for rebuilt Simple Mode`() {
        val keys = listOf(
            "nav_home",
            "nav_sell",
            "nav_baki",
            "nav_stock",
            "go_detailed_mode_link",
            "simple_btn_sell",
            "simple_btn_collect_baki",
            "simple_btn_add_stock",
            "simple_card_who_owes",
            "simple_card_low_stock",
            "simple_see_all",
            "simple_card_todays_accounts",
            "simple_cash_in_hand",
            "simple_todays_profit",
            "simple_empty_baki",
            "simple_empty_low_stock",
            "simple_todays_sales"
        )
        keys.forEach { key ->
            val en = ShopLocale.get(key, AppLanguage.EN)
            val bn = ShopLocale.get(key, AppLanguage.BN)
            assertTrue("Missing EN string for $key", en.isNotBlank() && en != key)
            assertTrue("Missing BN string for $key", bn.isNotBlank() && bn != key)
        }
    }

    @Test
    fun `ShopNavTab includes BAKI for 4-tab Simple Mode navigation`() {
        val simpleTabs = listOf(ShopNavTab.HOME, ShopNavTab.SELL, ShopNavTab.BAKI, ShopNavTab.STOCK)
        assertEquals(4, simpleTabs.toSet().size)
    }
}
