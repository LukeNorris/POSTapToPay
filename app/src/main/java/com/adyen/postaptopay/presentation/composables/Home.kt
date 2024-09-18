package com.adyen.postaptopay.presentation.composables

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
import androidx.compose.ui.unit.sp
import com.adyen.postaptopay.BuildConfig
import com.adyen.postaptopay.presentation.viewmodels.BoardingViewModel
import com.adyen.postaptopay.presentation.viewmodels.PaymentViewModel
import com.adyen.postaptopay.ui.theme.POSTapToPayTheme
import com.adyen.postaptopay.util.ToastUtils

/*@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Home(
    boardingViewModel: BoardingViewModel,
    paymentViewModel: PaymentViewModel,
    activity: AppCompatActivity
) {
    POSTapToPayTheme {
        val boardingState by boardingViewModel.boardingState.collectAsState()

        val paymentState by paymentViewModel.paymentState.collectAsState()

        if (boardingState.isLoading || paymentState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(100.dp))
                Text("Enter Amount",
                    fontSize = 30.sp,
                    color = Color.Gray,
                )
                NumberInput(
                    value = paymentState.price,
                    onValueChange = { paymentViewModel.updatePrice(it) },
                    modifier = Modifier.width(260.dp)
                )
                Spacer(modifier = Modifier.height(30.dp))
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            Log.d("Pay", "Pay button clicked")
                            val priceValue = paymentState.price.toDoubleOrNull()
                            if (priceValue != null) {
                                paymentViewModel.makePayment(activity, paymentState.price, "Normal")
                            } else {
                                ToastUtils.showToast(activity, "Please enter a valid price", 5000)
                            }
                        },
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp), // Increase the height
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("PAY",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 25.sp // Increase the text size
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = {
                            Log.d("Refund", "Refund button clicked")
                            val priceValue = paymentState.price.toDoubleOrNull()
                            if (priceValue != null) {
                                paymentViewModel.makePayment(activity, paymentState.price, "Refund")
                            } else {
                                ToastUtils.showToast(activity, "Please enter a valid price", 5000)
                            }
                        },
                        modifier = Modifier
                            .width(250.dp)
                            .height(60.dp), // Increase the height
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("REFUND",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 25.sp // Increase the text size
                        )
                    }
                    Spacer(modifier = Modifier.height(120.dp))
                    Button(
                        onClick = {
                            boardingViewModel.checkBoardingStatus(activity)
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.width(150.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Boarding check", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    boardingState.error?.let {
                        Text("Error: $it", color = Color.Red)
                        ToastUtils.showToast(activity, "Error: $it", 5000)
                    } ?: paymentState.error?.let {
                        Text("Error: $it", color = Color.Red)
                        ToastUtils.showToast(activity, "Payment Error: $it", 5000)
                    } ?: boardingState.apiResponse?.let {
                        Text("Boarding successful.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Filter logcat for BoardingRepository to see installationId and boardingToken from Adyen")
                    }
                }
            }
        }
    }
}*/


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Home(
    boardingViewModel: BoardingViewModel,
    paymentViewModel: PaymentViewModel,
    activity: AppCompatActivity
) {
    POSTapToPayTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(BuildConfig.APP_LABEL) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            content = { paddingValues ->
                val boardingState by boardingViewModel.boardingState.collectAsState()
                val paymentState by paymentViewModel.paymentState.collectAsState()

                if (boardingState.isLoading || paymentState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(100.dp))
                        Text(
                            "Enter Amount",
                            fontSize = 30.sp,
                            color = Color.Gray
                        )
                        NumberInput(
                            value = paymentState.price,
                            onValueChange = { paymentViewModel.updatePrice(it) },
                            modifier = Modifier.width(260.dp)
                        )
                        Spacer(modifier = Modifier.height(30.dp))
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    Log.d("Pay", "Pay button clicked")
                                    val priceValue = paymentState.price.toDoubleOrNull()
                                    if (priceValue != null) {
                                        paymentViewModel.makePayment(activity, paymentState.price, "Normal")
                                    } else {
                                        ToastUtils.showToast(activity, "Please enter a valid price", 5000)
                                    }
                                },
                                modifier = Modifier
                                    .width(250.dp)
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    "PAY",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 25.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(
                                onClick = {
                                    Log.d("Refund", "Refund button clicked")
                                    val priceValue = paymentState.price.toDoubleOrNull()
                                    if (priceValue != null) {
                                        paymentViewModel.makePayment(activity, paymentState.price, "Refund")
                                    } else {
                                        ToastUtils.showToast(activity, "Please enter a valid price", 5000)
                                    }
                                },
                                modifier = Modifier
                                    .width(250.dp)
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    "REFUND",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 25.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(120.dp))
                            Button(
                                onClick = {
                                    boardingViewModel.checkBoardingStatus(activity)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Boarding check", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            boardingState.error?.let {
                                Text("Error: $it", color = Color.Red)
                                ToastUtils.showToast(activity, "Error: $it", 5000)
                            } ?: paymentState.error?.let {
                                Text("Error: $it", color = Color.Red)
                                ToastUtils.showToast(activity, "Payment Error: $it", 5000)
                            } ?: boardingState.apiResponse?.let {
                                Text("Boarding successful.")
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Filter logcat for BoardingRepository to see installationId and boardingToken from Adyen")
                            }
                        }
                    }
                }
            }
        )
    }
}

