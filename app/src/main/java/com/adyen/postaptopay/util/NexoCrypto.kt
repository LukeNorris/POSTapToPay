package com.adyen.postaptopay.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.*
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonReader
import javax.json.JsonString
import javax.json.JsonWriter
import kotlin.experimental.xor

class NexoCrypto(private val passphrase: CharArray) {

    companion object {
        const val NEXO_HMAC_KEY_LENGTH = 32
        const val NEXO_CIPHER_KEY_LENGTH = 32
        const val NEXO_IV_LENGTH = 16

        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun deriveKeyMaterial(passphrase: CharArray): ByteArray {
            val salt = "AdyenNexoV1Salt".toByteArray()
            val iterations = 4000
            val spec = PBEKeySpec(passphrase, salt, iterations, (NEXO_HMAC_KEY_LENGTH +
                    NEXO_CIPHER_KEY_LENGTH + NEXO_IV_LENGTH) * 8)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            return skf.generateSecret(spec).encoded
        }
    }

    private val derivedKeys: NexoDerivedKeys

    inner class NexoDerivedKeys(keyMaterial: ByteArray) {
        val hmac_key = keyMaterial.copyOfRange(0, NEXO_HMAC_KEY_LENGTH)
        val cipher_key = keyMaterial.copyOfRange(NEXO_HMAC_KEY_LENGTH, NEXO_HMAC_KEY_LENGTH + NEXO_CIPHER_KEY_LENGTH)
        val iv = keyMaterial.copyOfRange(NEXO_HMAC_KEY_LENGTH + NEXO_CIPHER_KEY_LENGTH, keyMaterial.size)
    }

    init {
        val keyMaterial = deriveKeyMaterial(passphrase)
        derivedKeys = NexoDerivedKeys(keyMaterial)
    }

