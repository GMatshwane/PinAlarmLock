package com.example.pinalarmlock.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class PinHasherTest {
    @Test
    fun generateSaltIsSixteenBytes() {
        assertEquals(16, PinHasher.generateSalt().size)
        assertEquals(PinHasher.SALT_LENGTH_BYTES, PinHasher.generateSalt().size)
    }

    @Test
    fun generateSaltIsNotDeterministic() {
        val first = PinHasher.generateSalt()
        val second = PinHasher.generateSalt()
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun hashPinIsDeterministicForSameSalt() {
        val salt = ByteArray(16) { index -> index.toByte() }
        val first = PinHasher.hashPin("24680", salt)
        val second = PinHasher.hashPin("24680", salt)
        assertEquals(first, second)
    }

    @Test
    fun hashPinChangesWithSaltOrPin() {
        val saltA = ByteArray(16) { 1 }
        val saltB = ByteArray(16) { 2 }
        assertNotEquals(PinHasher.hashPin("1234", saltA), PinHasher.hashPin("1234", saltB))
        assertNotEquals(PinHasher.hashPin("1234", saltA), PinHasher.hashPin("1235", saltA))
    }

    @Test
    fun hashUsesUrlBase64WithoutPadding() {
        val salt = ByteArray(16) { 0xFF.toByte() }
        val hash = PinHasher.hashPin("1234", salt)
        assertFalse(hash.contains("="))
        assertFalse(hash.contains("+"))
        assertFalse(hash.contains("/"))
        assertTrue(hash.matches(Regex("^[A-Za-z0-9_-]+$")))
        val encodedSalt = PinHasher.encode(salt)
        assertEquals(
            Base64.getUrlEncoder().withoutPadding().encodeToString(salt),
            encodedSalt,
        )
        assertFalse(encodedSalt.contains("="))
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val bytes = byteArrayOf(0, 1, 2, 127, 0xFF.toByte())
        assertArrayEquals(bytes, PinHasher.decode(PinHasher.encode(bytes)))
    }

    @Test
    fun verifyAcceptsCorrectPinAndRejectsWrongPin() {
        val salt = PinHasher.generateSalt(SecureRandom(byteArrayOf(9, 8, 7)))
        val hash = PinHasher.hashPin("13579", salt)
        val saltB64 = PinHasher.encode(salt)
        assertTrue(PinHasher.verify("13579", saltB64, hash))
        assertFalse(PinHasher.verify("13578", saltB64, hash))
        assertFalse(PinHasher.verify("1357", saltB64, hash))
    }

    @Test
    fun verifyUsesConstantTimeComparison() {
        val salt = ByteArray(16) { 3 }
        val expected = PinHasher.digestPin("9999", salt)
        val actual = PinHasher.digestPin("9999", salt)
        assertTrue(MessageDigest.isEqual(expected, actual))
        assertFalse(MessageDigest.isEqual(expected, PinHasher.digestPin("0000", salt)))
        assertTrue(PinHasher.verify("9999", PinHasher.encode(salt), PinHasher.encode(expected)))
    }

    @Test
    fun verifyReturnsFalseForCorruptPayloads() {
        assertFalse(PinHasher.verify("1234", "@@@", "not-base64"))
        assertFalse(PinHasher.verify("1234", PinHasher.encode(ByteArray(16)), "%%%"))
    }
}
