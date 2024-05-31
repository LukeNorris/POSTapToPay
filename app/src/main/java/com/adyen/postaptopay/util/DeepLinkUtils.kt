package com.adyen.postaptopay.util

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

object DeepLinkUtils {



    fun parseDeepLink(intent: Intent): Map<String, String>? {
        val uri = intent.data ?: return null
        Log.d("DeepLinkUtils", "Parsing URI: $uri")

        return uri.query?.split("&")?.associate {
            val (key, value) = it.split("=")
            Uri.decode(key) to Uri.decode(value)
        }
    }

    fun encodeUriParameters(params: Map<String, String>): String {
        return params.map { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")
    }

    fun openDeepLink(activity: AppCompatActivity, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
            }
            Log.d("DeepLinkUtils", "Opening deep link: $uri")
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                Log.d("DeepLinkUtils", "Deep link opened successfully")
            } else {
                Log.e("DeepLinkUtils", "No activity found to handle deep link: $uri")
            }
        } catch (e: Exception) {
            Log.e("DeepLinkUtils", "Error opening deep link", e)
        }
    }
}
