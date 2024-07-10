package com.adyen.postaptopay.model.payment

import kotlinx.serialization.Serializable

@Serializable
data class MessageHeader(
    val MessageClass: String,
    val ProtocolVersion: String,
    val ServiceID: String,
    val MessageCategory: String,
    val SaleID: String,
    val MessageType: String,
    val POIID: String,
    val Timestamp: String? = null,
    val SaleTransactionID: String? = null
)
