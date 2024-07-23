package com.adyen.postaptopay.model.payment

import kotlinx.serialization.Serializable

@Serializable
data class NexoRequestModel(
    val nexoBlob: String? = null,
    val securityTrailer: String? = null,
    val saleToPOIRequest: SaleToPOIRequest
)