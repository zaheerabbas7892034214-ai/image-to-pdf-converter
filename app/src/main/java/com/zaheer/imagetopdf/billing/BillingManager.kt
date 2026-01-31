package com.zaheer.imagetopdf.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages Google Play Billing integration for the app
 */
class BillingManager(
    private val context: Context,
    private val onPurchaseSuccess: () -> Unit = {},
    private val onPurchaseFailure: (String) -> Unit = {}
) : PurchasesUpdatedListener {
    
    companion object {
        private const val PRODUCT_ID = "pdf_pro_unlock"
        private const val PREFS_NAME = "billing_prefs"
        private const val KEY_IS_PRO = "is_pro"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var billingClient: BillingClient? = null
    
    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()
    
    private val _connectionState = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    private var productDetails: ProductDetails? = null
    
    /**
     * Initializes the billing client and establishes connection
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }
    
    /**
     * Starts the connection to Google Play Billing
     */
    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = true
                    // Query product details
                    queryProductDetails()
                    // Check for existing purchases
                    queryPurchases()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                _connectionState.value = false
                // Try to restart the connection on the next request to Google Play
            }
        })
    }
    
    /**
     * Queries product details from Google Play
     */
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
            }
        }
    }
    
    /**
     * Queries existing purchases and updates pro status
     */
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    /**
     * Launches the purchase flow for the Pro unlock
     * @param activity The activity to launch the billing flow from
     */
    suspend fun launchPurchaseFlow(activity: Activity): Boolean = withContext(Dispatchers.Main) {
        if (billingClient == null || !_connectionState.value) {
            onPurchaseFailure("Billing not ready")
            return@withContext false
        }
        
        val currentProductDetails = productDetails
        if (currentProductDetails == null) {
            onPurchaseFailure("Product not available")
            return@withContext false
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(currentProductDetails)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        billingResult?.responseCode == BillingClient.BillingResponseCode.OK
    }
    
    /**
     * Handles purchase updates from Google Play
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handlePurchases(purchases)
                    onPurchaseSuccess()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onPurchaseFailure("Purchase cancelled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // User already owns the product
                setPro(true)
                onPurchaseSuccess()
            }
            else -> {
                onPurchaseFailure("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Processes a list of purchases
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID)) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Grant entitlement to the user
                    setPro(true)
                    
                    // Acknowledge the purchase if it hasn't already been acknowledged
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }
    
    /**
     * Acknowledges a purchase
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Purchase acknowledged
            }
        }
    }
    
    /**
     * Restores purchases
     */
    suspend fun restorePurchases(): Boolean = withContext(Dispatchers.IO) {
        if (billingClient == null || !_connectionState.value) {
            return@withContext false
        }
        
        var foundPurchase = false
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
                foundPurchase = purchases.any { it.products.contains(PRODUCT_ID) }
            }
        }
        
        foundPurchase
    }
    
    /**
     * Sets the pro status and persists it
     */
    private fun setPro(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
        _isPro.value = isPro
    }
    
    /**
     * Gets the product price as a formatted string
     */
    fun getProductPrice(): String? {
        return productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }
    
    /**
     * Cleans up billing client
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
