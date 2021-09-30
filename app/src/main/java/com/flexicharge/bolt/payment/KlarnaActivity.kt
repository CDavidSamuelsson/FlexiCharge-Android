package com.flexicharge.bolt.payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.flexicharge.bolt.R
import com.flexicharge.bolt.payment.api.OrderClient
import com.flexicharge.bolt.payment.api.OrderLine
import com.flexicharge.bolt.payment.api.OrderPayload
import com.google.android.gms.maps.GoogleMap
import com.klarna.mobile.sdk.api.payments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class KlarnaActivity : AppCompatActivity(), KlarnaPaymentViewCallback {
    private val klarnaPaymentView by lazy { findViewById<KlarnaPaymentView>(R.id.klarnaPaymentView) }
    private val authorizeButton by lazy { findViewById<Button>(R.id.authorizeButton) }
    private val finalizeButton by lazy { findViewById<Button>(R.id.finalizeButton) }
    private val orderButton by lazy { findViewById<Button>(R.id.orderButton) }
    private var chargerId : Int = 0

    private val paymentCategory = KlarnaPaymentCategory.PAY_NOW // please update this value if needed

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_klarna)
        chargerId = intent.getIntExtra("ChargerId", 0)

        initialize()

        setupButtons()
        klarnaPaymentView.category = paymentCategory
    }

    private fun initialize() {
        if (OrderClient.hasSetCredentials()) {
            job = GlobalScope.launch {

                // create a session and then initialize the payment view with the client token received in the response
                val sessionCall = OrderClient.instance.createCreditSession(OrderPayload.defaultPayload)
                try {
                    val resp = sessionCall.execute()
                    resp.body()?.let { session ->
                        runOnUiThread {
                            klarnaPaymentView.initialize(
                                session.client_token,
                                "${getString(R.string.return_url_scheme)}://${getString(R.string.return_url_host)}"
                            )
                        }
                    } ?: showError(getString(R.string.error_server, resp.code()))
                } catch (exception: Exception) {
                    showError(exception.message)
                }
            }
        } else {
            showError(getString(R.string.error_credentials))
        }
    }

    private fun createOrder() {
        job = GlobalScope.launch {

            val orderLIne = OrderLine(
                "https://demo.klarna.se/fashion/kp/media/wysiwyg/Accessoriesbagimg.jpg",
                "physical",
                "ChargerId: " + chargerId.toString(),
                "FlexiCharge Charge Reservation",
                1,
                30000,
                0,
                30000,
                0
            )
            val orderPayload = OrderPayload("SE", "SEK", "en-US", 30000, 0, listOf(orderLIne))
            // create the order using the auth token received in the authorization response
            val orderCall = OrderClient.instance.createOrder(orderButton.tag as String, orderPayload)
            try {
                val response = orderCall.execute()
                if (response.isSuccessful) {
                    runOnUiThread {
                        val intent = Intent(this@KlarnaActivity, KlarnaOrderCompletedActivity::class.java)
                        intent.putExtra("message","Charged 300SEK for Charger: " + chargerId)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    showError(null)
                }
            } catch (exception: Exception) {
                showError(exception.message)
            }
        }
    }

    private fun setupButtons() {
        authorizeButton.setOnClickListener {
            klarnaPaymentView.authorize(true, null)
        }

        finalizeButton.setOnClickListener {
            klarnaPaymentView.finalize(null)
        }

        orderButton.setOnClickListener {
            createOrder()
        }
    }

    private fun showError(message: String?) {
        runOnUiThread {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setMessage(message ?: getString(R.string.error_general))
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialog, _ ->
                dialog.dismiss()
            }
            alertDialog.show()
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            action.invoke()
        }
    }

    override fun onInitialized(view: KlarnaPaymentView) {

        // load the payment view after its been initialized
        view.load(null)
    }

    override fun onLoaded(view: KlarnaPaymentView) {

        // enable the authorization after the payment view is loaded
        authorizeButton.isEnabled = true
    }

    override fun onLoadPaymentReview(view: KlarnaPaymentView, showForm: Boolean) {}

    override fun onAuthorized(
        view: KlarnaPaymentView,
        approved: Boolean,
        authToken: String?,
        finalizedRequired: Boolean?
    ) {
        finalizeButton.isEnabled = finalizedRequired ?: false
        orderButton.isEnabled = approved && !(finalizedRequired ?: false)
        orderButton.tag = authToken
    }

    override fun onReauthorized(view: KlarnaPaymentView, approved: Boolean, authToken: String?) {}

    override fun onErrorOccurred(view: KlarnaPaymentView, error: KlarnaPaymentsSDKError) {
        println("An error occurred: ${error.name} - ${error.message}")
        if (error.isFatal) {
            klarnaPaymentView.visibility = View.INVISIBLE
        }
    }

    override fun onFinalized(view: KlarnaPaymentView, approved: Boolean, authToken: String?) {
        orderButton.isEnabled = approved
        orderButton.tag = authToken
    }

    override fun onResume() {
        super.onResume()
        klarnaPaymentView.registerPaymentViewCallback(this)
    }

    override fun onPause() {
        super.onPause()
        klarnaPaymentView.unregisterPaymentViewCallback(this)
        job?.cancel()
    }

}
