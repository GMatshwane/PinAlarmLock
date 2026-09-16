package com.example.pinalarmlock.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
class LockViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bootstrapGoesToSetupWhenNoPin() = runTest {
        val vm = viewModel(hasPin = false)
        vm.bootstrap()
        assertEquals(Dest.SetupEnter, vm.uiState.value.dest)
    }

    @Test
    fun bootstrapGoesToLockedWhenPinExists() = runTest {
        val vm = viewModel(hasPin = true)
        vm.bootstrap()
        assertEquals(Dest.Locked, vm.uiState.value.dest)
    }

    @Test
    fun bootstrapGoesToUnlockedWhenSessionAlreadyUnlocked() = runTest {
        val vm = viewModel(hasPin = true, isSessionUnlocked = { true })
        vm.bootstrap()
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
    }

    @Test
    fun setupConfirmMatchUnlocksSessionAndSavesPin() = runTest {
        val saved = mutableListOf<String>()
        var unlocked = false
        val vm = viewModel(
            hasPin = false,
            onSetPin = { saved += it },
            unlockSession = { unlocked = true },
        )
        vm.bootstrap()
        enter(vm, "2468")
        vm.onSubmit()
        assertEquals(Dest.SetupConfirm, vm.uiState.value.dest)
        enter(vm, "2468")
        vm.onSubmit()
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
        assertEquals(listOf("2468"), saved)
        assertTrue(unlocked)
    }

    @Test
    fun setupConfirmMismatchReturnsToEnterWithoutAlarm() = runTest {
        val alarms = mutableListOf<String>()
        val vm = viewModel(hasPin = false, alarms = alarms)
        vm.bootstrap()
        enter(vm, "1111")
        vm.onSubmit()
        enter(vm, "2222")
        vm.onSubmit()
        assertEquals(Dest.SetupEnter, vm.uiState.value.dest)
        assertEquals(LockViewModel.PINS_DO_NOT_MATCH, vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.shakeNonce > 0)
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun submitRejectsShortPin() = runTest {
        val vm = viewModel(hasPin = true)
        vm.bootstrap()
        enter(vm, "123")
        vm.onSubmit()
        assertEquals(Dest.Locked, vm.uiState.value.dest)
        assertEquals(LockViewModel.PIN_LENGTH_ERROR, vm.uiState.value.errorMessage)
        assertEquals("123", vm.uiState.value.enteredPin)
    }

    @Test
    fun backspaceRemovesLastDigit() = runTest {
        val vm = viewModel(hasPin = true)
        vm.bootstrap()
        enter(vm, "1234")
        vm.onBackspace()
        assertEquals("123", vm.uiState.value.enteredPin)
    }

    @Test
    fun sixthDigitAutoSubmits() = runTest {
        val vm = viewModel(hasPin = true, verify = { it == "123456" })
        vm.bootstrap()
        enter(vm, "123456")
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
        assertEquals("", vm.uiState.value.enteredPin)
    }

    @Test
    fun firstWrongPinStartsAlarmStaysLockedAndShakes() = runTest {
        val alarms = mutableListOf<String>()
        val vm = viewModel(hasPin = true, verify = { false }, alarms = alarms)
        vm.bootstrap()
        enter(vm, "0000")
        vm.onSubmit()
        assertEquals(Dest.Locked, vm.uiState.value.dest)
        assertEquals(LockViewModel.WRONG_PIN, vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.shakeNonce > 0)
        assertTrue(vm.uiState.value.alarmActive)
        assertEquals(listOf("start"), alarms)
        assertEquals("", vm.uiState.value.enteredPin)
    }

    @Test
    fun correctPinUnlocksAndStopsAlarm() = runTest {
        val alarms = mutableListOf<String>()
        val vm = viewModel(hasPin = true, verify = { it == "4242" }, alarms = alarms)
        vm.bootstrap()
        enter(vm, "0000")
        vm.onSubmit()
        enter(vm, "4242")
        vm.onSubmit()
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
        assertFalse(vm.uiState.value.alarmActive)
        assertEquals(listOf("start", "stop"), alarms)
    }

    @Test
    fun correctPinUnlocksSessionOnHome() = runTest {
        var unlocked = false
        val vm = viewModel(
            hasPin = true,
            verify = { true },
            unlockSession = { unlocked = true },
        )
        vm.bootstrap()
        enter(vm, "5555")
        vm.onSubmit()
        assertTrue(unlocked)
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
    }

    @Test
    fun gateCorrectPinUnlocksSessionAndFinishesWithoutEnrolment() = runTest {
        var unlocked = false
        var finished = false
        val vm = viewModel(
            hasPin = true,
            verify = { true },
            unlockSession = { unlocked = true },
            isGate = true,
            onGateUnlocked = { finished = true },
        )
        vm.bootstrap()
        enter(vm, "5555")
        vm.onSubmit()
        assertTrue(unlocked)
        assertTrue(finished)
        assertEquals(Dest.Locked, vm.uiState.value.dest)
    }

    @Test
    fun gateCorrectPinEmitsUnlockEvent() = runTest {
        val vm = viewModel(
            hasPin = true,
            verify = { true },
            isGate = true,
        )
        vm.bootstrap()
        val unlocked = async { vm.gateUnlockedEvents.first() }

        enter(vm, "5555")
        vm.onSubmit()

        unlocked.await()
    }

    @Test
    fun backgroundDoesNotRelockEnrolment() = runTest {
        val vm = viewModel(hasPin = true, verify = { true })
        vm.bootstrap()
        enter(vm, "5555")
        vm.onSubmit()
        vm.onAppBackgrounded()
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)
    }

    @Test
    fun bootstrapAfterScreenOffRelocksEnrolment() = runTest {
        var sessionUnlocked = false
        val vm = viewModel(
            hasPin = true,
            verify = { true },
            isSessionUnlocked = { sessionUnlocked },
            unlockSession = { sessionUnlocked = true },
        )
        vm.bootstrap()
        enter(vm, "5555")
        vm.onSubmit()
        assertEquals(Dest.Unlocked, vm.uiState.value.dest)

        vm.onAppBackgrounded()
        sessionUnlocked = false
        vm.bootstrap()

        assertEquals(Dest.Locked, vm.uiState.value.dest)
    }

    @Test
    fun backgroundDuringSetupDoesNotSkipToLocked() = runTest {
        val vm = viewModel(hasPin = false)
        vm.bootstrap()
        enter(vm, "12")
        vm.onAppBackgrounded()
        assertEquals(Dest.SetupEnter, vm.uiState.value.dest)
        assertEquals("", vm.uiState.value.enteredPin)
    }

    private fun enter(vm: LockViewModel, pin: String) {
        pin.forEach { vm.onDigit(it) }
    }

    private fun viewModel(
        hasPin: Boolean,
        verify: (String) -> Boolean = { false },
        onSetPin: (String) -> Unit = {},
        alarms: MutableList<String> = mutableListOf(),
        isSessionUnlocked: () -> Boolean = { false },
        unlockSession: () -> Unit = {},
        isGate: Boolean = false,
        onGateUnlocked: () -> Unit = {},
    ): LockViewModel = LockViewModel(
        hasPin = { hasPin },
        setPin = onSetPin,
        verifyPin = verify,
        startAlarm = { alarms += "start" },
        stopAlarm = { alarms += "stop" },
        isSessionUnlocked = isSessionUnlocked,
        unlockSession = unlockSession,
        isGate = isGate,
        onGateUnlocked = onGateUnlocked,
    )
}
