/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.service.CallConfirmKeepAliveService
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.PermissionUtil.hasCallPhonePermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.PermissionUtil.hasFullScreenIntentPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.PermissionUtil.hasNotificationPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.PermissionUtil.hasOverlayPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.PermissionUtil.isCallRedirectionRoleHeld
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.getAppPreference

/**
 * # 端末起動時またはパッケージ更新時に常駐サービスを自動起動するレシーバー
 * AIによる自動コーディング.
 *
 * Gemini情報）
 * 通常はACTION_LOCKED_BOOT_COMPLETEDの対応だけで十分だが、
 * Full-Disk Encryption 端末の場合、ACTION_LOCKED_BOOT_COMPLETED　が発火しない.
 *
 * ダイレクトブート状態に対応する.
 * ユースケース）カーナビと接続中に端末を再起動した場合.初回ロック解除しなくとも、カーナビと自動再接続する.
 */
class BootReceiver : BroadcastReceiver() {
    // 有効無効を切り替えるstatic関数.
    companion object {
        fun enableComponent(context: Context) {
            val receiver = ComponentName(context, BootReceiver::class.java)
            context.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        fun disableComponent(context: Context) {
            val receiver = ComponentName(context, BootReceiver::class.java)
            context.packageManager.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    /**
     * 端末起動時に常駐サービスを起動する.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_BOOT_COMPLETED
        ) {
            val appPreference = context.getAppPreference()
            val isKeepAlive = appPreference.isKeepAliveSync()
            Log.i(Const.TAG, "=== BroadcastReceiver : onReceive() detected action=$action called. keepAlive=$isKeepAlive")
            if (appPreference.isKeepAliveSync() &&
                hasCallPhonePermission(context) && isCallRedirectionRoleHeld(context) &&
                hasOverlayPermission(context) && hasNotificationPermission(context) &&
                hasFullScreenIntentPermission(context)
            ) {
                Log.i(Const.TAG, "=== BroadcastReceiver : CallConfirmKeepAliveService Service starting...")
                CallConfirmKeepAliveService.startForeground(context)
            }
        }
    }
}
