package com.adyen.postaptopay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.adyen.postaptopay.presentation.composables.Home
import com.adyen.postaptopay.presentation.viewmodels.BoardingViewModel
import com.adyen.postaptopay.presentation.viewmodels.PaymentViewModel
import com.adyen.postaptopay.ui.theme.POSTapToPayTheme
import com.adyen.postaptopay.util.DeepLinkUtils
import com.adyen.postaptopay.util.ToastUtils
import org.json.JSONObject
import java.util.Base64

class MainActivity : AppCompatActivity() {

    private val boardingViewModel: BoardingViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            POSTapToPayTheme {
                Home(boardingViewModel, paymentViewModel, this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleIntent(intent: Intent) {
        val queryParams = DeepLinkUtils.parseDeepLink(intent)
        Log.d("DeepLinkResponse", "Parsed query parameters: $queryParams")

        queryParams?.let {
            when {
                it.containsKey("boarded") -> handleBoardingDeepLink(it)
                it.containsKey("response") -> handlePaymentDeepLink(it)
                else -> {
                    Log.d("DeepLinkResponse", "Unknown deep link host or missing query parameters.")
                    ToastUtils.showToast(this, "Unknown deep link host or missing query parameters.")
                }
            }
        } ?: run {
            Log.d("DeepLinkResponse", "No data received in intent")
            ToastUtils.showToast(this, "No data received in intent")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleBoardingDeepLink(queryParams: Map<String, String>) {
        val boardingRequestToken = queryParams["boardingRequestToken"]
        val installationId = queryParams["installationId"]
        val boarded = queryParams["boarded"]
        Log.d("DeepLinkResponse", "Boarding parameters: boardingRequestToken=$boardingRequestToken, installationId=$installationId, boarded=$boarded")

        // Show toast for boarding result
        if (boarded != null && boarded.toBoolean()) {
            ToastUtils.showToast(this, "Boarding successful. Installation ID: $installationId")
        } else {
            ToastUtils.showToast(this, "Boarding failed or not completed.")
        }

        boardingViewModel.handleDeepLinkResponse(boardingRequestToken, boarded, installationId, this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handlePaymentDeepLink(queryParams: Map<String, String>) {
        val response = queryParams["response"]
        val error = queryParams["error"]

        response?.let {
            try {
                val firstDecodedResponse = String(Base64.getDecoder().decode(it), Charsets.UTF_8)
                val secondDecodedResponse = String(Base64.getDecoder().decode(firstDecodedResponse), Charsets.UTF_8)
                val jsonResponse = JSONObject(secondDecodedResponse)
                val saleToPOIResponse = jsonResponse.getJSONObject("SaleToPOIResponse")
                val paymentResponse = saleToPOIResponse.getJSONObject("PaymentResponse")

                if (paymentResponse.has("Response")) {
                    val responseObj = paymentResponse.getJSONObject("Response")
                    val result = responseObj.getString("Result")
                    ToastUtils.showToast(this, "Result: $result")
                } else {
                    ToastUtils.showToast(this, "No 'Response' found in PaymentResponse")
                }
            } catch (e: Exception) {
                ToastUtils.showToast(this, "Error parsing response: ${e.message}")
            }
        } ?: run {
            ToastUtils.showToast(this, "Response is null")
        }

        paymentViewModel.handleDeepLinkResponse(response, error)
    }

}


