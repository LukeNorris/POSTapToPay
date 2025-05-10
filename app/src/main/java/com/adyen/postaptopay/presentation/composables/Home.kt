package com.adyen.postaptopay.presentation.composables

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.runtime.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.adyen.postaptopay.BuildConfig
import com.adyen.postaptopay.presentation.viewmodels.BoardingViewModel
import com.adyen.postaptopay.presentation.viewmodels.PaymentViewModel
import com.adyen.postaptopay.ui.theme.POSTapToPayTheme
import com.adyen.postaptopay.util.ToastUtils
import com.adyen.postaptopay.R
import androidx.compose.ui.res.painterResource


private enum class BottomTab { NONE, TRANSACTIONS, BOARDING }

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Home(
    boardingViewModel: BoardingViewModel,
    paymentViewModel: PaymentViewModel,
    navController: NavController,
    activity: AppCompatActivity
) {
    var currentTab by remember { mutableStateOf(BottomTab.NONE) }

    POSTapToPayTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { /* optional back button */ },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp) // overall left/right inset
                        ) {
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 12.dp)       // space between logo+demo and your title
                            ) {
                                Image(
                                    painter            = painterResource(R.drawable.digital_adyen_green_rgb),
                                    contentDescription = "Adyen Logo",
                                    modifier           = Modifier
                                        .size(100.dp)             // shrink to fit the bar
                                )
                                /*Text(
                                    text  = "Demo App",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )*/
                            }
                            Text(
                                text     = BuildConfig.APP_LABEL,
                                style    = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .weight(1f)                // take up the rest of the row
                                    .padding(start = 4.dp)     // a tiny gap from the column
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )


            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == BottomTab.TRANSACTIONS,
                        onClick = {
                            currentTab = BottomTab.TRANSACTIONS
                            navController.navigate("transactions")
                        },
                        icon = { Icon(Icons.Filled.List, contentDescription = "Transactions") },
                        label = { Text("Transactions") }
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomTab.BOARDING,
                        onClick = {
                            currentTab = BottomTab.BOARDING
                            boardingViewModel.checkBoardingStatus(activity)
                        },
                        icon = { Icon(Icons.Filled.Refresh, contentDescription = "Boarding") },
                        label = { Text("Boarding") }
                    )
                }
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
                            .padding(paddingValues)
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

                        // Your NumberInput composable
                        NumberInput(
                            value = paymentState.price,
                            onValueChange = { paymentViewModel.updatePrice(it) },
                            modifier = Modifier.width(260.dp)
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                Log.d("Pay", "Pay button clicked")
                                paymentState.price.toDoubleOrNull()?.let {
                                    paymentViewModel.makePayment(activity, paymentState.price, "Normal")
                                } ?: ToastUtils.showToast(activity, "Please enter a valid price", 5000)
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
                                paymentState.price.toDoubleOrNull()?.let {
                                    paymentViewModel.makePayment(activity, paymentState.price, "Refund")
                                } ?: ToastUtils.showToast(activity, "Please enter a valid price", 5000)
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

                        Spacer(modifier = Modifier.height(30.dp))

                        boardingState.error?.let {
                            Text("Error: $it", color = Color.Red)
                            ToastUtils.showToast(activity, "Error: $it", 5000)
                        } ?: paymentState.error?.let {
                            Text("Error: $it", color = Color.Red)
                            ToastUtils.showToast(activity, "Payment Error: $it", 5000)
                        }
                    }
                }
            }
        )
    }
}