    @Throws(
        NoSuchAlgorithmException::class, NoSuchPaddingException::class,
        IllegalBlockSizeException::class, BadPaddingException::class,
        InvalidKeyException::class, InvalidAlgorithmParameterException::class
    )
    private fun crypt(
        bytes: ByteArray, dk: NexoDerivedKeys, ivmod: ByteArray, mode: Int
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val s = SecretKeySpec(dk.cipher_key, "AES")
        val actualIV = ByteArray(NEXO_IV_LENGTH)
        for (i in actualIV.indices) {
            actualIV[i] = (dk.iv[i] xor ivmod[i]).toByte()
        }
        val i = IvParameterSpec(actualIV)
        cipher.init(mode, s, i)
        return cipher.doFinal(bytes)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    private fun hmac(bytes: ByteArray, dk: NexoDerivedKeys): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val s = SecretKeySpec(dk.hmac_key, "HmacSHA256")
        mac.init(s)
        return mac.doFinal(bytes)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(
        InvalidKeyException::class, NoSuchAlgorithmException::class,
        NoSuchPaddingException::class, IllegalBlockSizeException::class,
        BadPaddingException::class, InvalidAlgorithmParameterException::class
    )
    fun encrypt_and_hmac(input: ByteArray, keyIdentifier: String, keyVersion: Long): ByteArray {
        val encb64 = Base64.getEncoder()

        val jsonreader = Json.createReader(ByteArrayInputStream(input))
        val body = jsonreader.readObject()
        var request = true
        var saletopoirequest = body.getJsonObject("SaleToPOIRequest")
        if (saletopoirequest == null) {
            request = false
            saletopoirequest = body.getJsonObject("SaleToPOIResponse")
        }
        val messageheader = saletopoirequest!!.getJsonObject("MessageHeader")

        val ivmod = ByteArray(NEXO_IV_LENGTH)
        java.util.Random().nextBytes(ivmod)

        val encbytes = crypt(input, derivedKeys, ivmod, Cipher.ENCRYPT_MODE)
        val hmac = hmac(input, derivedKeys)

        val msg = Json.createObjectBuilder()
            .add("MessageHeader", messageheader)
            .add("NexoBlob", String(encb64.encode(encbytes)))
            .add(
                "SecurityTrailer", Json.createObjectBuilder()
                    .add("Hmac", String(encb64.encode(hmac)))
                    .add("KeyIdentifier", keyIdentifier)
                    .add("KeyVersion", keyVersion)
                    .add("AdyenCryptoVersion", 1)
                    .add("Nonce", String(encb64.encode(ivmod)))
            ).build()

        val total = Json.createObjectBuilder()
            .add(if (request) "SaleToPOIRequest" else "SaleToPOIResponse", msg)
            .build()

        return ByteArrayOutputStream().use { stream ->
            Json.createWriter(stream).use { writer ->
                writer.writeObject(total)
            }
            stream.toByteArray()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(
        InvalidKeyException::class, NoSuchAlgorithmException::class, IOException::class,
        NoSuchPaddingException::class, IllegalBlockSizeException::class,
        BadPaddingException::class, InvalidAlgorithmParameterException::class
    )
    fun decrypt_and_validate_hmac(input: ByteArray, keyIdentifier: String, keyVersion: Long): BytesAndOuterHeader {
        val b64dec = Base64.getDecoder()
        val stream = ByteArrayInputStream(input)
        val jsonreader = Json.createReader(stream)
        val total = jsonreader.readObject() ?: throw IOException("Faulty JSON")
        var saletopoirequest = total.getJsonObject("SaleToPOIRequest")
        if (saletopoirequest == null) {
            saletopoirequest = total.getJsonObject("SaleToPOIResponse")
        }
        if (saletopoirequest == null) {
            throw IOException("No SaleToPOIRequest or SaleToPOIResponse")
        }
        val messageheader = saletopoirequest.getJsonObject("MessageHeader")
            ?: throw IOException("MessageHeader not found")

        val payload = saletopoirequest.getJsonString("NexoBlob")
            ?: throw IOException("NexoBlob not found")
        val ciphertext = b64dec.decode(payload.string)

        val jsonTrailer = saletopoirequest.getJsonObject("SecurityTrailer")
            ?: throw IOException("SecurityTrailer not found")
        val version = jsonTrailer.getJsonNumber("AdyenCryptoVersion")
        if (version == null || version.intValue() != 1) {
            throw IOException("AdyenCryptoVersion version not found or not supported")
        }
        val nonce = jsonTrailer.getJsonString("Nonce")
            ?: throw IOException("Nonce not found")
        val keyId = jsonTrailer.getJsonString("KeyIdentifier")
            ?: throw IOException("KeyIdentifier not found")
        val kversion = jsonTrailer.getJsonNumber("KeyVersion")
            ?: throw IOException("KeyVersion not found")
        val b64 = jsonTrailer.getJsonString("Hmac")
            ?: throw IOException("Hmac not found")

        val dk = NexoDerivedKeys(deriveKeyMaterial(passphrase))

        val ivmod = b64dec.decode(nonce.string)
        val ret = crypt(ciphertext, dk, ivmod, Cipher.DECRYPT_MODE)

        val receivedmac = b64dec.decode(b64.string)
        val computedHmac = hmac(ret, dk)
        if (!MessageDigest.isEqual(receivedmac, computedHmac)) {
            throw IOException("Validation failed")
        }

        return BytesAndOuterHeader(ret, messageheader)
    }

    inner class BytesAndOuterHeader(val packet: ByteArray, val outer_header: JsonObject)

    fun validateInnerAndOuterHeader(inner: JsonObject?, outer: JsonObject?): Boolean {
        if (inner == null || outer == null) {
            return false
        }
        val fields = arrayOf(
            "DeviceID", "MessageCategory", "MessageClass", "MessageType",
            "SaleID", "ServiceID", "POIID", "ProtocolVersion"
        )
        for (field in fields) {
            val a = inner.getJsonString(field)
            val b = outer.getJsonString(field)
            if (a == null && b == null) continue
            if (a == null || a != b) return false
        }
        return true
    }
}
