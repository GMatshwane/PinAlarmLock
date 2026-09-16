package com.example.pinalarmlock.ui.home

import com.example.pinalarmlock.lockwatch.LaunchableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun switchesDisabledUntilBothPermissions() = runTest {
        val vm = homeVm(usage = false, overlay = true)
        vm.refresh()
        assertFalse(vm.uiState.value.canEnrol)
        assertFalse(vm.uiState.value.usageGranted)
        assertTrue(vm.uiState.value.overlayGranted)
    }

    @Test
    fun toggleAddAndRemoveUpdatesRowsAndNotifies() = runTest {
        val enrolled = mutableSetOf<String>()
        var changes = 0
        val vm = homeVm(
            enrolled = enrolled,
            usage = true,
            overlay = true,
            onChanged = { changes++ },
        )
        vm.refresh()
        assertTrue(vm.uiState.value.canEnrol)
        assertFalse(vm.uiState.value.apps.single { it.packageName == "com.whatsapp" }.enrolled)
        vm.setEnrolled("com.whatsapp", true)
        assertTrue(enrolled.contains("com.whatsapp"))
        assertTrue(vm.uiState.value.apps.single { it.packageName == "com.whatsapp" }.enrolled)
        vm.setEnrolled("com.whatsapp", false)
        assertFalse(enrolled.contains("com.whatsapp"))
        assertEquals(2, changes)
    }

    private fun homeVm(
        enrolled: MutableSet<String> = mutableSetOf(),
        usage: Boolean,
        overlay: Boolean,
        onChanged: () -> Unit = {},
    ): HomeViewModel = HomeViewModel(
        listEnrolled = { enrolled.toSet() },
        add = { enrolled += it },
        remove = { enrolled -= it },
        loadApps = {
            listOf(
                LaunchableApp("com.whatsapp", "WhatsApp"),
                LaunchableApp("com.google.android.apps.photos", "Photos"),
            )
        },
        hasUsageAccess = { usage },
        hasOverlay = { overlay },
        onEnrolmentChanged = onChanged,
    )
}
