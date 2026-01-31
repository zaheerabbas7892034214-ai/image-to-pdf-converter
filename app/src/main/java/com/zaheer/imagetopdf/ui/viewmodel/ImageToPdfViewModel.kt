package com.zaheer.imagetopdf.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaheer.imagetopdf.billing.BillingManager
import com.zaheer.imagetopdf.domain.PdfGenerator
import com.zaheer.imagetopdf.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for managing image to PDF conversion state and operations
 */
class ImageToPdfViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val FREE_IMAGE_LIMIT = 5
    }
    
    private val context: Context get() = getApplication<Application>().applicationContext
    private val pdfGenerator = PdfGenerator(context)
    
    // Billing Manager
    lateinit var billingManager: BillingManager
        private set
    
    // Selected images
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()
    
    // Processing state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // Generated PDF file
    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()
    
    // Messages/Errors
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // Success state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        initializeBilling()
        // Clean up old temp files on startup
        FileUtils.cleanupOldTempFiles(context)
    }
    
    /**
     * Initializes the billing manager
     */
    private fun initializeBilling() {
        billingManager = BillingManager(
            context = context,
            onPurchaseSuccess = {
                showMessage("Purchase successful! Pro features unlocked.")
            },
            onPurchaseFailure = { error ->
                showMessage("Purchase failed: $error")
            }
        )
        billingManager.initialize()
    }
    
    /**
     * Adds images to the selection
     */
    fun addImages(uris: List<Uri>) {
        val currentImages = _selectedImages.value.toMutableList()
        
        // Check if user is pro or within free limit
        if (!billingManager.isPro.value) {
            val remainingSlots = FREE_IMAGE_LIMIT - currentImages.size
            if (remainingSlots <= 0) {
                showMessage("Free version limited to $FREE_IMAGE_LIMIT images. Upgrade to Pro for unlimited!")
                return
            }
            
            if (uris.size > remainingSlots) {
                showMessage("Free version limited to $FREE_IMAGE_LIMIT images. Adding only first $remainingSlots.")
                currentImages.addAll(uris.take(remainingSlots))
            } else {
                currentImages.addAll(uris)
            }
        } else {
            currentImages.addAll(uris)
        }
        
        _selectedImages.value = currentImages
    }
    
    /**
     * Removes an image from the selection by index
     */
    fun removeImage(index: Int) {
        val currentImages = _selectedImages.value.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _selectedImages.value = currentImages
        }
    }
    
    /**
     * Reorders images in the selection
     */
    fun moveImage(fromIndex: Int, toIndex: Int) {
        val currentImages = _selectedImages.value.toMutableList()
        if (fromIndex in currentImages.indices && toIndex in currentImages.indices) {
            val item = currentImages.removeAt(fromIndex)
            currentImages.add(toIndex, item)
            _selectedImages.value = currentImages
        }
    }
    
    /**
     * Clears all selected images
     */
    fun clearImages() {
        _selectedImages.value = emptyList()
        _generatedPdfFile.value = null
    }
    
    /**
     * Converts selected images to PDF
     */
    fun convertToPdf() {
        if (_selectedImages.value.isEmpty()) {
            showMessage("Please select images first")
            return
        }
        
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val fileName = FileUtils.generatePdfFileName()
                val tempFile = FileUtils.createTempPdfFile(context, fileName)
                
                // Add watermark if user is not pro
                val addWatermark = !billingManager.isPro.value
                
                val success = pdfGenerator.generatePdf(
                    imageUris = _selectedImages.value,
                    outputFile = tempFile,
                    addWatermark = addWatermark
                )
                
                if (success) {
                    _generatedPdfFile.value = tempFile
                    showSuccessMessage("PDF created successfully!")
                } else {
                    showMessage("Error creating PDF")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Saves the generated PDF to device storage
     */
    fun savePdf() {
        val pdfFile = _generatedPdfFile.value
        if (pdfFile == null) {
            showMessage("No PDF to save")
            return
        }
        
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val fileName = FileUtils.generatePdfFileName()
                val uri = FileUtils.savePdfToDownloads(context, pdfFile, fileName)
                
                if (uri != null) {
                    showSuccessMessage("PDF saved to Downloads")
                } else {
                    showMessage("Error saving PDF")
                }
            } catch (e: Exception) {
                showMessage("Error: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Shares the generated PDF
     */
    fun sharePdf(): Intent? {
        val pdfFile = _generatedPdfFile.value ?: return null
        
        return try {
            val uri = FileUtils.getShareableUri(context, pdfFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            showMessage("Error sharing PDF: ${e.message}")
            null
        }
    }
    
    /**
     * Restores purchases
     */
    fun restorePurchases() {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val restored = billingManager.restorePurchases()
                if (restored) {
                    showSuccessMessage("Purchases restored successfully")
                } else {
                    showMessage("No purchases to restore")
                }
            } catch (e: Exception) {
                showMessage("Error restoring purchases: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Shows a message to the user
     */
    private fun showMessage(msg: String) {
        _message.value = msg
    }
    
    /**
     * Shows a success message to the user
     */
    private fun showSuccessMessage(msg: String) {
        _successMessage.value = msg
    }
    
    /**
     * Clears the current message
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * Clears the success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}
