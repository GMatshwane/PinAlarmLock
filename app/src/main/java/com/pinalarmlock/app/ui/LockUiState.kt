package com.pinalarmlock.app.ui

sealed interface Dest {
    data object Loading : Dest

    data object SetupEnter : Dest

    data object SetupConfirm : Dest

    data object Locked : Dest

    data object ChangeCurrent : Dest

    data object Unlocked : Dest
}

data class LockUiState(
    val dest: Dest = Dest.Loading,
    val enteredPin: String = "",
    val errorMessage: String? = null,
    val shakeNonce: Int = 0,
    val alarmActive: Boolean = false,
    val canCancelReset: Boolean = false,
)
