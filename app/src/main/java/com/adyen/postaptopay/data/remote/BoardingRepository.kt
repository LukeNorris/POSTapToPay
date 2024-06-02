package com.adyen.postaptopay.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import java.io.IOException
/*import com.adyen.postaptopay.BuildConfig*/

data class BoardingResponse(
    val installationId: String,
    val boardingToken: String
)

class BoardingRepository {

    private val client = OkHttpClient()

    fun generateBoardingToken(boardingRequestToken: String): BoardingResponse? {
        /*val url = "https://management-test.adyen.com/v1/merchants/${BuildConfig.ADYEN_MERCHANT_ACCOUNT}/generatePaymentsAppBoardingToken"*/
        val url = "https://management-test.adyen.com/v1/merchants/AdyenAccount223ECOM/generatePaymentsAppBoardingToken"
        val jsonBody = JSONObject().put("boardingRequestToken", boardingRequestToken).toString()
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            /*.addHeader("x-api-key", BuildConfig.ADYEN_API_KEY)*/
            .addHeader("x-api-key","AQExhmfxL4LIaBNEw0m/n3Q5qf3VaY9UCJ1rW2ZZ03a/zDQYC5UOqdp/KudqUlSejGF+fhDBXVsNvuR83LVYjEgiTGAH-6p0p0SQFQGPRJQzfX3yoFTh1Ou0ppDxYbe/Nmj33I68=-i1i(ez33_mRy+}U7J9h")
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
