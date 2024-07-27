package com.adyen.postaptopay.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.adyen.postaptopay.BuildConfig
import com.adyen.postaptopay.util.DeepLinkUtils
import com.adyen.postaptopay.util.NexoCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64
import org.json.JSONException

class PaymentRepository(private val context: Context) {

    private val nexoCrypto = NexoCrypto(BuildConfig.PASSPHRASE.toCharArray())

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
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data =
                Uri.parse("adyenpayments://nexo?request=$nexoRequestBase64&returnUrl=$returnUrlEncoded")
        }
        DeepLinkUtils.openDeepLink(activity, intent.data!!)
    }




    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun handleDeepLinkResponse(response: String?): String {
        return withContext(Dispatchers.Default) {
            try {
                Log.d("PaymentRepository", "Starting handleDeepLinkResponse")
                val decodedResponse = String(Base64.getDecoder().decode(response), Charsets.UTF_8)
                Log.d("PaymentRepository", "Decoded Response: $decodedResponse")

                // Parse the decoded response into a JSON object
                val decodedResponseJson = JSONObject(decodedResponse)

                // Convert the modified decoded response JSON back to string
                val modifiedDecodedResponse = decodedResponseJson.toString()

                val decryptedNexoResponse = nexoCrypto.decrypt_and_validate_hmac(
                    modifiedDecodedResponse.toByteArray(Charsets.UTF_8),
                    BuildConfig.KEY_IDENTIFIER,
                    BuildConfig.KEY_VERSION.toLong()
                )
                Log.d("PaymentRepository", "Decrypted Nexo Response: ${String(decryptedNexoResponse.packet, Charsets.UTF_8)}")

                // Extract ByteArray from the decrypted response
                val decryptedNexoResponseBytes = decryptedNexoResponse.packet

                // Convert ByteArray to String
                val decryptedNexoResponseString = String(decryptedNexoResponseBytes, Charsets.UTF_8)

                // Verifying if decryptedNexoResponseString is a valid JSON string
                if (decryptedNexoResponseString.isEmpty()) {
                    throw JSONException("Decrypted Nexo Response is null or empty")
                }

                decryptedNexoResponseString
            } catch (e: Exception) {
                Log.e("PaymentRepository", "Error in handleDeepLinkResponse", e)
                throw e
            }
        }
    }

}
