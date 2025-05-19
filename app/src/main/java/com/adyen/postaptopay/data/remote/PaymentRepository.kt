/*package com.adyen.postaptopay.data.remote

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
        val returnUrlEncoded = URLEncoder.encode(returnUrl, Charsets.UTF_8.name())

        // Encrypt the NEXO request
        val encryptedNexoRequest = nexoCrypto.encrypt_and_hmac(
            nexoRequest.toByteArray(),
            BuildConfig.KEY_IDENTIFIER,
            BuildConfig.KEY_VERSION.toLong()
        )

        // Base64URL-encode the encrypted request
        val nexoRequestBase64Url = Base64.getUrlEncoder().encodeToString(encryptedNexoRequest)
        Log.d("PaymentRepository", "Encoded NEXO request: $nexoRequestBase64Url")

        // Build the App Link URL
        val appLinkUri = Uri.parse("https://www.adyen.com/test/nexo")
            .buildUpon()
            .appendQueryParameter("request", nexoRequestBase64Url)
            .appendQueryParameter("returnUrl", returnUrlEncoded)
            .build()

        Log.d("PaymentRepository", "Launching App Link: $appLinkUri")

        // Create intent and set Adyen Payments app package
        val intent = Intent(Intent.ACTION_VIEW, appLinkUri).apply {
            setPackage("com.adyen.ipp.mobile.companion.test") // Change to .prod for production
        }

        // Check if the Adyen app can handle the intent
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            Log.e("PaymentRepository", "Adyen Payments app not found or App Link not supported.")
        }
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
*/

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
import com.adyen.postaptopay.util.NexoCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64

/**
 * Handles NEXO payment App Link creation and response decryption.
 * Also persists the last 5 transactions with POITransactionID.
 */
class PaymentRepository(private val context: Context) {

    private val nexoCrypto = NexoCrypto(BuildConfig.PASSPHRASE.toCharArray())
    private val transactionRepo = TransactionRepository(context)

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPaymentLink(nexoRequest: String, activity: AppCompatActivity) {
        val returnUrl = "merchantscheme://com.merchant.companion/payment"
        val encrypted = nexoCrypto.encrypt_and_hmac(
            nexoRequest.toByteArray(),
            BuildConfig.KEY_IDENTIFIER,
            BuildConfig.KEY_VERSION.toLong()
        )

        val requestParam = Base64.getUrlEncoder().encodeToString(encrypted)
        val appLink = Uri.parse("${BuildConfig.APP_LINK_URL}/nexo").buildUpon()
            .appendQueryParameter("request", requestParam)
            .appendQueryParameter("returnUrl", returnUrl)
            .build()

        Log.d(TAG, "Launching payment App Link: $appLink")

        Intent(Intent.ACTION_VIEW, appLink).apply {
            setPackage("com.adyen.ipp.mobile.companion.test")
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }.also { intent ->
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                Log.e(TAG, "Adyen Payments app not found to handle App Link")
            }
        }
    }

    private var lastReversalResult: String? = null
    fun getLastReversalResult(): String? = lastReversalResult

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun handleDeepLinkResponse(responseParam: String?): String = withContext(Dispatchers.Default) {
        val rawJson = try {
            val decoded = String(Base64.getDecoder().decode(responseParam), Charsets.UTF_8)
            val decrypted = nexoCrypto.decrypt_and_validate_hmac(
                decoded.toByteArray(Charsets.UTF_8),
                BuildConfig.KEY_IDENTIFIER,
                BuildConfig.KEY_VERSION.toLong()
            )
            String(decrypted.packet, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting NEXO response", e)
            throw e
        }

        JSONObject(rawJson).optJSONObject("SaleToPOIResponse")?.let { saleToPOI ->
            saleToPOI.optJSONObject("ReversalResponse")?.optJSONObject("Response")?.optString("Result")?.also {
                lastReversalResult = it
                Log.d(TAG, "ReversalResult=$it")
                return@withContext rawJson
            }

            lastReversalResult = null

            val paymentResponse = saleToPOI.optJSONObject("PaymentResponse")

            // (3) extract POITransactionID
            val poiObj = paymentResponse
                ?.optJSONObject("POIData")
                ?.optJSONObject("POITransactionID")

            val txId = poiObj?.optString("TransactionID")
            val ts   = poiObj?.optString("TimeStamp")

            // (4) extract amount + currency
            val amountsResp   = paymentResponse
                ?.optJSONObject("PaymentResult")
                ?.optJSONObject("AmountsResp")

            val authorizedAmt = amountsResp?.optDouble("AuthorizedAmount")
            val currency      = amountsResp?.optString("Currency")

            // ── CONDITIONAL SAVE ───────────────────────────────────────────
            if (!txId.isNullOrEmpty()
                && !ts.isNullOrEmpty()
                && authorizedAmt != null
                && currency != null
                && authorizedAmt > 0
            ) {
                transactionRepo.addTransaction(
                    TransactionRepository.Transaction(
                        id        = txId,
                        timestamp = ts,
                        amount    = authorizedAmt,
                        currency  = currency
                    )
                )
                Log.d(TAG, "Saved transaction $txId at $ts for $authorizedAmt $currency")
            } else {
                Log.d(
                    TAG, "Skipping save: " +
                            "txId=$txId, ts=$ts, amt=$authorizedAmt, cur=$currency"
                )
            }
            // ────────────────────────────────────────────────────────────────
        }

        rawJson
    }

    companion object {
        private const val TAG = "PaymentRepository"
    }
}
