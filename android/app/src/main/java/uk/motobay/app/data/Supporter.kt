package uk.motobay.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [price] is the store's own formatted price ("£2.99"), or null while the
 * store can't be reached or the product isn't set up. [pending] is a payment
 * Google hasn't confirmed yet (some payment methods take a while).
 */
data class SupporterState(val isSupporter: Boolean = false, val price: String? = null, val pending: Boolean = false)

/** The one-off "Supporter" purchase, which unlocks the extras and funds the app. */
interface Supporter {
    val state: StateFlow<SupporterState>

    /** Asks the store again for the price and for a purchase made earlier ("Restore purchase"). */
    fun refresh()

    /** Opens the store's purchase sheet. False if it couldn't be opened. */
    fun buy(activity: Activity): Boolean

    /** Debug builds only: switch the unlock on and off without paying. */
    fun setForTesting(on: Boolean)
}

/**
 * Supporter through Google Play Billing: one non-consumable in-app product,
 * [PRODUCT_ID], created in the Play Console. The answer is remembered on the
 * phone so the extras work offline, and re-checked with Play whenever it can
 * be reached, which is also how a refund takes the unlock away again.
 */
class PlayBillingSupporter(context: Context) : Supporter {
    private val prefs = context.getSharedPreferences("supporter", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SupporterState(isSupporter = prefs.getBoolean(KEY, false)))
    override val state: StateFlow<SupporterState> = _state.asStateFlow()
    private var product: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> if (result.ok()) handle(purchases.orEmpty(), authoritative = false) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        connect()
    }

    private fun connect() {
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.ok()) query() else Log.w(TAG, "Billing unavailable: ${result.debugMessage}")
                }
                override fun onBillingServiceDisconnected() {}
            })
        }.onFailure { Log.w(TAG, "Couldn't start billing: ${it.message}") }
    }

    override fun refresh() {
        if (client.isReady) query() else connect()
    }

    private fun query() {
        val wanted = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(wanted)).build()) { result, details ->
            if (result.ok()) {
                product = details.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                _state.update { it.copy(price = product?.oneTimePurchaseOfferDetails?.formattedPrice) }
            }
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.ok()) handle(purchases, authoritative = true)
        }
    }

    /** [authoritative] is Play's full list, so no purchase in it means there isn't one (never bought, or refunded). */
    private fun handle(purchases: List<Purchase>, authoritative: Boolean) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val bought = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (bought != null) {
            // Play refunds a purchase the app doesn't acknowledge within three days.
            if (!bought.isAcknowledged) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(bought.purchaseToken).build()) { r ->
                    if (!r.ok()) Log.w(TAG, "Acknowledge failed: ${r.debugMessage}")
                }
            }
            set(true, pending = false)
        } else if (authoritative) {
            set(false, pending)
        } else if (pending) {
            _state.update { it.copy(pending = true) }
        }
    }

    private fun set(on: Boolean, pending: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
        _state.update { it.copy(isSupporter = on, pending = pending) }
    }

    override fun buy(activity: Activity): Boolean {
        val details = product ?: return false.also { refresh() }
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
            .apply { details.oneTimePurchaseOfferDetails?.offerToken?.takeIf { it.isNotEmpty() }?.let(::setOfferToken) }
            .build()
        return client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item)).build()).ok()
    }

    override fun setForTesting(on: Boolean) = set(on, pending = false)

    private fun BillingResult.ok() = responseCode == BillingClient.BillingResponseCode.OK

    companion object {
        /** The in-app product's id in the Play Console. */
        const val PRODUCT_ID = "supporter"
        private const val KEY = "is_supporter"
        private const val TAG = "Supporter"
    }
}
