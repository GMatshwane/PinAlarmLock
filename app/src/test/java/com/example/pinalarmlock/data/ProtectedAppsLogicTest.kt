package com.example.pinalarmlock.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedAppsLogicTest {
    @Test
    fun startsEmpty() = runTest {
        val logic = InMemoryProtected().logic()
        assertEquals(emptySet<String>(), logic.list())
        assertFalse(logic.contains("com.whatsapp"))
    }

    @Test
    fun addAndRemove() = runTest {
        val logic = InMemoryProtected().logic()
        logic.add("com.whatsapp")
        logic.add("com.google.android.apps.photos")
        logic.add("com.whatsapp")
        assertTrue(logic.contains("com.whatsapp"))
        assertEquals(
            setOf("com.whatsapp", "com.google.android.apps.photos"),
            logic.list(),
        )
        logic.remove("com.whatsapp")
        assertFalse(logic.contains("com.whatsapp"))
        assertEquals(setOf("com.google.android.apps.photos"), logic.list())
    }

    private class InMemoryProtected {
        var packages: Set<String> = emptySet()

        fun logic(): ProtectedAppsLogic = ProtectedAppsLogic(
            readPackages = { packages },
            writePackages = { packages = it },
        )
    }
}
