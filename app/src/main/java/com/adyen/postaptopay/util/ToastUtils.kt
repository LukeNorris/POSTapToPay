package com.adyen.postaptopay.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object ToastUtils {
    private const val TAG = "ToastUtils"

    fun showToast(context: Context, message: String, duration: Long = 3000L) {  // Default duration set to 3000 milliseconds
        Log.d(TAG, "Preparing to show toast: $message")

        Handler(Looper.getMainLooper()).post {
            Log.d(TAG, "Showing toast: $message")
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast.show()

            // Dismiss the toast after a custom duration
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Dismissing toast: $message")
                toast.cancel()
            }, duration)
        }
    }
}
