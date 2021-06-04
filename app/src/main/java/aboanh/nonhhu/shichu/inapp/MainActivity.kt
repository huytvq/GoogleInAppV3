package aboanh.nonhhu.shichu.inapp

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private val mSkuDetailsMap: MutableMap<String, SkuDetails> = HashMap()
    private val mSkuId1 = SkuConstant.ITEM_TO_BUY_SKU_1
    private val mSkuId2 = SkuConstant.ITEM_TO_BUY_SKU_2

    private var billingClient: BillingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBilling()
        onClickHandler()
    }

    private fun onClickHandler() {
        purchase_button.setOnClickListener { launchBilling(0) }
        purchase_button1.setOnClickListener { launchBilling(1) }
    }

    private fun initBilling() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases().setListener(this).build()
//        billingClient = BillingClient.newBuilder(this).setListener { billingResult, purchases ->
//            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                processPurchases(purchases)
//            }
//        }.build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails() //query for products
                    val purchasesList = queryPurchases() //query for purchases
                    processPurchases(purchasesList)
                    updatePriceInUi()
                }
            }

            override fun onBillingServiceDisconnected() {
                //here when something went wrong, e.g. no internet connection
            }
        })
    }

    private fun updatePriceInUi() {
        val skuList: MutableList<String> = ArrayList()
        skuList.add(SkuConstant.ITEM_TO_BUY_SKU_1)
        skuList.add(SkuConstant.ITEM_TO_BUY_SKU_2)

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        try {
            if (billingClient != null) {
                billingClient!!.querySkuDetailsAsync(
                    params.build()
                ) { billingResult, skuDetailsList ->
                    if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
                        for (detail in skuDetailsList) {
                            if (detail.sku == SkuConstant.ITEM_TO_BUY_SKU_1) {
                                purchase_button!!.text = detail.price
                            } else if (detail.sku == SkuConstant.ITEM_TO_BUY_SKU_2) {
                                purchase_button1!!.text = detail.price
                            }
                        }
                    }
                }
            } else {
                purchase_button!!.text = "$4.99/week"
                purchase_button1!!.text = "$5.99/week"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchBilling(positionButtonInApp: Int) {
        billingClient!!.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS) { responseCode, purchasesList ->
            if (responseCode.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchasesList!!.size > 0) {
//                    SessionManager.getInstance().keySaveBuyInApp = "buy"
//                    SessionManager.getInstance().keySaveBuyInAppScreen = "buy_premium"
                }
            }
        }

        val billingFlowParams = SkuDetailsParams.newBuilder()
        billingFlowParams.setSkusList(arrayListOf(mSkuId1, mSkuId2)).setType(SkuType.INAPP)
        billingClient!!.querySkuDetailsAsync(billingFlowParams.build(),
            SkuDetailsResponseListener
            { billingResult, skuDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (skuDetailsList != null && skuDetailsList.size > 0) {
                        val flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList[positionButtonInApp])
                            .build()
                        billingClient!!.launchBillingFlow(this@MainActivity, flowParams)
                    } else {
                        //try to add item/product id "purchase" inside managed product in google play console
                        Toast.makeText(
                            applicationContext,
                            "Purchase Item not Found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        " Error " + billingResult.debugMessage, Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun queryPurchases(): List<Purchase> {
        if (billingClient != null) {
            val purchasesResult = billingClient!!.queryPurchases(BillingClient.SkuType.SUBS)
            return purchasesResult.purchasesList ?: Collections.emptyList()
        }
        return Collections.emptyList()
    }

    private fun querySkuDetails() {
        val skuDetailsParamsBuilder = SkuDetailsParams.newBuilder()
        val skuList: MutableList<String> = ArrayList()
        skuList.add(mSkuId1)
        skuDetailsParamsBuilder.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        if (billingClient != null) {
            try {
                billingClient!!.querySkuDetailsAsync(skuDetailsParamsBuilder.build()) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == 0) {
                        if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
                            for (skuDetails in skuDetailsList) {
                                mSkuDetailsMap[skuDetails.sku] = skuDetails
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun payComplete() {
        Toast.makeText(this, "Buyed", Toast.LENGTH_SHORT).show()
//        val intent = Intent(this@InAppActivity, MainActivity::class.java)
//        intent.flags =
//            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
    }

    private fun processPurchases(purchases: List<Purchase>?) {
        if (purchases != null && purchases.isNotEmpty()) {
            for (purchase in purchases) {
                if (TextUtils.equals(mSkuId1, purchase.sku) || TextUtils.equals(
                        mSkuId2,
                        purchase.sku
                    )
                ) {
                    payComplete()
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
//        //if item newly purchased
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }

//        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
//            val queryAlreadyPurchasesResult = billingClient!!.queryPurchases(SkuType.INAPP)
//            val alreadyPurchases = queryAlreadyPurchasesResult.purchasesList
//            alreadyPurchases?.let { handlePurchases(it) }
//        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
//            Toast.makeText(applicationContext, "Purchase Canceled", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(
//                applicationContext,
//                "Error " + billingResult.debugMessage,
//                Toast.LENGTH_SHORT
//            ).show()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null) {
            billingClient!!.endConnection()
        }
    }
}