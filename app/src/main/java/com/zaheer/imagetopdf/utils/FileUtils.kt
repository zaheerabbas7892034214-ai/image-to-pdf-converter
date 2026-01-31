package com.zaheer.imagetopdf.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileUtils {
    
    /**
     * Saves a PDF file to the device's Downloads folder using MediaStore
     * @param context Application context
     * @param pdfFile The PDF file to save
     * @param fileName The desired file name
     * @return Uri of the saved file, or null if failed
     */
    fun savePdfToDownloads(context: Context, pdfFile: File, fileName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                uri
            } else {
                // For older Android versions, use legacy approach
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, fileName)
                pdfFile.copyTo(destFile, overwrite = true)
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Creates a temporary PDF file in the app's cache directory
     * @param context Application context
     * @param fileName The desired file name
     * @return The created File object
     */
    fun createTempPdfFile(context: Context, fileName: String): File {
        val cacheDir = File(context.cacheDir, "pdfs")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, fileName)
    }
    
    /**
     * Gets a shareable URI for a file using FileProvider
     * @param context Application context
     * @param file The file to share
     * @return Content URI for sharing
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Generates a unique PDF file name based on current timestamp
     * @return A unique file name
     */
    fun generatePdfFileName(): String {
        return "PDF_${System.currentTimeMillis()}.pdf"
    }
    
    /**
     * Cleans up temporary PDF files older than a certain age
     * @param context Application context
     * @param maxAgeMillis Maximum age in milliseconds (default 24 hours)
     */
    fun cleanupOldTempFiles(context: Context, maxAgeMillis: Long = 24 * 60 * 60 * 1000) {
        val cacheDir = File(context.cacheDir, "pdfs")
        if (cacheDir.exists() && cacheDir.isDirectory) {
            val currentTime = System.currentTimeMillis()
            cacheDir.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }
        }
    }
}
