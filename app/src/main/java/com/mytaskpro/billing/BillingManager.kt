package com.mytaskpro.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    private lateinit var billingClient: BillingClient
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _purchaseFlow = MutableStateFlow<PurchaseState>(PurchaseState.NotPurchased)
    val purchaseFlow: StateFlow<PurchaseState> = _purchaseFlow

    private val _productDetailsFlow = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsFlow: StateFlow<List<ProductDetails>> = _productDetailsFlow

    private val _billingConnectionState = MutableLiveData<Boolean>()
    val billingConnectionState: LiveData<Boolean> = _billingConnectionState

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        connectToGooglePlayBilling()
    }

    private fun connectToGooglePlayBilling() {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingResponseCode.OK) {
            _billingConnectionState.postValue(true)
            coroutineScope.launch {
                queryProductDetails()
                queryPurchases()
            }
        } else {
            _billingConnectionState.postValue(false)
            Timber.e("Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        _billingConnectionState.postValue(false)
        connectToGooglePlayBilling()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            coroutineScope.launch {
                processPurchases(purchases)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            _purchaseFlow.value = PurchaseState.Canceled
        } else {
            _purchaseFlow.value = PurchaseState.Error(billingResult.debugMessage)
        }
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_upgrade")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        withContext(Dispatchers.IO) {
            val result = billingClient.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingResponseCode.OK) {
                if (result.productDetailsList.isNullOrEmpty()) {
                    Timber.e("Product details list is null or empty")
                } else {
                    Timber.d("Product details retrieved successfully: ${result.productDetailsList}")
                }
                _productDetailsFlow.value = result.productDetailsList ?: emptyList()
            } else {
                Timber.e("Failed to get product details: ${result.billingResult.debugMessage}")
            }
        }
    }

    private suspend fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }

        processPurchases(result.purchasesList)
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase.purchaseToken)
                }
                _purchaseFlow.value = PurchaseState.Purchased
            }
        }
    }

    private suspend fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        withContext(Dispatchers.IO) {
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    Timber.e("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        if (_billingConnectionState.value != true) {
            Timber.e("Billing client is not connected")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun refreshProductDetails() {
        coroutineScope.launch {
            queryProductDetails()
        }
    }

    sealed class PurchaseState {
        object NotPurchased : PurchaseState()
        object Purchased : PurchaseState()
        object Canceled : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }
}