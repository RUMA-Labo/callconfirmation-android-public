/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity.Companion.EXTRA_NEW_URI
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity.Companion.EXTRA_ORG_URI
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity.Companion.EXTRA_PHONE_ACCT
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmScreen
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmViewModel
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme.CallConfirmationTheme
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.CallStateHolder
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.NotificationUtil
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.getAppPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration

/**
 * # 確認画面表示のためのフォアグラウンドサービス
 * ロック時：ヘッドアップ通知→全画面インテント表示
 * 通常時：オーバーレイウィンドウ表示
 *
 * AI Agentによるアシストコーディング
 */
class CallConfirmForegroundService :
    LifecycleService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    companion object {
        private const val AUTO_STOP_TIMER = (Const.CONFIRM_TIMEOUT_MILLIS * 2) // 画面待機時間 * 2　先にアプリからstopSelfが呼ばれる.
        private const val STOP_DELAY = 900L
        const val EXTRA_ACTION_MODE = "ACTION_MODE"
        private const val MODE_START = "START"
        private const val MODE_STOP = "STOP"

        fun startForeground(context: Context, confirm: Intent) {
            val intent = Intent(context, CallConfirmForegroundService::class.java).apply {
                putExtra(EXTRA_ACTION_MODE, MODE_START)
                putExtra(CallConfirmActivity.EXTRA_ARG_INTENT, confirm)
            }
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(
                    Const.TAG,
                    "Failed to start CallConfirmForegroundService, falling back to startActivity",
                    e,
                )
            }
        }

        fun stopForegroundWithDelay(context: Context) {
            val intent = Intent(context, CallConfirmForegroundService::class.java)
            intent.putExtra(EXTRA_ACTION_MODE, MODE_STOP)
            try {
                context.startService(intent)
                // context.stopService(intent)
            } catch (e: Exception) {
                Log.e(Const.TAG, "Failed to stop CallConfirmForegroundService", e)
            }
        }
    }

    // コルーチンを管理するための Job 変数
    private var limitTimerJob: Job? = null
    private var stopDelayJob: Job? = null

    // Overlay対応のための、メンバ変数ならびに、Compose　I/F実装 Start -----
    private var composeView: ComposeView? = null
    private lateinit var viewModel: CallConfirmViewModel
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private lateinit var _viewModelStore: ViewModelStore
    override val viewModelStore get() = _viewModelStore
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    // Overlay対応のための、メンバ変数ならびに、Compose　I/F実装 Start -----

    override fun onCreate() {
        super.onCreate()
        // OverLay対応　SavedStateRegistry の復元（サービスのため初回は null）
        savedStateRegistryController.performRestore(null)
        _viewModelStore = ViewModelStore()
        viewModel = ViewModelProvider(this)[CallConfirmViewModel::class.java]

        Log.v(Const.TAG, "+++ CallConfirmForegroundService : onCreate() called.  AUTO_STOP=${AUTO_STOP_TIMER}ms.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            if (intent.getStringExtra(EXTRA_ACTION_MODE) == MODE_START) {
                Log.v(
                    Const.TAG,
                    "+++ CallConfirmForegroundService : onStartCommand() Called. intent=$intent",
                )
                cancelStopDelayTimer()
                resetAutoStopTimer()

                // 起動時に渡された値を復元する.
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        CallConfirmActivity.EXTRA_ARG_INTENT,
                        Intent::class.java,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(CallConfirmActivity.EXTRA_ARG_INTENT)
                }
                // 確認画面を表示する.内部でstartForeground()を内部で呼び出している.
                if (confirmIntent != null) {
                    showConfirmationOrNotification(confirmIntent)
                }
            } else if (intent.getStringExtra(EXTRA_ACTION_MODE) == MODE_STOP) {
                Log.v(
                    Const.TAG,
                    "+++ CallConfirmForegroundService : stopForegroundWithDelay() called. delay=${STOP_DELAY}ms",
                )
                startStopDelayTimer()
            } else {
                Log.v(
                    Const.TAG,
                    "+++ CallConfirmForegroundService : Argument Error.",
                )
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * サービス終了時に呼ばれる.
     */
    override fun onDestroy() {
        // OverLay対応  のクリア処理
        removeOverlayView()
        _viewModelStore.clear()
        // LimitTimer
        if (limitTimerJob?.isActive == true) {
            limitTimerJob?.cancel()
            Log.d(Const.TAG, "Canceled limitTimerJob.")
        }
        limitTimerJob = null
        // StopTimer
        if (stopDelayJob?.isActive == true) {
            stopDelayJob?.cancel()
            Log.d(Const.TAG, "Canceled stopDelayJob.")
        }
        stopDelayJob = null
        Log.v(Const.TAG, "+++ CallConfirmForegroundService : onDestroy() called.")
        super.onDestroy()
    }

    // -----------------------------------------------------------
    // -- イベントハンドラ. -----------------------------------------
    // -----------------------------------------------------------

    /**
     * 電話発信をする場合（発信のロングタップ）イベント.
     */
    private fun handleConfirm(uri: Uri, phoneAccountHandle: PhoneAccountHandle?) {
        removeOverlayView() // すぐに非表示にする.
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
        stopForegroundWithDelay(applicationContext)
    }

    /**
     * キャンセル
     */
    private fun handleCancel() {
        Log.d(Const.TAG, "Confirmation Dialog abortCallAndFinish() called.")
        removeOverlayView() // すぐに非表示にする.
        viewModel.stopTimer()
        stopForegroundWithDelay(applicationContext)
    }

    // -------------------------------------------------------
    // -- 内部処理 --------------------------------------------
    // -------------------------------------------------------
    private fun resetAutoStopTimer() {
        limitTimerJob?.cancel()
        limitTimerJob = lifecycleScope.launch {
            delay(Duration.ofMillis(AUTO_STOP_TIMER))
            Log.v(Const.TAG, "+++ CallConfirmForegroundService : Auto-stop time was UP!")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startStopDelayTimer() {
        stopDelayJob?.cancel()
        stopDelayJob = lifecycleScope.launch {
            delay(Duration.ofMillis(STOP_DELAY))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.v(Const.TAG, "+++ CallConfirmForegroundService : Delay Stopped!")
        }
    }

    private fun cancelStopDelayTimer() {
        if (stopDelayJob?.isActive == true) {
            stopDelayJob?.cancel()
            Log.v(Const.TAG, "+++ CallConfirmForegroundService : Cancelled pending stopDelayJob.")
        }
        stopDelayJob = null
    }

    /**
     * 画面ロック状態に応じて、確認画面を開く or 全画面コンテンツを開くを選択する.
     */
    private fun showConfirmationOrNotification(confirmIntent: Intent) {
        // 以前の通知がのこっていれば削除する.
        NotificationUtil.cancelConfirmationNotification(this)
        // キーガードの状態が変わっている可能性があるので、最新の状態を取り直す.
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) {
            // 画面ロック状態：全画面インテント通知およびActivityの更新を行う.
            Log.d(Const.TAG, "Locked FullScreenIntent called.")
            removeOverlayView() // 残っていればクリアする.
            viewModel.stopTimer()
            NotificationUtil.startFullScreenConfirmationNotification(this, confirmIntent)
            try {
                startActivity(confirmIntent)
            } catch (e: Exception) {
                Log.e(Const.TAG, "Failed to start CallConfirmActivity", e)
            }
        } else {
            // 画面ロックが外れている状態：確認画面を表示する.
            NotificationUtil.startForegroundServiceNotification(this)

            // Intentからパラメータを取得してViewModelへ渡す
            val orgUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                confirmIntent.getParcelableExtra(EXTRA_ORG_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                confirmIntent.getParcelableExtra(EXTRA_ORG_URI)
            }
            val newUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                confirmIntent.getParcelableExtra(EXTRA_NEW_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                confirmIntent.getParcelableExtra(EXTRA_NEW_URI)
            }
            val phoneAccountHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                confirmIntent.getParcelableExtra(EXTRA_PHONE_ACCT, PhoneAccountHandle::class.java)
            } else {
                @Suppress("DEPRECATION")
                confirmIntent.getParcelableExtra(EXTRA_PHONE_ACCT)
            }

            viewModel.setCallParameters(orgUri ?: Uri.EMPTY, newUri ?: Uri.EMPTY, phoneAccountHandle)
            // 新しい発信があれば、タイマーを最初（10秒）からリスタートする
            viewModel.startTimer(
                Const.CONFIRM_TIMEOUT_MILLIS,
                Const.CONFIRM_INTERVAL_MILLIS,
            )

            if (composeView == null) {
                createOverlayWindow()
                setupViewModelObservers()
            }
        }
    }

    /**
     * Overlay対応のウィンドウを作成する.
     */
    private fun createOverlayWindow() {
        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            // 3 つの owner を設定
            setViewTreeLifecycleOwner(this@CallConfirmForegroundService)
            setViewTreeViewModelStoreOwner(this@CallConfirmForegroundService)
            setViewTreeSavedStateRegistryOwner(this@CallConfirmForegroundService)

            setContent {
                CallConfirmationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorResource(R.color.call_confirm_bg_cover),
                    ) {
                        val uiState by viewModel.uiState.collectAsState()

                        CallConfirmScreen(
                            uiState = uiState,
                            isActivity = false,
                            isOutsideDoubleTapDismiss = getAppPreference().isOutsideDismissSync(),
                            onPhoneNumberSelected = { isNewSelected ->
                                viewModel.togglePhoneNumber(isNewSelected)
                            },
                            onConfirm = { handleConfirm(uiState.currentUri, uiState.phoneAccountHandle) },
                            onCancel = { handleCancel() },
                            onTimerPause = { viewModel.pauseTimer() },
                            onTimerResume = { viewModel.resumeTimer() },
                        )
                    }
                }
            }
        }
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
        windowManager.addView(composeView, params)
    }

    private fun removeOverlayView() {
        // ComposeView を削除
        composeView?.let {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(Const.TAG, "Failed to remove overlay view", e)
            }
            composeView = null
        }
    }

    /**
     * タイマ終了監視
     */
    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onTimerFinished.collect {
                    handleCancel()
                }
            }
        }
    }
}
