package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.data.repository.StoreRepository
import com.example.domain.ShopType
import com.example.ui.screens.SellScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.NetworkMonitor
import com.example.util.ProductImageUtil
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class SellScreenPhotoUpgradeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createDetailedSampleJpeg(file: File, width: Int, height: Int): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        for (y in 0 until height step 16) {
            for (x in 0 until width step 16) {
                paint.color = Color.rgb((x * 7) % 255, (y * 11) % 255, ((x + y) * 5) % 255)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 16).toFloat(), (y + 16).toFloat(), paint)
            }
        }
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bmp.recycle()
        return file
    }

    @Test
    fun productImageCompression_staysSmallUnderTightStorageCaps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawFile = createDetailedSampleJpeg(File(context.cacheDir, "raw_test_photo.jpg"), 1600, 1200)
        try {
            val syncBytes = ProductImageUtil.compressUriForSync(context, Uri.fromFile(rawFile))
            assertTrue(
                "Sync photo bytes (${syncBytes.size}) should be <= ${ProductImageUtil.SYNC_MAX_BYTES}",
                syncBytes.size <= ProductImageUtil.SYNC_MAX_BYTES
            )
            val syncBmp = BitmapFactory.decodeByteArray(syncBytes, 0, syncBytes.size)
            assertNotNull(syncBmp)
            assertTrue(syncBmp!!.width <= ProductImageUtil.SYNC_MAX_DIMENSION_PX)
            assertTrue(syncBmp.height <= ProductImageUtil.SYNC_MAX_DIMENSION_PX)
            syncBmp.recycle()

            val localBytes = ProductImageUtil.compressUriForPhoneOnly(context, Uri.fromFile(rawFile))
            assertTrue(
                "Phone-only photo bytes (${localBytes.size}) should be <= ${ProductImageUtil.PHONE_ONLY_MAX_BYTES}",
                localBytes.size <= ProductImageUtil.PHONE_ONLY_MAX_BYTES
            )
            val localBmp = BitmapFactory.decodeByteArray(localBytes, 0, localBytes.size)
            assertNotNull(localBmp)
            assertTrue(localBmp!!.width <= ProductImageUtil.PHONE_ONLY_MAX_DIMENSION_PX)
            assertTrue(localBmp.height <= ProductImageUtil.PHONE_ONLY_MAX_DIMENSION_PX)
            localBmp.recycle()

            val croppedUri = ProductImageUtil.cropSquareAndSaveCameraPhoto(
                context = context,
                rawCaptureFile = rawFile,
                previewWidthPx = 1080,
                previewHeightPx = 1920,
                squareFrameSizePx = 864f,
                maxDimensionPx = ProductImageUtil.SYNC_MAX_DIMENSION_PX,
                maxBytes = ProductImageUtil.SYNC_MAX_BYTES
            )
            val croppedFile = File(croppedUri.path!!)
            assertTrue(croppedFile.exists())
            assertTrue(croppedFile.length() <= ProductImageUtil.SYNC_MAX_BYTES)
            val croppedBmp = BitmapFactory.decodeFile(croppedFile.absolutePath)
            assertNotNull(croppedBmp)
            assertEquals("Camera crop must remain 1:1 square", croppedBmp!!.width, croppedBmp.height)
            croppedBmp.recycle()
            croppedFile.delete()
        } finally {
            rawFile.delete()
        }
    }

    @Test
    fun sellScreen_showsFullSquarePhotoForProductWithPhoto_andOrbForProductWithoutPhoto() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleFile = createDetailedSampleJpeg(File(context.cacheDir, "sell_card_sample.jpg"), 320, 320)
        val dataUri = ProductImageUtil.encodeBytesToDataUri(
            ProductImageUtil.compressUriForSync(context, Uri.fromFile(sampleFile))
        )
        sampleFile.delete()

        val repo = StoreRepository(db = null, appContext = context).apply {
            configureDemoStore(ShopType.GROCERY)
        }
        val (photoProdId, plainProdId) = kotlinx.coroutines.runBlocking {
            val res1 = repo.addProduct(
                "demo_store",
                Product(
                    name = "AAA Photo Rice Pack",
                    category = "Groceries",
                    sellPrice = 120.0,
                    stock = 10.0,
                    unit = "kg",
                    photoUri = dataUri
                ),
                com.example.util.PhotoStorageMode.SYNC
            )
            Thread.sleep(5)
            val res2 = repo.addProduct(
                "demo_store",
                Product(
                    name = "AAA Plain Sugar",
                    category = "Groceries",
                    sellPrice = 95.0,
                    stock = 8.0,
                    unit = "kg",
                    photoUri = ""
                ),
                com.example.util.PhotoStorageMode.SYNC
            )
            res1.productId to res2.productId
        }

        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )

        try {
            composeRule.setContent {
                StoreOSTheme {
                    Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                        SellScreen(storeViewModel = storeVm, onBack = {})
                    }
                }
            }

            composeRule.waitForIdle()

            val fullPhotoNode = composeRule.onNodeWithTag("sell_product_full_photo_$photoProdId", useUnmergedTree = true)
            fullPhotoNode.assertIsDisplayed()

            val cardBounds = composeRule.onNodeWithTag("sell_catalog_item_$photoProdId").getBoundsInRoot()
            val photoBounds = fullPhotoNode.getBoundsInRoot()

            assertTrue(
                "Full photo width (${photoBounds.width}) should match full card width (${cardBounds.width})",
                abs(cardBounds.width.value - photoBounds.width.value) <= 4f
            )
            assertTrue(
                "Full photo should be 1:1 square (${photoBounds.width} vs ${photoBounds.height}), not a tiny 60dp circle",
                abs(photoBounds.width.value - photoBounds.height.value) <= 2f && photoBounds.width.value >= 140f
            )

            // Product without photo should NOT show a full photo node
            assertEquals(
                0,
                composeRule.onAllNodesWithTag("sell_product_full_photo_$plainProdId", useUnmergedTree = true)
                    .fetchSemanticsNodes().size
            )
            composeRule.onNodeWithTag("sell_catalog_item_$plainProdId").assertIsDisplayed()
        } finally {
            storeVm.dispose()
        }
    }
}
