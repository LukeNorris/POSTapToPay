package com.adyen.postaptopay.util
/*
 *                       ######
 *                       ######
 * ############    ####( ######  #####. ######  ############   ############
 * #############  #####( ######  #####. ######  #############  #############
 *        ######  #####( ######  #####. ######  #####  ######  #####  ######
 * ###### ######  #####( ######  #####. ######  #####  #####   #####  ######
 * ###### ######  #####( ######  #####. ######  #####          #####  ######
 * #############  #############  #############  #############  #####  ######
 *  ############   ############  #############   ############  #####  ######
 *                                      ######
 *                               #############
 *                               ############
 *
 * Adyen Kotlin API Library
 *
 * Copyright (c) 2019 Adyen B.V.
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more info.
 */

import com.adyen.model.terminal.TerminalAPIRequest
import com.adyen.model.terminal.TerminalAPIResponse
import com.adyen.model.terminal.TerminalAPISecuredRequest
import com.adyen.model.terminal.TerminalAPISecuredResponse
import com.adyen.model.terminal.security.SaleToPOISecuredMessage
import com.adyen.model.terminal.security.SecurityKey
import com.adyen.terminal.security.NexoCrypto
import com.adyen.terminal.security.exception.NexoCryptoException
import com.adyen.terminal.serialization.TerminalAPIGsonBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TerminalCryptoHandler(securityKey: SecurityKey) {

   /* private val nexoCrypto: NexoCrypto = NexoCrypto(securityKey)
    private val terminalApiGson: Gson = TerminalAPIGsonBuilder.create()

    /**
     * Encrypt JSON request
     *
     * @param terminalAPIRequest TerminalAPIRequest
     * @return Encrypted SaleToPOISecuredMessage as JSON string
     * @throws NexoCryptoException exception
     */
    @Throws(NexoCryptoException::class)
    fun encryptRequest(terminalAPIRequest: TerminalAPIRequest): String {
        val jsonRequest = terminalApiGson.toJson(terminalAPIRequest)
        val saleToPOISecuredRequest = nexoCrypto.encrypt(jsonRequest, terminalAPIRequest.saleToPOIRequest.messageHeader)

        val securedPaymentRequest = TerminalAPISecuredRequest().apply {
            saleToPOIRequest = saleToPOISecuredRequest
        }
        return terminalApiGson.toJson(securedPaymentRequest)
    }

    /**
     * Decrypt JSON response
     *
     * @param jsonResponse Encrypted JSON response string
     * @return Decrypted TerminalAPIResponse
     * @throws NexoCryptoException exception
     */
    @Throws(NexoCryptoException::class)
    fun decryptResponse(jsonResponse: String): TerminalAPIResponse {
        val securedPaymentResponse: TerminalAPISecuredResponse = terminalApiGson.fromJson(jsonResponse, object : TypeToken<TerminalAPISecuredResponse>() {}.type)
        val saleToPOISecuredResponse = securedPaymentResponse.saleToPOIResponse
        val jsonDecryptedResponse = nexoCrypto.decrypt(saleToPOISecuredResponse)
        return terminalApiGson.fromJson(jsonDecryptedResponse, object : TypeToken<TerminalAPIResponse>() {}.type)
    }*/
}
