package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

enum class PhotoStorageMode(val prefValue: String) {
    SYNC("sync"),
    PHONE_ONLY("phone_only")
}

object ProductImageUtil {
    const val SYNC_MAX_DIMENSION_PX = 320
    const val SYNC_MAX_BYTES = 18_000
    const val PHONE_ONLY_MAX_DIMENSION_PX = 400
    const val PHONE_ONLY_MAX_BYTES = 24_000
    const val LOCAL_ONLY_FLAG = "local_only"

    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private const val DIR_NAME = "product_photos"

    fun isLocalOnlyFlag(uri: String): Boolean = uri.trim() == LOCAL_ONLY_FLAG

    fun isSyncDataUri(uri: String): Boolean = uri.trim().startsWith("data:image/", ignoreCase = true)

    fun isRemoteUrl(uri: String): Boolean {
        val clean = uri.trim().lowercase()
        return clean.startsWith("https://") || clean.startsWith("http://")
    }

    fun isLocalPhotoUri(uri: String): Boolean {
        val clean = uri.trim()
        if (clean.isEmpty() || isLocalOnlyFlag(clean) || isSyncDataUri(clean) || isRemoteUrl(clean)) {
            return false
        }
        return clean.startsWith("file://") ||
            clean.startsWith("content://") ||
            clean.startsWith("/")
    }

    fun isLocalFilePresent(context: Context, uriString: String): Boolean {
        val clean = uriString.trim()
        if (clean.isEmpty()) return false
        return when {
            clean.startsWith("file://") -> Uri.parse(clean).path?.let { File(it).exists() } == true
            clean.startsWith("/") -> File(clean).exists()
            clean.startsWith("content://") -> runCatching {
                context.contentResolver.openInputStream(Uri.parse(clean))?.use { true } ?: false
            }.getOrDefault(false)
            else -> false
        }
    }

    fun createTempCameraPhotoFile(context: Context): File {
        val dir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
        return File(dir, "cam_raw_${System.currentTimeMillis()}.jpg")
    }

