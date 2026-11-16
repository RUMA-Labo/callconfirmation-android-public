/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration

/**
 * プロセス優先度を高く維持するための常駐フォアグラウンドサービス.
 * 電話転送に割り込む本アプリの場合、優先度が高くなるため不要となる.
 * しかしながら、一部の端末では、プロセス優先度が上がらずLMK発生で強制 KILL
 * されてしまうものがあり、その対策で必要となる.
 *
 * AIによる自動コーディング.
 */
class CallConfirmKeepAliveService : Service() {
    // 有効無効を切り替えるstatic関数.
    companion object {
        fun enableComponent(context: Context) {
            val receiver = ComponentName(context, CallConfirmKeepAliveService::class.java)
            context.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        fun disableComponent(context: Context) {
            val receiver = ComponentName(context, CallConfirmKeepAliveService::class.java)
            context.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        fun startForeground(context: Context) {
            val intent = Intent(context, CallConfirmKeepAliveService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(Const.TAG, "Failed to start CallConfirmKeepAliveService", e)
            }
        }
        fun stopForeground(context: Context) {
            val intent = Intent(context, CallConfirmKeepAliveService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(Const.TAG, "Failed to stop CallConfirmKeepAliveService", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // サービス専用のスコープを作成（SupervisorJobを使うと、1つのエラーで全体が落ちるのを防げます）
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var counterJob: Job? = null

    /**
     * サービス開始時に呼ばれる.
     */
    override fun onCreate() {
        super.onCreate()
        Log.v(Const.TAG, "+++ CallConfirmKeepAliveService : onCreate() called.")
        // 内部でstartForeground()を呼び出している.
        NotificationUtil.startKeepAliveNotification(this)
    }

    /**
     * サービス開始時に呼ばれる.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "+++ CallConfirmKeepAliveService : onStartCommand() called.  intent=$intent.")
        debugKeepAliveMessageTask()
        return START_STICKY
    }

    /**
     * Debug用：キープアライブ稼働メッセージ.
     *
     *-assumenosideeffects class jp.ruma.freesoft.kotlinlesson.callConfirmation.service.CallConfirmKeepAliveService {
     *     private debugKeepAliveMessageTask();
     * }
     */
    private fun debugKeepAliveMessageTask() {
        counterJob?.cancel()
        var count: Long = 0
        counterJob = serviceScope.launch {
            while (true) {
                Log.d(Const.TAG, "Keep-alive Active... ${count++}")
                delay(Duration.ofSeconds(1))
            }
        }
    }

    /**
     * サービス終了時に呼ばれる.
     */
    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.v(Const.TAG, "+++ CallConfirmKeepAliveService : onDestroy() called.")
        counterJob?.cancel()
        counterJob = null
        serviceScope.cancel()

        super.onDestroy()
    }
}
