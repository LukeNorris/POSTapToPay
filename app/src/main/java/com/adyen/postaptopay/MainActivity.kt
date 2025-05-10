package com.adyen.postaptopay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adyen.postaptopay.presentation.composables.TransactionsScreen
import com.adyen.postaptopay.presentation.composables.Home
import com.adyen.postaptopay.presentation.viewmodels.BoardingViewModel
import com.adyen.postaptopay.presentation.viewmodels.PaymentViewModel
import com.adyen.postaptopay.presentation.viewmodels.ReferencedRefundViewModel
import com.adyen.postaptopay.ui.theme.POSTapToPayTheme
import com.adyen.postaptopay.util.DeepLinkUtils
import com.adyen.postaptopay.util.ToastUtils

class MainActivity : AppCompatActivity() {
    private val boardingViewModel: BoardingViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()
    private val referencedRefundViewModel: ReferencedRefundViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        supportActionBar?.hide()
        title = BuildConfig.APP_LABEL

        setContent {
            POSTapToPayTheme {
                AppNavHost(
                    boardingViewModel,
                    paymentViewModel,
                    activity = this
                )
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
                it.containsKey("boarded")  ->
                    boardingViewModel.handleDeepLinkResponse(it, this)
                it.containsKey("response") -> {
                    paymentViewModel.handleDeepLinkResponse(it)
                    referencedRefundViewModel.handleDeepLinkResponse(it)
                }
                else -> {
                    Log.d("DeepLinkResponse", "Unknown deep link host or missing query parameters.")
                    ToastUtils.showToast(
                        this,
                        "Unknown deep link host or missing query parameters."
                    )
                }
            }
        } ?: run {
            Log.d("DeepLinkResponse", "No data received in intent")
            ToastUtils.showToast(this, "No data received in intent")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    boardingViewModel: BoardingViewModel,
    paymentViewModel: PaymentViewModel,
    activity: AppCompatActivity
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") {
            Home(
                boardingViewModel  = boardingViewModel,
                paymentViewModel   = paymentViewModel,
                navController      = navController,
                activity           = activity
            )
        }
        composable("transactions") {
            TransactionsScreen(navController)
        }
    }
}
