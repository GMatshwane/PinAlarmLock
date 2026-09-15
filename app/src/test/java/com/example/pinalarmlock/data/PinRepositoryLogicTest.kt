package com.example.pinalarmlock.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class PinRepositoryLogicTest {
    @Test
    fun hasPinIsFalseUntilSet() = runTest {
        val store = InMemoryPinStore()
        val logic = store.logic()
        assertFalse(logic.hasPin())
        assertFalse(logic.verifyPin("1234"))
    }

    @Test
    fun setPinPersistsSaltAndHashNeverPlaintext() = runTest {
        val store = InMemoryPinStore()
        val salt = ByteArray(16) { 7 }
        val logic = store.logic(generateSalt = { salt })
        logic.setPin("2468")
        assertTrue(logic.hasPin())
        assertEquals(PinHasher.encode(salt), store.salt)
        assertEquals(PinHasher.hashPin("2468", salt), store.hash)
        assertFalse(store.hash == "2468")
        assertFalse(store.salt == "2468")
        assertFalse(store.hash!!.contains("2468"))
    }

    @Test
    fun verifyPinMatchesOnlyTheStoredPin() = runTest {
        val store = InMemoryPinStore()
        val logic = store.logic()
        logic.setPin("135790")
        assertTrue(logic.verifyPin("135790"))
        assertFalse(logic.verifyPin("135791"))
        assertFalse(logic.verifyPin("1357"))
        assertFalse(logic.verifyPin("abcdefgh"))
    }

    @Test
    fun setPinRejectsInvalidLengthsAndNonDigits() = runTest {
        val logic = InMemoryPinStore().logic()
        assertFailsWith<IllegalArgumentException> { logic.setPin("123") }
        assertFailsWith<IllegalArgumentException> { logic.setPin("1234567") }
        assertFailsWith<IllegalArgumentException> { logic.setPin("12ab") }
        assertFailsWith<IllegalArgumentException> { logic.setPin("") }
        assertFalse(logic.hasPin())
    }

    @Test
    fun setPinOverwritesPreviousCredentials() = runTest {
        val store = InMemoryPinStore()
        val logic = store.logic()
        logic.setPin("1111")
        val oldSalt = store.salt
        val oldHash = store.hash
        logic.setPin("22222")
        assertNotEquals(oldSalt, store.salt)
        assertNotEquals(oldHash, store.hash)
        assertTrue(logic.verifyPin("22222"))
        assertFalse(logic.verifyPin("1111"))
    }

    @Test
    fun isValidPinEnforcesFourToSixDigits() {
        assertTrue(PinRepositoryLogic.isValidPin("0000"))
        assertTrue(PinRepositoryLogic.isValidPin("12345"))
        assertTrue(PinRepositoryLogic.isValidPin("987654"))
        assertFalse(PinRepositoryLogic.isValidPin("123"))
        assertFalse(PinRepositoryLogic.isValidPin("1234567"))
        assertFalse(PinRepositoryLogic.isValidPin("12a4"))
    }

    private class InMemoryPinStore {
        var salt: String? = null
        var hash: String? = null

        fun logic(
            generateSalt: () -> ByteArray = { PinHasher.generateSalt() },
        ): PinRepositoryLogic = PinRepositoryLogic(
            readSaltAndHash = { salt to hash },
            writeSaltAndHash = { newSalt, newHash ->
                salt = newSalt
                hash = newHash
            },
            generateSalt = generateSalt,
        )
    }
}
