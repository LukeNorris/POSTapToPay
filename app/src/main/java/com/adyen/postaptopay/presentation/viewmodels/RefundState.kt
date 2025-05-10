package com.adyen.postaptopay.presentation.viewmodels

data class RefundState(
    val isLoading: Boolean = false,
    val error: String? = null
)