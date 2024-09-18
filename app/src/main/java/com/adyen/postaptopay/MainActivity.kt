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

class MainActivity : AppCompatActivity() {

    private val boardingViewModel: BoardingViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        supportActionBar?.hide()
        title = BuildConfig.APP_LABEL
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
                it.containsKey("boarded") -> boardingViewModel.handleDeepLinkResponse(it, this)  // Pass 'this' as the activity
                it.containsKey("response") -> paymentViewModel.handleDeepLinkResponse(it)
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



