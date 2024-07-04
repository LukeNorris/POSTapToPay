package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.postaptopay.data.local.InstallationIdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Base64
import java.util.UUID
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.adyen.postaptopay.util.DeepLinkUtils

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val installationIdRepository = InstallationIdRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val paymentIsLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _error

    private val _price = MutableStateFlow("")
    val price: StateFlow<String> = _price

    fun updatePrice(newPrice: String) {
        _price.value = newPrice
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @RequiresApi(Build.VERSION_CODES.O)
    fun makePayment(activity: AppCompatActivity, requestedAmount: String, paymentType: String) {
        viewModelScope.launch {
            val installationId = withContext(Dispatchers.IO) {
                installationIdRepository.getInstallationId()
            }
            Log.d("PaymentViewModel", "Current Installation ID: $installationId")
            if (installationId.isNullOrEmpty()) {
                Log.d("PaymentViewModel", "Installation ID is empty. Cannot create nexo request without.")
            } else {
                Log.d("PaymentViewModel", "Making a payment initiated.")
                val nexoRequest = generateNexoRequest("Luke's cash register", installationId, "EUR", requestedAmount, paymentType)
                Log.d("PaymentViewModel", "Generated nexo request: $nexoRequest")
                createPaymentLink(nexoRequest = nexoRequest, activity)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPaymentLink(nexoRequest: String, activity: AppCompatActivity) {
        val returnUrl = "merchantscheme://com.merchant.companion/payment"
        val returnUrlEncoded = URLEncoder.encode(returnUrl, Charsets.UTF_8.name())
        val nexoRequestBase64 = Base64.getEncoder().encodeToString(nexoRequest.toByteArray())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("adyenpayments://nexo?request=$nexoRequestBase64&returnUrl=$returnUrlEncoded")
        }
        Log.d("PaymentViewModel", "Encoded nexo request: $nexoRequestBase64")
        Log.d("PaymentViewModel", "Full deep link URL: ${intent.data}")
        DeepLinkUtils.openDeepLink(activity, intent.data!!)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(response: String? = null, error: String? = null) {
        Log.d("PaymentViewModel", "Handling deep link response. Response: $response, error: $error")
        if (!response.isNullOrEmpty()) {
            Log.d("PaymentViewModel", "Received payment response: $response")
            // Handle the response from the payment here
        } else if (!error.isNullOrEmpty()) {
            Log.e("PaymentViewModel", "Received error from deep link response: $error")
            _error.value = error
        } else {
            Log.e("PaymentViewModel", "Unhandled deep link response: no response or error provided.")
            _error.value = "Unhandled deep link response."
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateNexoRequest(
        saleId: String = "AndroidSampleApp",
        poiId: String,
        currency: String,
        requestedAmount: String,
        paymentType: String
    ): String {
        val timeStamp = ZonedDateTime.now().format(DATE_FORMAT)
        val maxServiceIdSize = 10
        val serviceId: String = UUID.randomUUID().toString()
        val transactionID: String = "Luke-Adyen-TTP-Test"

        return """       
        |{
        |  "SaleToPOIRequest": {
        |    "MessageHeader": {
        |      "ProtocolVersion": "3.0",
        |      "MessageClass": "Service",
        |      "MessageCategory": "Payment",
        |      "MessageType": "Request",
        |      "ServiceID": "${serviceId.take(maxServiceIdSize)}",
        |      "SaleID": "$saleId",
        |      "POIID": "$poiId",
        |      "Timestamp": "$timeStamp",
        |      "SaleTransactionID": "$transactionID"
        |    },
        |    "PaymentRequest": {
        |      "SaleData": {
        |        "SaleTransactionID": {
        |          "TransactionID": "$transactionID",
        |          "TimeStamp": "$timeStamp"
        |        },
        |        "SaleReferenceID": "testSaleReferenceID",
        |        "RequestedValidity": "60"
        |      },
        |      "PaymentTransaction": {
        |        "AmountsReq": {
        |          "Currency": "$currency",
        |          "RequestedAmount":$requestedAmount
        |        }
        |      },
        |      "PaymentData":{
        |           "PaymentType":"$paymentType"
        |       }
        |    }
        |  }
        |}
    """.trimMargin("|")
    }
}
