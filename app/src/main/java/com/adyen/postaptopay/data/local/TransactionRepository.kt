package com.adyen.postaptopay.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple repository to keep track of the most recent 5 POI transactions
 * stored in SharedPreferences as a JSON array.
 */
class TransactionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG_HISTORY  = "TransactionRepo"
        private const val KEY_HISTORY  = "last5Transactions"
        private const val KEY_PENDING_REFUND_TX_ID = "pendingRefundTransactionId" // NEW KEY
        private const val MAX_SIZE     = 5
    }

    /**
     * Data class representing a single transaction record.
     */
    data class Transaction(
        val id: String,
        val timestamp: String,
        val amount: Double?   = null,
        val currency: String? = null,
        val isRefunded: Boolean = false
    )

    /**
     * Returns up to the last MAX_SIZE transactions, most recent first.
     */
    fun getLastTransactions(): List<Transaction> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        Log.d(TAG_HISTORY, "getLastTransactions: raw JSON = $raw")
        val arr  = JSONArray(raw)
        val list = mutableListOf<Transaction>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Transaction(
                id         = o.getString("id"),
                timestamp  = o.getString("timestamp"),
                amount     = o.optDouble("amount").takeIf { o.has("amount") },
                currency   = o.optString("currency", null),
                isRefunded = o.optBoolean("refunded", false)   // ← read it
            )
        }

        Log.d(TAG_HISTORY, "getLastTransactions: parsed ${list.size} transactions. First item refunded status: ${list.firstOrNull()?.isRefunded}")
        return list
    }

    /**
     * Adds or updates a transaction at the front of the list.
     */
    fun addTransaction(tx: Transaction) {
        Log.d(TAG_HISTORY, "addTransaction: input TX = $tx")
        val current = getLastTransactions().toMutableList()
        current.removeAll { it.id == tx.id }
        current.add(0, tx)
        if (current.size > MAX_SIZE) {
            val removed = current.removeAt(current.lastIndex)
            Log.d(TAG_HISTORY, "addTransaction: removed oldest TX = $removed")
        }
        saveList(current)
    }

    /**
     * Marks an existing transaction as refunded.
     * If the ID isn’t in the list, this is a no-op.
     */
    fun markTransactionRefunded(txId: String) {
        Log.d(TAG_HISTORY, "markTransactionRefunded: Attempting to mark TX ID: $txId as refunded.")
        val currentTransactions = getLastTransactions()
        val updated = currentTransactions.map {
            if (it.id == txId) {
                Log.d(TAG_HISTORY, "markTransactionRefunded: Found TX ID $txId. Setting isRefunded to true.")
                it.copy(isRefunded = true)
            } else {
                it
            }
        }
        saveList(updated)
        // Verify immediately after saving
        val savedTransactions = getLastTransactions()
        val isSuccessfullyMarked = savedTransactions.any { it.id == txId && it.isRefunded }
        Log.d(TAG_HISTORY, "markTransactionRefunded: TX ID $txId successfully marked as refunded in SharedPreferences: $isSuccessfullyMarked")
    }

    /**
     * Clears the entire transaction history.
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        Log.d(TAG_HISTORY, "clearHistory: cleared all transactions")
    }

    /**
     * Helper to persist the full list, including the refunded flag.
     */
    private fun saveList(list: List<Transaction>) {
        val arr = JSONArray().apply {
            list.forEach { t ->
                put(JSONObject().apply {
                    put("id",        t.id)
                    put("timestamp", t.timestamp)
                    t.amount?.let   { put("amount",   it) }
                    t.currency?.let { put("currency", it) }
                    put("refunded",  t.isRefunded)          // ← write it
                })
            }
        }
        prefs.edit()
            .putString(KEY_HISTORY, arr.toString())
            .apply()
        Log.d(TAG_HISTORY, "saveList: saved JSON = ${arr}")
    }

    /**
     * Stores the ID of the transaction that is currently undergoing a refund process.
     * This is used to persist the ID across potential process deaths.
     */
    fun savePendingRefundTxId(txId: String?) {
        prefs.edit().putString(KEY_PENDING_REFUND_TX_ID, txId).apply()
        Log.d(TAG_HISTORY, "savePendingRefundTxId: Stored pending refund TX ID: $txId")
    }

    /**
     * Retrieves the ID of the transaction that is currently undergoing a refund process.
     */
    fun getPendingRefundTxId(): String? {
        val txId = prefs.getString(KEY_PENDING_REFUND_TX_ID, null)
        Log.d(TAG_HISTORY, "getPendingRefundTxId: Retrieved pending refund TX ID: $txId")
        return txId
    }
}