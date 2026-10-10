package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

data class OptimizedImageResult(
    val base64Data: String,
    val thumbnailBitmap: Bitmap,
    val byteSize: Int,
    val originalWidth: Int,
    val originalHeight: Int,
    val scaledWidth: Int,
    val scaledHeight: Int
)

object ImageOptimizer {
    private const val MAX_DIMENSION = 800
    private const val JPEG_QUALITY = 60 // 0.6 quality for <100KB target

    suspend fun optimize(context: Context, imageUri: Uri): OptimizedImageResult = withContext(Dispatchers.IO) {
        // Step 1: Decode bounds to calculate inSampleSize
        var inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Step 2: Compute inSampleSize
        var inSampleSize = 1
        val maxDim = max(originalWidth, originalHeight)
        if (maxDim > MAX_DIMENSION) {
            inSampleSize = maxDim / MAX_DIMENSION
            if (inSampleSize < 1) inSampleSize = 1
        }

        // Step 3: Decode bitmap with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        inputStream = context.contentResolver.openInputStream(imageUri)
        val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            ?: throw IllegalStateException("Failed to decode image from uri")
        inputStream?.close()

        // Step 4: Scale strictly so max dimension <= 800
        val currentMax = max(sampledBitmap.width, sampledBitmap.height)
        val finalBitmap = if (currentMax > MAX_DIMENSION) {
            val scaleRatio = MAX_DIMENSION.toFloat() / currentMax.toFloat()
            val targetW = (sampledBitmap.width * scaleRatio).toInt()
            val targetH = (sampledBitmap.height * scaleRatio).toInt()
            Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                if (it != sampledBitmap) {
                    sampledBitmap.recycle()
                }
            }
        } else {
            sampledBitmap
        }

        // Step 5: Compress to JPEG with 60% quality
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        val byteSize = byteArray.size

        // Step 6: Encode clean base64 without data URI prefix
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        OptimizedImageResult(
            base64Data = base64String,
            thumbnailBitmap = finalBitmap,
            byteSize = byteSize,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            scaledWidth = finalBitmap.width,
            scaledHeight = finalBitmap.height
        )
    }
}
