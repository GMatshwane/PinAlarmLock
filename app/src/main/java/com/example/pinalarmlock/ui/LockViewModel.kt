package com.example.pinalarmlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pinalarmlock.alarm.AlarmPlayer
import com.example.pinalarmlock.data.PinRepository
import com.example.pinalarmlock.data.PinRepositoryLogic
import com.example.pinalarmlock.session.LockSession
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

    private var pendingSetupPin: String = ""

    fun bootstrap() {
        viewModelScope.launch {
            val dest =
                when {
                    !hasPin() -> Dest.SetupEnter
                    isGate || !isSessionUnlocked() -> Dest.Locked
                    else -> Dest.Unlocked
                }
            _uiState.update {
                it.copy(dest = dest, enteredPin = "", errorMessage = null)
            }
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
                    stopAlarm()
                    _uiState.update {
                        it.copy(
                            dest = Dest.Unlocked,
                            enteredPin = "",
                            errorMessage = null,
                            alarmActive = false,
                        )
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

    private fun isPinEntryDest(dest: Dest): Boolean = dest == Dest.SetupEnter || dest == Dest.SetupConfirm || dest == Dest.Locked

    companion object {
        const val WRONG_PIN = "Wrong PIN"
        const val PINS_DO_NOT_MATCH = "PINs do not match"
        const val PIN_LENGTH_ERROR = "PIN must be 4–6 digits"

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
