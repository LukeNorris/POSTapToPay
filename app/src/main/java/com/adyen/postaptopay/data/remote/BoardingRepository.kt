package com.adyen.postaptopay.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import java.io.IOException
import com.adyen.postaptopay.BuildConfig

data class BoardingResponse(
    val installationId: String,
    val boardingToken: String
)

class BoardingRepository {

    private val client = OkHttpClient()

    fun generateBoardingToken(boardingRequestToken: String): BoardingResponse? {
        val url = "https://management-test.adyen.com/v1/merchants/${BuildConfig.ADYEN_MERCHANT_ACCOUNT}/generatePaymentsAppBoardingToken"
        val jsonBody = JSONObject().put("boardingRequestToken", boardingRequestToken).toString()
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        Log.d("BoardingRepository", "generateBoardingToken: ")

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", BuildConfig.ADYEN_API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val jsonResponse = JSONObject(it)
                    BoardingResponse(
                        installationId = jsonResponse.getString("installationId"),
                        boardingToken = jsonResponse.getString("boardingToken")
                    )
                }
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }
}
