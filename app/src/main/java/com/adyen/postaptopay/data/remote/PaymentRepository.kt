package com.adyen.postaptopay.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.adyen.postaptopay.BuildConfig
import com.adyen.postaptopay.data.local.TransactionRepository
import com.adyen.postaptopay.util.DeepLinkUtils
import com.adyen.postaptopay.util.NexoCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64
import org.json.JSONException

/**
 * Repository handling Nexo payment deep-link requests and responses.
 * Now also extracts POITransactionID and persists the last 5 transactions.
 */
class PaymentRepository(private val context: Context) {

    private val nexoCrypto = NexoCrypto(BuildConfig.PASSPHRASE.toCharArray())
    private val transactionRepo = TransactionRepository(context)

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPaymentLink(nexoRequest: String, activity: AppCompatActivity) {
        val returnUrl = "merchantscheme://com.merchant.companion/payment"
        val returnUrlEncoded =
            URLEncoder.encode(returnUrl, Charsets.UTF_8.name())  // Ensure URL is encoded correctly

        val encryptedNexoRequest = nexoCrypto.encrypt_and_hmac(
            nexoRequest.toByteArray(),
            BuildConfig.KEY_IDENTIFIER,
            BuildConfig.KEY_VERSION.toLong()
        )
        val nexoRequestBase64 = Base64.getEncoder().encodeToString(encryptedNexoRequest)
        Log.d("PaymentRepository", "Encoded nexo request: $nexoRequestBase64")

        val scheme = BuildConfig.SCHEME_NAME
        val uriString = "$scheme://nexo?request=$nexoRequestBase64&returnUrl=$returnUrlEncoded"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uriString)
        }
        DeepLinkUtils.openDeepLink(activity, intent.data!!)
    }

    /**
     * Decrypts the response, parses out POITransactionID and payment details,
     * and saves the transaction to SharedPreferences (last 5 max).
     * Returns the raw decrypted Nexo response JSON string.
     */
    // ← new: holds the last‐seen reversal Result (e.g. "Success"/"Failure"), or null otherwise
    private var lastReversalResult: String? = null

    /** ← new: call this from your VM after handleDeepLinkResponse(...) */
    fun getLastReversalResult(): String? = lastReversalResult
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun handleDeepLinkResponse(response: String?): String {
        return withContext(Dispatchers.Default) {
            try {
                Log.d("PaymentRepository", "Starting handleDeepLinkResponse")
                // Base64-decode the URL param
                val decodedResponse = String(Base64.getDecoder().decode(response), Charsets.UTF_8)
                Log.d("PaymentRepository", "Decoded Response: $decodedResponse")

                // Parse and re-stringify to ensure valid JSON
                val decodedJson = JSONObject(decodedResponse)
                val readyForDecrypt = decodedJson.toString()

                // Decrypt + validate HMAC
                val decrypted = nexoCrypto.decrypt_and_validate_hmac(
                    readyForDecrypt.toByteArray(Charsets.UTF_8),
                    BuildConfig.KEY_IDENTIFIER,
                    BuildConfig.KEY_VERSION.toLong()
                )
                val decryptedJsonString = String(decrypted.packet, Charsets.UTF_8)
                Log.d("PaymentRepository", "Decrypted Nexo Response: $decryptedJsonString")

                // Parse decrypted JSON
                val decryptedResponseJson = JSONObject(decryptedJsonString)

                // Extract SaleToPOIResponse → PaymentResponse
                val saleToPOI = decryptedResponseJson.optJSONObject("SaleToPOIResponse")

                //look for reversal first
                val reversal  = saleToPOI?.optJSONObject("ReversalResponse")
                if (reversal != null) {
                    val responseObj = reversal.optJSONObject("Response")
                    val result      = responseObj?.optString("Result")
                    lastReversalResult = result
                    Log.d("PaymentRepository", "ReversalResult = $result")
                    // short-circuit: return the raw JSON as before
                    return@withContext decryptedJsonString
                } else {
                    // not a reversal, clear any stale value
                    lastReversalResult = null
                }


                val paymentResponse = saleToPOI?.optJSONObject("PaymentResponse")

                // Extract POITransactionID (TransactionID + TimeStamp)
                val poiData = paymentResponse?.optJSONObject("POIData")
                val poiObj  = poiData?.optJSONObject("POITransactionID")
                val txId    = poiObj?.optString("TransactionID")
                val ts      = poiObj?.optString("TimeStamp")

                // Extract authorized amount & currency
                val paymentResult = paymentResponse?.optJSONObject("PaymentResult")
                val amountsResp   = paymentResult?.optJSONObject("AmountsResp")
                val authorizedAmt = amountsResp?.optDouble("AuthorizedAmount")
                val currency      = amountsResp?.optString("Currency")

                // Persist if valid
                if (!txId.isNullOrEmpty() && !ts.isNullOrEmpty()) {
                    transactionRepo.addTransaction(
                        TransactionRepository.Transaction(
                            id        = txId,
                            timestamp = ts,
                            amount    = authorizedAmt,
                            currency  = currency
                        )
                    )
                    Log.d("PaymentRepository", "Saved transaction $txId at $ts")
                } else {
                    Log.d("PaymentRepository", "No POITransactionID found to save.")
                }

                // Optional: Log other fields
                decryptedResponseJson.optString("MessagePayload")?.takeIf { it.isNotEmpty() }?.let {
                    Log.d("PaymentRepository", "MessagePayload: $it")
                }
                decryptedResponseJson.optString("PaymentInstrumentData")?.takeIf { it.isNotEmpty() }?.let {
                    Log.d("PaymentRepository", "PaymentInstrumentData: $it")
                }

                // Return the raw decrypted JSON
                decryptedJsonString

            } catch (e: Exception) {
                Log.e("PaymentRepository", "Error in handleDeepLinkResponse", e)
                throw e
            }
        }
    }
}
