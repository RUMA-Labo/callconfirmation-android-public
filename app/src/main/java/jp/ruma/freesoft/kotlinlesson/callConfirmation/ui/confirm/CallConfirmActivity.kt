/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.service.CallConfirmForegroundService
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme.CallConfirmationTheme
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.CallStateHolder
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.NotificationUtil
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.getAppPreference
import kotlinx.coroutines.launch

/**
 * CallConfirmActivity
 * AI Agentによる、ほぼ自動コーディング.
 */
class CallConfirmActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ORG_URI = "EXTRA_ORG_URI"
        const val EXTRA_NEW_URI = "EXTRA_NEW_URI"
        const val EXTRA_PHONE_ACCT = "EXTRA_PHONE_ACCT"
        const val EXTRA_ARG_INTENT = "EXTRA_ARG_INENT"
    }

    private val viewModel: CallConfirmViewModel by viewModels()

    // ---------------------------------------------------------
    // --- コールバックソッド--------------------------------------
    // ---------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        // 起動アニメーションを即座に無効化してシステムの Dialer との競合（チラつき）を抑える
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)
        configureLockScreenFlags()

        if (savedInstanceState == null) {
            // 初回起動時のみIntentからパラメータを取得してViewModelへ渡す
            val orgUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_ORG_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_ORG_URI)
            }
            val newUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_NEW_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_NEW_URI)
            }
            val phoneAccountHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_PHONE_ACCT, PhoneAccountHandle::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_PHONE_ACCT)
            }
            viewModel.setCallParameters(orgUri ?: Uri.EMPTY, newUri ?: Uri.EMPTY, phoneAccountHandle)
            // タイマーを開始
            viewModel.startTimer(
                Const.CONFIRM_TIMEOUT_MILLIS,
                Const.CONFIRM_INTERVAL_MILLIS,
            )
        }

        setContent {
            CallConfirmationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(R.color.call_confirm_bg_cover),
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    CallConfirmScreen(
                        uiState = uiState,
                        isActivity = true,
                        isOutsideDoubleTapDismiss = getAppPreference().isOutsideDismissSync(),
                        onPhoneNumberSelected = { isNewSelected ->
                            viewModel.togglePhoneNumber(isNewSelected)
                        },
                        onConfirm = {
                            handleConfirm(
                                uiState.currentUri,
                                uiState.phoneAccountHandle,
                            )
                        },
                        onCancel = { handleCancel() },
                        onTimerPause = { viewModel.pauseTimer() },
                        onTimerResume = { viewModel.resumeTimer() },
                    )
                }
            }
        }
        setupViewModelObservers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val orgUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ORG_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ORG_URI)
        } ?: Uri.EMPTY

        val newUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_NEW_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_NEW_URI)
        } ?: Uri.EMPTY

        val phoneAccountHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PHONE_ACCT, PhoneAccountHandle::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PHONE_ACCT)
        }
        viewModel.setCallParameters(orgUri, newUri, phoneAccountHandle)
        // タイマーを初期化（10秒から再スタート）する
        viewModel.startTimer(
            Const.CONFIRM_TIMEOUT_MILLIS,
            Const.CONFIRM_INTERVAL_MILLIS,
        )
    }

    // ---------------------------------------------------------
    // --- ハンドラメソッド----------------------------------------
    // ---------------------------------------------------------

    /**
     * 電話発信をする場合（発信のロングタップ）イベント.
     */
    private fun handleConfirm(uri: Uri, phoneAccountHandle: PhoneAccountHandle?) {
        viewModel.stopTimer()
        CallStateHolder.markAsSelfInitiated()

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = uri // 渡された Uri オブジェクトをそのまま使用
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            // 取得した PhoneAccountHandle(SIM No..etc) を発信 Intent に引き渡す.
            phoneAccountHandle?.let {
                putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
            }
        }
        try {
            Log.d(Const.TAG, "CALL_PHONE Starting...")
            startActivity(callIntent)
        } catch (e: Exception) {
            Log.e(Const.TAG, "Exception ", e)
        }
        safeFinish()
    }

    /**
     * キャンセル
     */
    private fun handleCancel() {
        Log.d(Const.TAG, "Confirmation Dialog abortCallAndFinish() called.")
        viewModel.stopTimer()
        NotificationUtil.showCancelNotification(applicationContext)
        safeFinish()
    }

    // ------------------------------------
    // 内部処理メソッド
    // ------------------------------------

    /**
     * マニフェストに定義しているが、改めて設定しないと無効になる機種対応.
     */
    private fun configureLockScreenFlags() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

    private fun safeFinish() {
        if (!isFinishing && !isDestroyed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
            Log.d(Const.TAG, "finish() called.")
            finish()
        }
        CallConfirmForegroundService.stopForegroundWithDelay(this)
    }

    /***
     * ViewModelの監視メソッド.
     */
    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onTimerFinished.collect {
                    if (!isFinishing && !isDestroyed) {
                        handleCancel()
                    }
                }
            }
        }
    }
}
