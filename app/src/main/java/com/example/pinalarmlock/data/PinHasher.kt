package com.example.pinalarmlock.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PinHasher {
    const val SALT_LENGTH_BYTES = 16

    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()

    fun generateSalt(random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

    fun decode(value: String): ByteArray = decoder.decode(value)

    fun digestPin(
        pin: String,
        salt: ByteArray,
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    fun hashPin(
        pin: String,
        salt: ByteArray,
    ): String = encode(digestPin(pin, salt))

    fun verify(
        pin: String,
        saltBase64: String,
        hashBase64: String,
    ): Boolean {
        return try {
            val expected = decode(hashBase64)
            val actual = digestPin(pin, decode(saltBase64))
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        }
    }
}
