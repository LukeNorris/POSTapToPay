package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
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

    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState: StateFlow<PaymentState> = _paymentState

    fun updatePrice(newPrice: String) {
        _paymentState.value = _paymentState.value.copy(
            price = newPrice
        )
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
                        val errorCondition = responseObj.optString("ErrorCondition", null)
                        val additionalResponse = responseObj.optString("AdditionalResponse", null)

                        val paymentResult = paymentResponse.optJSONObject("PaymentResult")
                        val amountsResp = paymentResult?.optJSONObject("AmountsResp")
                        val authorizedAmount = amountsResp?.optDouble("AuthorizedAmount")
                        val currency = amountsResp?.optString("Currency")

                        val toastMessage = StringBuilder("Result: $result")
                        /*if (!errorCondition.isNullOrEmpty()) {
                            toastMessage.append("\nError Condition: $errorCondition")
                        }*/
                        if (!additionalResponse.isNullOrEmpty()) {
                            val additionalResponseMap = additionalResponse.split("&").associate {
                                val (key, value) = it.split("=")
                                key to value
                            }
                            val message = additionalResponseMap["message"]?.replace("%20", " ")
                            if (!message.isNullOrEmpty()) {
                                toastMessage.append("\nReason: $message")
                            }
                        }

                        if (authorizedAmount != null && currency != null) {
                            toastMessage.append("\nAuthorized Amount: $authorizedAmount\nCurrency: $currency")
                        }

                        ToastUtils.showToast(getApplication(), toastMessage.toString())
                    } else {
                        ToastUtils.showToast(getApplication(), "No 'Response' found in PaymentResponse")
                    }
                } catch (e: Exception) {
                    ToastUtils.showToast(getApplication(), "Error parsing response: ${e.message}")
                }
            }
        } ?: run {
            _paymentState.value = _paymentState.value.copy(
                error = error?:"There was an error",
                isLoading = false
            )
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

