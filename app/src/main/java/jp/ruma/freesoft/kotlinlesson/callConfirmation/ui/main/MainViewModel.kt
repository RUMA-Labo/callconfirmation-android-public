/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.main

import androidx.lifecycle.ViewModel
import jp.ruma.freesoft.kotlinlesson.callConfirmation.enum.LockMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * メイン画面の Jetpack Compose ViewModel
 * AI Agentによる自動コーディング
 */
data class MainUiState(
    val lockMode: LockMode = LockMode.CALL_DIRECT,
    val keepAliveEnabled: Boolean = false,
    val outsideDismissEnabled: Boolean = false,
    val isServiceEnabled: Boolean = true,
    val allGranted: Boolean = false,
    val hasCallPermission: Boolean = false,
    val isCallRedirection: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasFullScreenIntentPermission: Boolean = false,
    val isLoading: Boolean = false,
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun updateLockMode(mode: LockMode) {
        _uiState.update { it.copy(lockMode = mode) }
    }

    fun updateKeepAlive(enabled: Boolean) {
        _uiState.update { it.copy(keepAliveEnabled = enabled) }
    }

    fun updateOutsideDismiss(enabled: Boolean) {
        _uiState.update { it.copy(outsideDismissEnabled = enabled) }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun updatePermissionsAndServiceState(
        allGranted: Boolean,
        hasCall: Boolean,
        isRedirection: Boolean,
        hasOverlay: Boolean,
        hasNotification: Boolean,
        hasFullScreen: Boolean,
        isServiceEnabled: Boolean,
    ) {
        _uiState.update {
            it.copy(
                allGranted = allGranted,
                hasCallPermission = hasCall,
                isCallRedirection = isRedirection,
                hasOverlayPermission = hasOverlay,
                hasNotificationPermission = hasNotification,
                hasFullScreenIntentPermission = hasFullScreen,
                isServiceEnabled = isServiceEnabled,
            )
        }
    }
}
