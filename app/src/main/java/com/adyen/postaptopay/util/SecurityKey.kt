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


import com.google.gson.annotations.SerializedName

data class SecurityKey(
    @SerializedName("passphrase") var passphrase: String? = null,
    @SerializedName("keyIdentifier") var keyIdentifier: String? = null,
    @SerializedName("keyVersion") var keyVersion: Int? = null,
    @SerializedName("adyenCryptoVersion") var adyenCryptoVersion: Int? = null
) {
    override fun toString(): String {
        return "SecurityKey(passphrase='$passphrase', keyIdentifier='$keyIdentifier', keyVersion=$keyVersion, adyenCryptoVersion=$adyenCryptoVersion)"
    }
}

