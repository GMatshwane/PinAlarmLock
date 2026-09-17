package com.example.pinalarmlock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pinalarmlock.data.ProtectedAppsRepository
import com.example.pinalarmlock.lockwatch.LaunchableApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppRow(
    val packageName: String,
    val label: String,
    val enrolled: Boolean,
)

data class HomeUiState(
    val usageGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val apps: List<AppRow> = emptyList(),
) {
    val canEnrol: Boolean get() = usageGranted && overlayGranted
}

class HomeViewModel(
    private val listEnrolled: suspend () -> Set<String>,
    private val add: suspend (String) -> Unit,
    private val remove: suspend (String) -> Unit,
    private val loadApps: () -> List<LaunchableApp>,
    private val hasUsageAccess: () -> Boolean,
    private val hasOverlay: () -> Boolean,
    private val onEnrolmentChanged: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val enrolled = listEnrolled()
            _uiState.value =
                withContext(ioDispatcher) {
                    HomeUiState(
                        usageGranted = hasUsageAccess(),
                        overlayGranted = hasOverlay(),
                        apps =
                            loadApps().map { app ->
                                AppRow(app.packageName, app.label, app.packageName in enrolled)
                            },
                    )
                }
        }
    }

    fun setEnrolled(
        packageName: String,
        enrolled: Boolean,
    ) {
        if (!_uiState.value.canEnrol) return
        viewModelScope.launch {
            if (enrolled) add(packageName) else remove(packageName)
            onEnrolmentChanged()
            refresh()
        }
    }

    companion object {
        fun factory(
            protectedApps: ProtectedAppsRepository,
            loadApps: () -> List<LaunchableApp>,
            hasUsageAccess: () -> Boolean,
            hasOverlay: () -> Boolean,
            onEnrolmentChanged: () -> Unit,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                    return HomeViewModel(
                        listEnrolled = protectedApps::list,
                        add = protectedApps::add,
                        remove = protectedApps::remove,
                        loadApps = loadApps,
                        hasUsageAccess = hasUsageAccess,
                        hasOverlay = hasOverlay,
                        onEnrolmentChanged = onEnrolmentChanged,
                    ) as T
                }
            }
    }
}
