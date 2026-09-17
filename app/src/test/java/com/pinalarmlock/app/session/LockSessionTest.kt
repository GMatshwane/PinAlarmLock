package com.pinalarmlock.app.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockSessionTest {
    private val own = "com.pinalarmlock.app"
    private val whatsapp = "com.whatsapp"
    private val photos = "com.google.android.apps.photos"

    @Test
    fun startsLocked() {
        val session = LockSession(own)
        assertFalse(session.isUnlocked)
    }

    @Test
    fun shouldGateWhenLockedAndEnrolled() {
        val session = LockSession(own)
        assertTrue(session.shouldGate(whatsapp, setOf(whatsapp)))
    }

    @Test
    fun shouldNotGateWhenUnlocked() {
        val session = LockSession(own)
        session.unlock()
        assertTrue(session.isUnlocked)
        assertFalse(session.shouldGate(whatsapp, setOf(whatsapp)))
    }

    @Test
    fun shouldNotGateOwnPackage() {
        val session = LockSession(own)
        assertFalse(session.shouldGate(own, setOf(own, whatsapp)))
    }

    @Test
    fun shouldNotGateUnenrolledPackage() {
        val session = LockSession(own)
        assertFalse(session.shouldGate(photos, setOf(whatsapp)))
    }

    @Test
    fun lockAfterScreenOffGatesAgain() {
        val session = LockSession(own)
        session.unlock()
        session.lock()
        assertFalse(session.isUnlocked)
        assertTrue(session.shouldGate(whatsapp, setOf(whatsapp, photos)))
    }
}
