// HomeScreen.kt
package com.adyen.postaptopay.presentation

import android.os.Build
import android.util.Log
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
import com.adyen.postaptopay.presentation.viewmodels.BoardingViewModel
import com.adyen.postaptopay.presentation.viewmodels.PaymentViewModel
import com.adyen.postaptopay.util.ToastUtils

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
