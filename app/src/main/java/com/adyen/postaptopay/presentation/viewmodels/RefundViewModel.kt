package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.postaptopay.data.local.InstallationIdRepository
import com.adyen.postaptopay.data.local.TransactionRepository
import com.adyen.postaptopay.data.remote.PaymentRepository
import com.adyen.postaptopay.presentation.viewmodels.RefundState
import com.adyen.postaptopay.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * ViewModel to trigger a refund deep-link and handle its response.
 */
class RefundViewModel(application: Application) : AndroidViewModel(application) {

    private val installationRepo = InstallationIdRepository(application)
    private val paymentRepo      = PaymentRepository(application)
    private val txRepo           = TransactionRepository(application)

    private val _refundState = MutableStateFlow(RefundState())
    val refundState: StateFlow<RefundState> = _refundState

    // for ISO timestamps, same as PaymentViewModel
    @RequiresApi(Build.VERSION_CODES.O)
    private val DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * Kick off a refund.  Looks up the original transaction, builds
     * a Nexo “Refund” request, and opens the deep-link.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun makeRefund(
        activity: AppCompatActivity,
        currency: String,
        amount: Double
    ) {
        viewModelScope.launch {
            _refundState.value = RefundState(isLoading = true)

            // 1) need your installation / poiId just like payment
            val poiId = installationRepo.getInstallationId()
            if (poiId.isNullOrEmpty()) {
                _refundState.value = RefundState(
                    isLoading = false,
                    error     = "Missing installation/POI ID"
                )
                return@launch
            }

            // 2) grab the most recent transaction you want to refund
            val lastFive = txRepo.getLastTransactions()
            val original = lastFive.firstOrNull()
            if (original == null) {
                _refundState.value = RefundState(
                    isLoading = false,
                    error     = "No transaction to refund"
                )
                return@launch
            }

            // 3) build the JSON
            val nexoJson = generateRefundRequest(
                saleId         = "My cash register",
                poiId          = poiId,
                currency       = currency,
                amount         = amount,
            )
            Log.d("RefundViewModel", "Refund NexoRequest: $nexoJson")

            // 4) deep-link into Adyen app for refund
            paymentRepo.createPaymentLink(nexoJson, activity)
            // response will come back via MainActivity → handleDeepLink in PaymentViewModel
            // or you could route it here similarly if you like
            _refundState.value = RefundState(isLoading = false)
        }
    }

    /**
     * (Optional) if you want this ViewModel to also parse the deep-link response
     * instead of PaymentViewModel, you can mirror the handler here:
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String, String>) {
        val response = queryParams["response"]
        val error    = queryParams["error"]

        response?.let {
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val decrypted = paymentRepo.handleDeepLinkResponse(it)
                    // Simple feedback:
                    val json = JSONObject(decrypted)
                    val result = json
                        .getJSONObject("SaleToPOIResponse")
                        .getJSONObject("PaymentResponse")
                        .getJSONObject("Response")
                        .getString("Result")
                    ToastUtils.showToast(
                        getApplication(),
                        "Refund Result: $result"
                    )
                } catch (e: Exception) {
                    ToastUtils.showToast(
                        getApplication(),
                        "Refund parse error: ${e.message}"
                    )
                }
            }
        } ?: run {
            _refundState.value = RefundState(
                error = error ?: "Unknown refund error"
            )
        }
    }

    /**
     * Build the exact JSON you posted, substituting in the original
     * TransactionID/TimeStamp and refund amount.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateRefundRequest(
        saleId: String,
        poiId: String,
        currency: String,
        amount: Double,
    ): String {
        val timestamp  = ZonedDateTime.now().format(DATE_FORMAT)
        val serviceId  = UUID.randomUUID().toString().take(10)
        val transactionID: String = "Luke-Adyen-TTP-unreferencedRfeund-Test"

        return """
        {
          "SaleToPOIRequest": {
            "MessageHeader": {
              "ProtocolVersion":"3.0",
              "MessageClass":"Service",
              "MessageCategory":"Payment",
              "MessageType":"Request",
              "ServiceID":"$serviceId",
              "SaleID":"$saleId",
              "POIID":"$poiId",
            },
            "PaymentRequest": {
              "SaleData": {
                "SaleTransactionID": {
                  "TimeStamp":"$timestamp",
                  "TransactionID":"$transactionID"
                }
              },
              "PaymentTransaction": {
                "AmountsReq": {
                  "Currency":"$currency",
                  "RequestedAmount":$amount
                }
              },
              "PaymentData": {
                "PaymentType":"Refund"
              }
            }
          }
        }
        """.trimIndent()
    }
}


