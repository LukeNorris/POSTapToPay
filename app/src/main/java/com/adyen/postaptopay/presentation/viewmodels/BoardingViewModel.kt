package com.adyen.postaptopay.presentation.viewmodels

import android.app.Application
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
import com.adyen.postaptopay.util.DeepLinkUtils
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
            Log.d("BoardingViewModel", "Current Installation ID: $installationId")
            if (installationId.isNullOrEmpty()) {
                Log.d("BoardingViewModel", "No installation ID found. Initiating boarding process.")
                initiateBoarding(activity)
            } else {
                Log.d("BoardingViewModel", "App is already boarded. Installation ID: $installationId")
                ToastUtils.showToast(activity, "App is already boarded. Installation ID: $installationId")
            }
        }
    }

    private fun initiateBoarding(activity: AppCompatActivity) {
        val returnUrl = "merchantscheme://com.merchant.companion/onboarding"
        val encodedParams = DeepLinkUtils.encodeUriParameters(mapOf("returnUrl" to returnUrl))
        val scheme = BuildConfig.SCHEME_NAME
        val uri = Uri.parse("$scheme://boarded?$encodedParams")
        DeepLinkUtils.openDeepLink(activity, uri)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDeepLinkResponse(queryParams: Map<String, String>, activity: AppCompatActivity) {
        val boardingRequestToken = queryParams["boardingRequestToken"]
        val installationId = queryParams["installationId"]
        val boarded = queryParams["boarded"]
        Log.d(
            "DeepLinkResponse",
            "Boarding parameters: boardingRequestToken=$boardingRequestToken, installationId=$installationId, boarded=$boarded"
        )

        when (boarded) {
            "true" -> {
                Log.d("BoardingViewModel", "App is already boarded. Boarded parameter in URL is true")
                if (installationId != null) {
                    saveInstallationId(installationId)
                    ToastUtils.showToast(activity, "App is already boarded. Installation ID: $installationId")
                } else {
                    Log.e("BoardingViewModel", "Received boarding status as true but no installation ID.")
                    _boardingState.value = _boardingState.value.copy(error = "Installation ID is missing despite boarded being true.")
                    ToastUtils.showToast(activity, "Installation ID is missing despite boarded being true.")
                }
            }
            "false" -> {
                if (boardingRequestToken != null) {
                    generateBoardingToken(boardingRequestToken, activity)
                    Log.d("BoardingViewModel", "generateBoardingToken called")
                } else {
                    Log.e("BoardingViewModel", "Boarding request token is null.")
                    _boardingState.value = _boardingState.value.copy(error = "Boarding request token is null.")
                    ToastUtils.showToast(activity, "Boarding request token is null.")
                }
            }
            else -> {
                Log.e("BoardingViewModel", "Unhandled deep link response.")
                _boardingState.value = _boardingState.value.copy(error = "Unhandled deep link response.")
                ToastUtils.showToast(activity, "Unhandled deep link response.")
            }
        }
    }

    private fun saveInstallationId(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("BoardingViewModel", "Saving Installation ID: $installationId")
            installationIdRepository.setInstallationId(installationId)
            Log.d("BoardingViewModel", "Installation ID saved: $installationId")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateBoardingToken(boardingRequestToken: String, activity: AppCompatActivity) {
        viewModelScope.launch {
            Log.d("BoardingViewModel", "Starting to generate boarding token.")
            _boardingState.value = _boardingState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                Log.d("BoardingViewModel", "Sending network request to generate boarding token")
                val response: BoardingResponse? = withContext(Dispatchers.IO) {
                    repository.generateBoardingToken(boardingRequestToken)
                }
                Log.d("BoardingViewModel", "Network request completed with response: $response")
                response?.let {
                    Log.d("BoardingViewModel", "Boarding token generated successfully: ${it.boardingToken}")
                    _boardingState.value = _boardingState.value.copy(apiResponse = it.boardingToken)
                    sendBoardingTokenToApp(it.boardingToken, activity)
                } ?: run {
                    Log.e("BoardingViewModel", "Failed to generate boarding token.")
                    _boardingState.value = _boardingState.value.copy(error = "Failed to generate boarding token")
                    ToastUtils.showToast(activity, "Failed to generate boarding token")
                }
            } catch (e: Exception) {
                Log.e("BoardingViewModel", "Error generating boarding token", e)
                _boardingState.value = _boardingState.value.copy(
                    error = e.localizedMessage,
                    apiResponse = null
                )
                ToastUtils.showToast(activity, "Error generating boarding token: ${e.localizedMessage}")
            } finally {
                _boardingState.value = _boardingState.value.copy(isLoading = false)
                Log.d("BoardingViewModel", "Finished generating boarding token.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendBoardingTokenToApp(boardingToken: String, activity: AppCompatActivity) {
        Log.d("BoardingViewModel", "Sending boarding token to app")
        val returnUrl = "merchantscheme://com.merchant.companion/onboarding"
        val returnUrlEncoded = URLEncoder.encode(returnUrl, Charsets.UTF_8.name())
        val boardingTokenBase64 = Base64.getEncoder().encodeToString(boardingToken.toByteArray())
        val scheme = BuildConfig.SCHEME_NAME
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("$scheme://board?boardingToken=$boardingTokenBase64&returnUrl=$returnUrlEncoded")
        }
        DeepLinkUtils.openDeepLink(activity, intent.data!!)
    }
}

