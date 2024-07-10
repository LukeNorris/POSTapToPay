package com.adyen.postaptopay.model.payment

import kotlinx.serialization.Serializable

@Serializable
data class NexoRequest(
    val SaleToPOIRequest: SaleToPOIRequest
)
