package com.adyen.postaptopay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adyen.postaptopay.presentation.BoardingViewModel
import com.adyen.postaptopay.presentation.PaymentViewModel
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Home(
    boardingViewModel: BoardingViewModel,
    paymentViewModel: PaymentViewModel,
    activity: AppCompatActivity
) {
    val apiResponse by boardingViewModel.apiResponse.collectAsState(initial = null)
    val isLoading by boardingViewModel.isLoading.collectAsState()
    val error by boardingViewModel.error.collectAsState()

    val paymentIsLoading by paymentViewModel.paymentIsLoading.collectAsState()
    val paymentError by paymentViewModel.paymentError.collectAsState()

    if (isLoading || paymentIsLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(300.dp))
            Text(
                text = "Price: 12 EUR",
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    Log.d("Pay", "Pay button clicked")
                    paymentViewModel.makePayment(activity)
                }) {
                    Text("Pay")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    boardingViewModel.checkBoardingStatus(activity)
                }) {
                    Text("Boarding check")
                }
                error?.let {
                    Text("Error: $it", color = Color.Red)
                    ToastUtils.showToast(activity, "Error: $it", 5000)
                } ?: paymentError?.let {
                    Text("Error: $it", color = Color.Red)
                    ToastUtils.showToast(activity, "Payment Error: $it", 5000)
                } ?: apiResponse?.let {
                    Text("Boarding successful.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Filter logcat for BoardingRepository to see installationId and boardingToken from Adyen")
                }
            }
        }
    }
}
