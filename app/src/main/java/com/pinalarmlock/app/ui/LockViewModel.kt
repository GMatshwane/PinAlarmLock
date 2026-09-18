package com.pinalarmlock.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinalarmlock.app.alarm.AlarmPlayer
import com.pinalarmlock.app.data.PinRepository
import com.pinalarmlock.app.data.PinRepositoryLogic
import com.pinalarmlock.app.session.LockSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LockViewModel(
    private val hasPin: suspend () -> Boolean,
    private val setPin: suspend (String) -> Unit,
    private val verifyPin: suspend (String) -> Boolean,
    private val startAlarm: () -> Unit,
    private val stopAlarm: () -> Unit,
    private val isSessionUnlocked: () -> Boolean = { false },
    private val unlockSession: () -> Unit = {},
    private val isGate: Boolean = false,
    private val onGateUnlocked: () -> Unit = {},
) : ViewModel() {
    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _gateUnlockedEvents = MutableSharedFlow<Unit>(replay = 1)
    val gateUnlockedEvents: SharedFlow<Unit> = _gateUnlockedEvents.asSharedFlow()

    private val _confirmDeviceLockEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val confirmDeviceLockEvents: SharedFlow<Unit> = _confirmDeviceLockEvents.asSharedFlow()

    private var pendingSetupPin: String = ""
    private var replacingPin: Boolean = false

    fun bootstrap() {
        viewModelScope.launch {
            replacingPin = false
            val dest =
                when {
                    !hasPin() -> Dest.SetupEnter
                    isGate || !isSessionUnlocked() -> Dest.Locked
                    else -> Dest.Unlocked
                }
            _uiState.update {
                it.copy(
                    dest = dest,
                    enteredPin = "",
                    errorMessage = null,
                    canCancelReset = false,
                )
            }
        }
    }

    fun startChangePin() {
        if (_uiState.value.dest != Dest.Unlocked) return
        replacingPin = true
        pendingSetupPin = ""
        _uiState.update {
            it.copy(
                dest = Dest.ChangeCurrent,
                enteredPin = "",
                errorMessage = null,
                canCancelReset = true,
            )
        }
    }

    fun onForgotPin() {
        if (_uiState.value.dest != Dest.Locked) return
        _confirmDeviceLockEvents.tryEmit(Unit)
    }

    fun onDeviceLockConfirmed() {
        replacingPin = true
        pendingSetupPin = ""
        unlockSession()
        stopAlarm()
        _uiState.update {
            it.copy(
                dest = Dest.SetupEnter,
                enteredPin = "",
                errorMessage = null,
                alarmActive = false,
                canCancelReset = true,
            )
        }
    }

    fun onDeviceLockUnavailable() {
        if (_uiState.value.dest != Dest.Locked) return
        _uiState.update {
            it.copy(
                errorMessage = DEVICE_LOCK_REQUIRED,
                shakeNonce = it.shakeNonce + 1,
            )
        }
    }

    fun onCancelReset() {
        if (!_uiState.value.canCancelReset) return
        pendingSetupPin = ""
        replacingPin = false
        stopAlarm()
        val dest =
            when {
                isGate -> Dest.Locked
                isSessionUnlocked() -> Dest.Unlocked
                else -> Dest.Locked
            }
        _uiState.update {
            it.copy(
                dest = dest,
                enteredPin = "",
                errorMessage = null,
                alarmActive = false,
                canCancelReset = false,
            )
        }
    }

    fun onDigit(digit: Char) {
        if (digit !in '0'..'9') return
        if (!isPinEntryDest(_uiState.value.dest)) return
        val current = _uiState.value.enteredPin
        if (current.length >= 6) return
        val next = current + digit
        _uiState.update { it.copy(enteredPin = next, errorMessage = null) }
        if (next.length == 6) {
            onSubmit()
        }
    }

    fun onBackspace() {
        if (!isPinEntryDest(_uiState.value.dest)) return
        _uiState.update {
            it.copy(enteredPin = it.enteredPin.dropLast(1), errorMessage = null)
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!isPinEntryDest(state.dest)) return
        val pin = state.enteredPin
        if (!PinRepositoryLogic.isValidPin(pin)) {
            _uiState.update {
                it.copy(
                    errorMessage = PIN_LENGTH_ERROR,
                    shakeNonce = it.shakeNonce + 1,
                )
            }
            return
        }
        when (state.dest) {
            Dest.SetupEnter -> {
                pendingSetupPin = pin
                _uiState.update {
                    it.copy(dest = Dest.SetupConfirm, enteredPin = "", errorMessage = null)
                }
            }
            Dest.SetupConfirm -> {
                if (pin != pendingSetupPin) {
                    pendingSetupPin = ""
                    _uiState.update {
                        it.copy(
                            dest = Dest.SetupEnter,
                            enteredPin = "",
                            errorMessage = PINS_DO_NOT_MATCH,
                            shakeNonce = it.shakeNonce + 1,
                        )
                    }
                    return
                }
                viewModelScope.launch {
                    setPin(pin)
                    unlockSession()
                    pendingSetupPin = ""
                    replacingPin = false
                    stopAlarm()
                    if (isGate) {
                        _gateUnlockedEvents.tryEmit(Unit)
                        onGateUnlocked()
                        _uiState.update {
                            it.copy(
                                dest = Dest.Locked,
                                enteredPin = "",
                                errorMessage = null,
                                alarmActive = false,
                                canCancelReset = false,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                dest = Dest.Unlocked,
                                enteredPin = "",
                                errorMessage = null,
                                alarmActive = false,
                                canCancelReset = false,
                            )
                        }
                    }
                }
            }
            Dest.ChangeCurrent -> {
                viewModelScope.launch {
                    if (verifyPin(pin)) {
                        stopAlarm()
                        _uiState.update {
                            it.copy(
                                dest = Dest.SetupEnter,
                                enteredPin = "",
                                errorMessage = null,
                                alarmActive = false,
                            )
                        }
                    } else {
                        startAlarm()
                        _uiState.update {
                            it.copy(
                                dest = Dest.ChangeCurrent,
                                enteredPin = "",
                                errorMessage = WRONG_PIN,
                                shakeNonce = it.shakeNonce + 1,
                                alarmActive = true,
                            )
                        }
                    }
                }
            }
            Dest.Locked -> {
                viewModelScope.launch {
                    if (verifyPin(pin)) {
                        stopAlarm()
                        unlockSession()
                        if (isGate) {
                            _gateUnlockedEvents.tryEmit(Unit)
                            onGateUnlocked()
                            _uiState.update {
                                it.copy(
                                    dest = Dest.Locked,
                                    enteredPin = "",
                                    errorMessage = null,
                                    alarmActive = false,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    dest = Dest.Unlocked,
                                    enteredPin = "",
                                    errorMessage = null,
                                    alarmActive = false,
                                )
                            }
                        }
                    } else {
                        startAlarm()
                        _uiState.update {
                            it.copy(
                                dest = Dest.Locked,
                                enteredPin = "",
                                errorMessage = WRONG_PIN,
                                shakeNonce = it.shakeNonce + 1,
                                alarmActive = true,
                            )
                        }
                    }
                }
            }
            Dest.Loading, Dest.Unlocked -> Unit
        }
    }

    fun onAppBackgrounded() {
        stopAlarm()
        _uiState.update {
            it.copy(
                enteredPin = "",
                errorMessage = null,
                alarmActive = false,
            )
        }
    }

    override fun onCleared() {
        stopAlarm()
        super.onCleared()
    }

    private fun isPinEntryDest(dest: Dest): Boolean =
        dest == Dest.SetupEnter ||
            dest == Dest.SetupConfirm ||
            dest == Dest.Locked ||
            dest == Dest.ChangeCurrent

    companion object {
        const val WRONG_PIN = "Wrong PIN"
        const val PINS_DO_NOT_MATCH = "PINs do not match"
        const val PIN_LENGTH_ERROR = "PIN must be 4–6 digits"
        const val DEVICE_LOCK_REQUIRED = "Set a screen lock in Android Settings to reset a forgotten PIN."

        fun factory(
            pinRepository: PinRepository,
            alarmPlayer: AlarmPlayer,
            session: LockSession,
            isGate: Boolean,
            onGateUnlocked: () -> Unit,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(LockViewModel::class.java)) {
                        "Unknown ViewModel class $modelClass"
                    }
                    return LockViewModel(
                        hasPin = pinRepository::hasPin,
                        setPin = pinRepository::setPin,
                        verifyPin = pinRepository::verifyPin,
                        startAlarm = alarmPlayer::start,
                        stopAlarm = alarmPlayer::stop,
                        isSessionUnlocked = { session.isUnlocked },
                        unlockSession = { session.unlock() },
                        isGate = isGate,
                        onGateUnlocked = onGateUnlocked,
                    ) as T
                }
            }
    }
}
