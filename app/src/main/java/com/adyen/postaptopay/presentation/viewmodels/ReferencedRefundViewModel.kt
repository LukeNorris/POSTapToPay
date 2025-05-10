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
 * ViewModel to trigger a referenced refund (reversal) via deep-link.
 */
class ReferencedRefundViewModel(application: Application) : AndroidViewModel(application) {

    private val installationRepo = InstallationIdRepository(application)
    private val paymentRepo      = PaymentRepository(application)
    private val txRepo           = TransactionRepository(application)

    private val _refundState = MutableStateFlow(RefundState())
    val refundState: StateFlow<RefundState> = _refundState

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String,String>) {
        val response = queryParams["response"] ?: return
        viewModelScope.launch {
            _refundState.value = RefundState(isLoading = true)
            // reuse the same decryption + parsing path
            val json = paymentRepo.handleDeepLinkResponse(response)
            val result = paymentRepo.getLastReversalResult()
            if (result == null) {
                // not a reversal → bail out
                _refundState.value = RefundState(isLoading = false)
                return@launch
            }
            if (result == "Success") {
                // mark the last‐ref transaction as refunded
                txRepo.getLastTransactions().firstOrNull()?.id
                    ?.let { txRepo.markTransactionRefunded(it) }
                ToastUtils.showToast(getApplication(), "Refund successful")
                _refundState.value = RefundState(isLoading = false)
            } else {
                ToastUtils.showToast(getApplication(), "Refund failed")
                _refundState.value = RefundState(isLoading = false, error="Refund failed")
            }
        }
    }

    /*@RequiresApi(Build.VERSION_CODES.O)
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    */
    fun clearTransactionHistory(){
        txRepo.clearHistory()
    }
    /**
     * make referenced refund (reversal).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun makeReferencedRefund(
        activity: AppCompatActivity,
        currency: String,
        amount: Double
    ) {
        viewModelScope.launch {
            _refundState.value = RefundState(isLoading = true)

            // 1) get POIID
            val poiId = installationRepo.getInstallationId()
            if (poiId.isNullOrEmpty()) {
                _refundState.value = RefundState(isLoading = false, error = "Missing POI ID")
                return@launch
            }

            // 2) pick the most recent transaction
            val original = txRepo.getLastTransactions().firstOrNull()
            if (original == null) {
                _refundState.value = RefundState(isLoading = false, error = "No transaction to refund")
                return@launch
            }

            // 3) build reversal JSON
            val nexoJson = generateReversalRequest(
                saleId        = "My cash register",
                poiId         = poiId,
                origTxId      = original.id,
                origTimeStamp = original.timestamp
            )
            Log.d("ReferencedRefundVM", "Reversal JSON: $nexoJson")

            // 4) deep-link
            paymentRepo.createPaymentLink(nexoJson, activity)
            _refundState.value = RefundState(isLoading = false)
        }
    }

    /**
     * Build a Nexo reversal request JSON using the original transaction.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateReversalRequest(
        saleId: String,
        poiId: String,
        origTxId: String,
        origTimeStamp: String
    ): String {
        val serviceId = UUID.randomUUID().toString().take(10)

        return """
        |{
        |  "SaleToPOIRequest": {
        |    "MessageHeader": {
        |      "ProtocolVersion": "3.0",
        |      "MessageClass": "Service",
        |      "MessageCategory": "Reversal",
        |      "MessageType": "Request",
        |      "SaleID": "$saleId",
        |      "ServiceID": "$serviceId",
        |      "POIID": "$poiId"
        |    },
        |    "ReversalRequest": {
        |      "OriginalPOITransaction": {
        |        "POITransactionID": {
        |          "TimeStamp": "$origTimeStamp",
        |          "TransactionID": "$origTxId"
        |        }
        |      },
        |      "ReversalReason": "MerchantCancel"
        |    }
        |  }
        |}
    """.trimMargin("|")
    }
}

