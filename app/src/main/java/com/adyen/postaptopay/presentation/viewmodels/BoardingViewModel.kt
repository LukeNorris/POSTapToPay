package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adyen.postaptopay.BuildConfig
import com.adyen.postaptopay.data.local.InstallationIdRepository
import com.adyen.postaptopay.data.remote.BoardingRepository
import com.adyen.postaptopay.data.remote.BoardingResponse
import com.adyen.postaptopay.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Base64

class BoardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BoardingRepository()
    private val installationIdRepository = InstallationIdRepository(application)

    private val _boardingState = MutableStateFlow(BoardingState())
    val boardingState: StateFlow<BoardingState> = _boardingState

    fun checkBoardingStatus(activity: AppCompatActivity) {
        viewModelScope.launch {
            val installationId = withContext(Dispatchers.IO) {
                installationIdRepository.getInstallationId()
            }
            Log.d(TAG, "Current Installation ID: $installationId")
            if (installationId.isNullOrEmpty()) {
                initiateBoarding(activity)
            } else {
                ToastUtils.showToast(activity, "App is already boarded. Installation ID: $installationId")
            }
        }
    }

    private fun initiateBoarding(activity: AppCompatActivity) {
        val returnUrl = "merchantscheme://com.merchant.companion/onboarding"
        val appLinkUri = Uri.parse("${BuildConfig.APP_LINK_URL}/boarded").buildUpon()
            .appendQueryParameter("returnUrl", returnUrl)
            .build()

        val paymentAppPackage = "com.adyen.ipp.mobile.companion.test"
        val intent = Intent(Intent.ACTION_VIEW, appLinkUri).apply {
            setPackage(paymentAppPackage)
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            activity.startActivity(intent)
            Log.d(TAG, "App Link intent started: $appLinkUri")
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Adyen Payments app not installed or not responding.", e)
            ToastUtils.showToast(activity, "Please install the Adyen Payments app")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String, String>, activity: AppCompatActivity) {
        val boardingRequestToken = queryParams["boardingRequestToken"]
        val installationId = queryParams["installationId"]
        val boarded = queryParams["boarded"]
        Log.d(TAG, "Boarding parameters: boardingRequestToken=$boardingRequestToken, installationId=$installationId, boarded=$boarded")

        when (boarded) {
            "true" -> {
                installationId?.let {
                    saveInstallationId(it)
                    ToastUtils.showToast(activity, "App is already boarded. Installation ID: $it")
                } ?: run {
                    _boardingState.value = _boardingState.value.copy(error = "Installation ID missing despite boarded=true")
                    ToastUtils.showToast(activity, "Installation ID missing")
                }
            }
            "false" -> {
                boardingRequestToken?.let {
                    generateBoardingToken(it, activity)
                } ?: run {
                    _boardingState.value = _boardingState.value.copy(error = "Boarding request token is null")
                    ToastUtils.showToast(activity, "Boarding request token is null")
                }
            }
            else -> {
                _boardingState.value = _boardingState.value.copy(error = "Unhandled deep link response.")
                ToastUtils.showToast(activity, "Unhandled deep link response.")
            }
        }
    }

    private fun saveInstallationId(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            installationIdRepository.setInstallationId(installationId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateBoardingToken(boardingRequestToken: String, activity: AppCompatActivity) {
        viewModelScope.launch {
            _boardingState.value = _boardingState.value.copy(isLoading = true, error = null)
            try {
                val response: BoardingResponse? = withContext(Dispatchers.IO) {
                    repository.generateBoardingToken(boardingRequestToken)
                }
                response?.boardingToken?.let { token ->
                    _boardingState.value = _boardingState.value.copy(apiResponse = token)
                    val returnUrl = "merchantscheme://com.merchant.companion/onboarding"
                    val boardingTokenEncoded = Base64.getUrlEncoder().encodeToString(token.toByteArray())
                    val appLinkUri = Uri.parse("${BuildConfig.APP_LINK_URL}/board").buildUpon()
                        .appendQueryParameter("boardingToken", boardingTokenEncoded)
                        .appendQueryParameter("returnUrl", returnUrl)
                        .build()
                    val intent = Intent(Intent.ACTION_VIEW, appLinkUri).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    if (intent.resolveActivity(activity.packageManager) != null) {
                        activity.startActivity(intent)
                    } else {
                        ToastUtils.showToast(activity, "Adyen app not found")
                    }
                } ?: run {
                    _boardingState.value = _boardingState.value.copy(error = "Failed to generate boarding token")
                    ToastUtils.showToast(activity, "Failed to generate boarding token")
                }
            } catch (e: Exception) {
                _boardingState.value = _boardingState.value.copy(error = e.localizedMessage)
                ToastUtils.showToast(activity, "Error generating boarding token: ${e.localizedMessage}")
            } finally {
                _boardingState.value = _boardingState.value.copy(isLoading = false)
            }
        }
    }

    companion object {
        private const val TAG = "BoardingViewModel"
    }
}
