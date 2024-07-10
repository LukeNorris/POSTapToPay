package com.adyen.postaptopay.model.payment

import kotlinx.serialization.Serializable

@Serializable
data class SecurityTrailer(
    val KeyVersion: String,
    val KeyIdentifier: String,
    val Hmac: String,
    val Nonce: String,
    val AdyenCryptoVersion: String
)
