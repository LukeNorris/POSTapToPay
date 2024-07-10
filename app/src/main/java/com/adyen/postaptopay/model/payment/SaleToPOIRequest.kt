package com.adyen.postaptopay.model.payment

import kotlinx.serialization.Serializable

@Serializable
data class SaleToPOIRequest(
    val MessageHeader: MessageHeader,
    val NexoBlob: String,
    val SecurityTrailer: SecurityTrailer
)
