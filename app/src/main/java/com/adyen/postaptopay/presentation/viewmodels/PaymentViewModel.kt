package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.postaptopay.data.local.InstallationIdRepository
import com.adyen.postaptopay.data.remote.PaymentRepository
import com.adyen.postaptopay.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val installationIdRepository = InstallationIdRepository(application)

    private val repository = PaymentRepository(application)

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
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @RequiresApi(Build.VERSION_CODES.O)
    fun makePayment(activity: AppCompatActivity, requestedAmount: String, paymentType: String) {

        viewModelScope.launch {
            val installationId = withContext(Dispatchers.IO) {
                installationIdRepository.getInstallationId()
            }

            //Log.d("PaymentViewModel", "Current Installation ID: $installationId")
            if (installationId.isNullOrEmpty()) {
                Log.d("PaymentViewModel", "Installation ID is empty. Cannot create nexo request without.")
            } else {
                Log.d("PaymentViewModel", "Making a payment initiated.")
                val nexoRequest = generateNexoRequest("My cash register", installationId, "EUR", requestedAmount, paymentType)
                Log.d("PaymentViewModel", "Generated nexo request: $nexoRequest")
                repository.createPaymentLink(nexoRequest, activity)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String, String>) {
        val response = queryParams["response"]
        val error = queryParams["error"]

        response?.let {
            Log.d("handlePaymentDeepLink", "response: $it")
            viewModelScope.launch {
                try {
                    val decryptedNexoResponseString = repository.handleDeepLinkResponse(it)
                    val decryptedNexoJson = JSONObject(decryptedNexoResponseString)
                    val saleToPOIResponse = decryptedNexoJson.getJSONObject("SaleToPOIResponse")
                    val paymentResponse = saleToPOIResponse.optJSONObject("PaymentResponse")
                    Log.d("handlePaymentDeepLink", "paymentResponse: $paymentResponse")

                    if (paymentResponse?.has("Response") == true) {
                        val responseObj = paymentResponse.getJSONObject("Response")
                        val result = responseObj.getString("Result")
                        ToastUtils.showToast(getApplication(), "Result: $result")
                    } else {
                        ToastUtils.showToast(getApplication(), "No 'Response' found in PaymentResponse")
                    }
                } catch (e: Exception) {
                    ToastUtils.showToast(getApplication(), "Error parsing response: ${e.message}")
                }
            }
        } ?: run {
            _error.value = "Response is null"
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


