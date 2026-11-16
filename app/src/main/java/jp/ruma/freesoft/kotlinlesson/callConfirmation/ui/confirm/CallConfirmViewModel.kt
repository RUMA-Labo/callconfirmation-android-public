/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm

import android.annotation.SuppressLint
import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * CallConfirmActivityのUI状態
 * AI Agentによる自動コーディング
 */
data class CallConfirmUiState(
    val currentUri: Uri = Uri.EMPTY,
    val orgUri: Uri = Uri.EMPTY,
    val newUri: Uri = Uri.EMPTY,
    val phoneAccountHandle: PhoneAccountHandle? = null,
    val remainingTimeMillis: Long = 0,
    val isTimerRunning: Boolean = false,
    val isTimerFinished: Boolean = false,
    val isTimerPaused: Boolean = false,
) {
    // 画面表示用の番号文字列 (schemeSpecificPart)
    val displayPhoneNumber: String get() = currentUri.schemeSpecificPart ?: ""
    val displayOrgNumber: String get() = orgUri.schemeSpecificPart ?: ""
    val displayNewNumber: String get() = newUri.schemeSpecificPart ?: ""
}

class CallConfirmViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        CallConfirmUiState(
            orgUri = Uri.EMPTY,
            newUri = Uri.EMPTY,
            currentUri = Uri.EMPTY,
        ),
    )
    val uiState: StateFlow<CallConfirmUiState> = _uiState.asStateFlow()

    // タイマー完了イベント（SharedFlowで通知）
    private val _onTimerFinished = MutableSharedFlow<Unit>()
    val onTimerFinished: SharedFlow<Unit> = _onTimerFinished.asSharedFlow()

    private var _timerJob: Job? = null

    fun setCallParameters(orgUri: Uri, newUri: Uri, phoneAccountHandle: PhoneAccountHandle? = null) {
        _uiState.update {
            it.copy(
                orgUri = orgUri,
                newUri = newUri,
                currentUri = newUri,
                phoneAccountHandle = phoneAccountHandle,
            )
        }
    }

    fun togglePhoneNumber(isNewSelected: Boolean) {
        _uiState.update {
            it.copy(currentUri = if (isNewSelected) it.newUri else it.orgUri)
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isTimerPaused = true) }
    }

    fun resumeTimer() {
        _uiState.update { it.copy(isTimerPaused = false) }
    }

    fun startTimer(
        durationMillis: Long,
        intervalMillis: Long,
    ) {
        // 既に家運ダウンが動いていたら止める（発信確認中に、新たな発信確認が発生した場合）
        stopTimer()

        _uiState.update {
            it.copy(
                isTimerRunning = true,
                remainingTimeMillis = durationMillis,
                isTimerFinished = false,
                isTimerPaused = false,
            )
        }

        _timerJob = viewModelScope.launch {
            var remaining = durationMillis

            while (remaining > 0) {
                if (!_uiState.value.isTimerPaused) {
                    _uiState.update { it.copy(remainingTimeMillis = remaining) }
                    delay(timeMillis = intervalMillis)
                    remaining = max(0, remaining - intervalMillis)
                } else {
                    // 一時停止中はカウントを減らさずに待機
                    delay(timeMillis = intervalMillis)
                }
            }

            _uiState.update {
                it.copy(
                    remainingTimeMillis = 0L,
                    isTimerRunning = false,
                    isTimerFinished = true,
                )
            }
            _onTimerFinished.emit(Unit)
        }
    }

    fun stopTimer() {
        _timerJob?.cancel()
        _timerJob = null
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
