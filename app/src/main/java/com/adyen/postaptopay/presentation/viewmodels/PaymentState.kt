package com.adyen.postaptopay.presentation.viewmodels


data class PaymentState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val price: String = "",
        val isPaymentSuccessful: Boolean? = null
)

