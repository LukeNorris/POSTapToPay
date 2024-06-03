package com.adyen.postaptopay.data.local

import android.content.Context
import android.content.SharedPreferences

class InstallationIdRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val INSTALLATION_ID_KEY = "installationId"
    }

    fun getInstallationId(): String {
        return sharedPreferences.getString(INSTALLATION_ID_KEY, "") ?: ""
    }

    fun setInstallationId(installationId: String) {
        sharedPreferences.edit().putString(INSTALLATION_ID_KEY, installationId).apply()
    }

    fun clearInstallationId() {
        sharedPreferences.edit().remove(INSTALLATION_ID_KEY).apply()
    }
    // For debugging: Clear all SharedPreferences
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}
