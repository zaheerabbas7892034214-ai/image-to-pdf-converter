package com.zaheer.imagetopdf.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.zaheer.imagetopdf.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Core business logic for generating PDF documents from images
 */
class PdfGenerator(private val context: Context) {
    
    companion object {
        // Standard A4 size in points (1/72 inch)
        private const val A4_WIDTH = 595
        private const val A4_HEIGHT = 842
        
        // Margins
        private const val MARGIN = 20
    }
    
    /**
     * Generates a PDF from a list of image URIs
     * @param imageUris List of image URIs to include in the PDF
     * @param outputFile The file where the PDF will be written
     * @param addWatermark Whether to add a watermark (for free version)
     * @return True if successful, false otherwise
     */
    suspend fun generatePdf(
        imageUris: List<Uri>,
        outputFile: File,
        addWatermark: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext false
        }
        
        val pdfDocument = PdfDocument()
        
        try {
            imageUris.forEachIndexed { index, uri ->
                // Load bitmap
                val bitmap = ImageUtils.loadBitmapFromUri(context, uri) ?: return@forEachIndexed
                
                // Create a new page
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                
                val canvas = page.canvas
                
                // Draw white background
                canvas.drawColor(Color.WHITE)
                
                // Calculate scaling to fit the image on the page with margins
                val availableWidth = A4_WIDTH - (2 * MARGIN)
                val availableHeight = A4_HEIGHT - (2 * MARGIN)
                
                val scaleFactor = minOf(
                    availableWidth.toFloat() / bitmap.width,
                    availableHeight.toFloat() / bitmap.height
                )
                
                val scaledWidth = (bitmap.width * scaleFactor).toInt()
                val scaledHeight = (bitmap.height * scaleFactor).toInt()
                
                // Center the image on the page
                val left = (A4_WIDTH - scaledWidth) / 2f
                val top = (A4_HEIGHT - scaledHeight) / 2f
                
                // Scale and draw the bitmap
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                canvas.drawBitmap(scaledBitmap, left, top, null)
                
                // Add watermark if needed
                if (addWatermark) {
                    drawWatermark(canvas)
                }
                
                // Clean up
                scaledBitmap.recycle()
                bitmap.recycle()
                
                pdfDocument.finishPage(page)
            }
            
            // Write the document to file
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
    
    /**
     * Draws a watermark on the canvas
     * @param canvas The canvas to draw on
     */
    private fun drawWatermark(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.argb(80, 128, 128, 128)
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val watermarkText = "FREE VERSION"
        val x = A4_WIDTH / 2f
        val y = A4_HEIGHT / 2f
        
        canvas.save()
        canvas.rotate(-45f, x, y)
        canvas.drawText(watermarkText, x, y, paint)
        canvas.restore()
    }
    
    /**
     * Generates a preview bitmap of what the PDF will look like
     * @param imageUri The image URI to preview
     * @param addWatermark Whether to add a watermark
     * @return A preview bitmap or null if failed
     */
    suspend fun generatePreviewBitmap(
        imageUri: Uri,
        addWatermark: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = ImageUtils.loadBitmapFromUri(context, imageUri) ?: return@withContext null
            
            // Create a scaled version for preview
            val previewWidth = 400
            val previewHeight = (previewWidth * A4_HEIGHT / A4_WIDTH)
            
            val previewBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(previewBitmap)
            
            // Draw white background
            canvas.drawColor(Color.WHITE)
            
            // Calculate scaling to fit the image
            val margin = (MARGIN * previewWidth / A4_WIDTH)
            val availableWidth = previewWidth - (2 * margin)
            val availableHeight = previewHeight - (2 * margin)
            
            val scaleFactor = minOf(
                availableWidth.toFloat() / originalBitmap.width,
                availableHeight.toFloat() / originalBitmap.height
            )
            
            val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
            val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
            
            val left = (previewWidth - scaledWidth) / 2f
            val top = (previewHeight - scaledHeight) / 2f
            
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, left, top, null)
            
            if (addWatermark) {
                val paint = Paint().apply {
                    color = Color.argb(80, 128, 128, 128)
                    textSize = 20f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                val x = previewWidth / 2f
                val y = previewHeight / 2f
                
                canvas.save()
                canvas.rotate(-45f, x, y)
                canvas.drawText("FREE VERSION", x, y, paint)
                canvas.restore()
            }
            
            scaledBitmap.recycle()
            originalBitmap.recycle()
            
            previewBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
