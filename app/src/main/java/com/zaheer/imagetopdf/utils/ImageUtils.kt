package com.zaheer.imagetopdf.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object ImageUtils {
    
    /**
     * Loads a bitmap from a URI
     * @param context Application context
     * @param uri The URI of the image
     * @return Bitmap or null if loading fails
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Loads multiple bitmaps from URIs
     * @param context Application context
     * @param uris List of image URIs
     * @return List of successfully loaded bitmaps with their original URIs
     */
    suspend fun loadBitmapsFromUris(
        context: Context,
        uris: List<Uri>
    ): List<Pair<Uri, Bitmap>> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            loadBitmapFromUri(context, uri)?.let { bitmap ->
                uri to bitmap
            }
        }
    }
    
    /**
     * Calculates sample size for bitmap loading to avoid out of memory errors
     * @param options BitmapFactory.Options with outWidth and outHeight set
     * @param reqWidth Required width
     * @param reqHeight Required height
     * @return Sample size to use
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    /**
     * Loads a scaled bitmap from URI to avoid memory issues
     * @param context Application context
     * @param uri The URI of the image
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Scaled bitmap or null if loading fails
     */
    suspend fun loadScaledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            
            // Decode bitmap with inSampleSize set
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Gets the size of an image without loading it into memory
     * @param context Application context
     * @param uri The URI of the image
     * @return Pair of (width, height) or null if failed
     */
    suspend fun getImageSize(context: Context, uri: Uri): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
