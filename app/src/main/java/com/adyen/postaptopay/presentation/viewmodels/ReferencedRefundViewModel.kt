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
import com.adyen.postaptopay.util.ToastUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    // REMOVED: private var transactionIdBeingRefunded: String? = null
    // This variable was losing its state. We will now rely on txRepo.getPendingRefundTxId()

    private val _refundState = MutableStateFlow(RefundState())
    val refundState: StateFlow<RefundState> = _refundState

    // StateFlow to expose transactions to the UI
    private val _transactions = MutableStateFlow<List<TransactionRepository.Transaction>>(emptyList())
    val transactions: StateFlow<List<TransactionRepository.Transaction>> = _transactions

    init {
        viewModelScope.launch {
            Log.d("ReferencedRefundVM", "ViewModel init: Loading initial transactions.")
            _transactions.value = txRepo.getLastTransactions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String,String>) {
        val response = queryParams["response"] ?: run {
            Log.e("ReferencedRefundVM", "handleDeepLinkResponse: 'response' query parameter is missing.")
            return
        }
        Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Raw response received.")

        // Retrieve the pending transaction ID from persistent storage
        val currentTransactionIdBeingRefunded = txRepo.getPendingRefundTxId()
        Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Retrieved pending TX ID from storage: $currentTransactionIdBeingRefunded")

        viewModelScope.launch {
            _refundState.value = RefundState(isLoading = true)
            Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Refund state set to loading.")

            val decryptedNexoResponseString = try {
                paymentRepo.handleDeepLinkResponse(response)
            } catch (e: Exception) {
                Log.e("ReferencedRefundVM", "Error decrypting deep link response: ${e.message}", e)
                ToastUtils.showToast(getApplication(), "Error parsing response: ${e.message}")
                _refundState.value = RefundState(isLoading = false, error="Error parsing response")
                // Clear the pending ID even on error to avoid stale state
                txRepo.savePendingRefundTxId(null)
                return@launch
            }
            Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Decrypted response received.")

            val result = paymentRepo.getLastReversalResult()
            Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Reversal result from paymentRepo: $result")

            if (result == null) {
                Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Not a reversal response. Bailing out.")
                _refundState.value = RefundState(isLoading = false)
                txRepo.savePendingRefundTxId(null) // Clear pending ID
                return@launch
            }

            if (result == "Success") {
                Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Reversal result is SUCCESS.")
                currentTransactionIdBeingRefunded?.let { txId ->
                    Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Attempting to mark transaction $txId as refunded.")
                    txRepo.markTransactionRefunded(txId)
                    txRepo.savePendingRefundTxId(null) // Clear the pending ID after successful marking
                    // IMPORTANT: After marking, refresh the list in the ViewModel
                    _transactions.value = txRepo.getLastTransactions() // THIS SHOULD TRIGGER UI UPDATE
                    Log.d("ReferencedRefundVM", "handleDeepLinkResponse: _transactions StateFlow updated.")
                } ?: run {
                    Log.e("ReferencedRefundVM", "handleDeepLinkResponse: currentTransactionIdBeingRefunded was null, could not mark transaction. THIS IS THE PROBLEM IF IT STILL OCCURS. Check logs for why ID was not stored.")
                }
                ToastUtils.showToast(getApplication(), "Refund successful")
                _refundState.value = RefundState(isLoading = false)
                Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Refund successful, state set to false.")
            } else {
                Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Reversal result is FAILED.")
                ToastUtils.showToast(getApplication(), "Refund failed")
                _refundState.value = RefundState(isLoading = false, error="Refund failed")
                Log.d("ReferencedRefundVM", "handleDeepLinkResponse: Refund failed, state set to false.")
                txRepo.savePendingRefundTxId(null) // Clear pending ID on failure as well
            }
        }
    }

    fun clearTransactionHistory(){
        Log.d("ReferencedRefundVM", "Clearing transaction history.")
        txRepo.clearHistory()
        txRepo.savePendingRefundTxId(null) // Clear any pending ID if history is cleared
        _transactions.value = emptyList()
        Log.d("ReferencedRefundVM", "_transactions StateFlow cleared.")
    }

    /**
     * make referenced refund (reversal).
     * Now accepts the specific Transaction object to refund.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun makeReferencedRefund(
        activity: AppCompatActivity,
        transactionToRefund: TransactionRepository.Transaction
    ) {
        viewModelScope.launch {
            _refundState.value = RefundState(isLoading = true)
            Log.d("ReferencedRefundVM", "makeReferencedRefund: Refund state set to loading.")

            // 1) get POIID
            val poiId = installationRepo.getInstallationId()
            if (poiId.isNullOrEmpty()) {
                Log.e("ReferencedRefundVM", "makeReferencedRefund: Missing POI ID.")
                _refundState.value = RefundState(isLoading = false, error = "Missing POI ID")
                return@launch
            }

            // 2) Use the passed transaction directly
            val original = transactionToRefund
            // Store the ID in persistent storage immediately before deep-linking
            txRepo.savePendingRefundTxId(original.id)
            Log.d("ReferencedRefundVM", "makeReferencedRefund: Stored pending refund TX ID: ${original.id} for deep-link.")

            Log.d("ReferencedRefundVM", "makeReferencedRefund: Attempting to refund transaction ID: ${original.id} Timestamp: ${original.timestamp}")

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
            // No need to set isLoading to false here, as the deep link response will handle it
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