    fun localOnlyPhotoFile(context: Context, storeId: String, productId: String): File {
        val safeStore = storeId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "default" }
        val safeProd = productId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "item" }
        val dir = File(context.filesDir, "$DIR_NAME/$safeStore").apply { mkdirs() }
        return File(dir, "${safeProd}.jpg")
    }

    fun findLocalOnlyPhotoFile(context: Context, storeId: String, productId: String): File? {
        if (storeId.isBlank() || productId.isBlank()) return null
        val file = localOnlyPhotoFile(context, storeId, productId)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun saveLocalOnlyPhotoBytes(
        context: Context,
        storeId: String,
        productId: String,
        bytes: ByteArray
    ): File {
        val outFile = localOnlyPhotoFile(context, storeId, productId)
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { fos -> fos.write(bytes) }
        return outFile
    }

    fun deleteLocalOnlyPhoto(context: Context, storeId: String, productId: String) {
        if (storeId.isBlank() || productId.isBlank()) return
        runCatching {
            val file = localOnlyPhotoFile(context, storeId, productId)
            if (file.exists()) file.delete()
        }
    }

    fun deleteLocalPhotoIfOwned(context: Context, uriString: String) {
        val clean = uriString.trim()
        val path = when {
            clean.startsWith("file://") -> Uri.parse(clean).path
            clean.startsWith("/") -> clean
            else -> null
        } ?: return
        runCatching {
            val target = File(path)
            val cacheDir = File(context.cacheDir, DIR_NAME).canonicalPath
            val filesDir = File(context.filesDir, DIR_NAME).canonicalPath
            val canonical = target.canonicalPath
            if (canonical.startsWith(cacheDir) || canonical.startsWith(filesDir)) {
                if (target.exists()) target.delete()
            }
        }
    }

    fun decodeDataUriBytes(dataUri: String): ByteArray? {
        val clean = dataUri.trim()
        val commaIdx = clean.indexOf(',')
        if (commaIdx <= 0 || commaIdx >= clean.lastIndex) return null
        val base64Part = clean.substring(commaIdx + 1)
        return runCatching { Base64.decode(base64Part, Base64.DEFAULT) }.getOrNull()
    }

    fun encodeBytesToDataUri(bytes: ByteArray): String {
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "$DATA_URI_PREFIX$encoded"
    }

    fun compressUriForSync(context: Context, uri: Uri): ByteArray {
        return compressUri(context, uri, SYNC_MAX_DIMENSION_PX, SYNC_MAX_BYTES)
    }

    fun compressUriForPhoneOnly(context: Context, uri: Uri): ByteArray {
        return compressUri(context, uri, PHONE_ONLY_MAX_DIMENSION_PX, PHONE_ONLY_MAX_BYTES)
    }

    fun cropSquareAndSaveCameraPhoto(
        context: Context,
        rawCaptureFile: File,
        previewWidthPx: Int,
        previewHeightPx: Int,
        squareFrameSizePx: Float,
        maxDimensionPx: Int,
        maxBytes: Int
    ): Uri {
        val decoded = decodeOrientedFileBitmap(rawCaptureFile)
            ?: throw IllegalStateException("Could not read captured image")
        try {
            val w = decoded.width
            val h = decoded.height
            val cropSide: Int
            if (previewWidthPx > 0 && previewHeightPx > 0 && squareFrameSizePx > 0f) {
                val scaleToFitPreview = maxOf(
                    w.toFloat() / previewWidthPx.toFloat(),
                    h.toFloat() / previewHeightPx.toFloat()
                )
                val projectedSide = (squareFrameSizePx * scaleToFitPreview).roundToInt()
                cropSide = projectedSide.coerceIn(1, min(w, h))
            } else {
                cropSide = min(w, h)
            }
            val startX = ((w - cropSide) / 2).coerceAtLeast(0)
            val startY = ((h - cropSide) / 2).coerceAtLeast(0)
            val squared = Bitmap.createBitmap(decoded, startX, startY, cropSide, cropSide)
            try {
                val compressed = compressBitmapToJpeg(squared, maxDimensionPx, maxBytes)
                val cacheDir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
                val outFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { it.write(compressed) }
                return Uri.fromFile(outFile)
            } finally {
                if (squared !== decoded) squared.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    fun toCoilImageModel(
        context: Context,
        photoUri: String,
        storeId: String = "",
        productId: String = ""
    ): Any? {
        val clean = photoUri.trim()
        if (clean.isEmpty()) return null
        return when {
            isLocalOnlyFlag(clean) -> findLocalOnlyPhotoFile(context, storeId, productId)
            isSyncDataUri(clean) -> decodeDataUriBytes(clean)
            clean.startsWith("file://") -> {
                val file = Uri.parse(clean).path?.let { File(it) }
                if (file != null && file.exists()) file else null
            }
            clean.startsWith("/") -> {
                val file = File(clean)
                if (file.exists()) file else null
            }
            clean.startsWith("content://") -> Uri.parse(clean)
            isRemoteUrl(clean) -> clean
            else -> null
        }
    }

    private fun compressUri(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int,
        maxBytes: Int
    ): ByteArray {
        val bytes = when (uri.scheme) {
            "file", null -> {
                val path = uri.path ?: throw IllegalArgumentException("Invalid file URI")
                File(path).readBytes()
            }
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot read image URI")
        }

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        var inSample = 1
        while (
            boundsOpts.outWidth / inSample > maxDimensionPx * 2 ||
            boundsOpts.outHeight / inSample > maxDimensionPx * 2
        ) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: throw IllegalStateException("Could not decode image")
        return try {
            compressBitmapToJpeg(bmp, maxDimensionPx, maxBytes)
        } finally {
            bmp.recycle()
        }
    }

    private fun compressBitmapToJpeg(
        source: Bitmap,
        maxDimensionPx: Int,
        maxBytes: Int
    ): ByteArray {
        val initialMaxSide = maxOf(source.width, source.height)
        var currentBitmap = if (initialMaxSide > maxDimensionPx) {
            val ratio = maxDimensionPx.toFloat() / initialMaxSide.toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).roundToInt().coerceAtLeast(1),
                (source.height * ratio).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        try {
            var quality = 58
            var result: ByteArray
            do {
                val out = ByteArrayOutputStream()
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                result = out.toByteArray()
                quality -= 6
            } while (result.size > maxBytes && quality >= 20)

            // If still above byte cap on highly detailed textures, step down dimensions gently
            while (result.size > maxBytes && maxOf(currentBitmap.width, currentBitmap.height) > 140) {
                val nextWidth = (currentBitmap.width * 0.8f).roundToInt().coerceAtLeast(1)
                val nextHeight = (currentBitmap.height * 0.8f).roundToInt().coerceAtLeast(1)
                val nextBitmap = Bitmap.createScaledBitmap(currentBitmap, nextWidth, nextHeight, true)
                if (currentBitmap !== source && currentBitmap !== nextBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = nextBitmap

                var passQuality = 40
                do {
                    val out = ByteArrayOutputStream()
                    currentBitmap.compress(Bitmap.CompressFormat.JPEG, passQuality, out)
                    result = out.toByteArray()
                    passQuality -= 8
                } while (result.size > maxBytes && passQuality >= 20)
            }
            return result
        } finally {
            if (currentBitmap !== source) currentBitmap.recycle()
        }
    }

    private fun decodeOrientedFileBitmap(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        val rotationDeg = runCatching {
            val exif = ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (rotationDeg == 0f) return bmp
        val matrix = Matrix().apply { postRotate(rotationDeg) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        if (rotated !== bmp) bmp.recycle()
        return rotated
    }
}
