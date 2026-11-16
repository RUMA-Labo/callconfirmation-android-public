/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.util

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * 権限チェックに関するユーティリティ。
 * AI Agentによる自動コーディング
 */
object PermissionUtil {
    /**
     * 電話発信権限（CALL_PHONE）を持っているかチェック
     */
    fun hasCallPhonePermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CALL_PHONE,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * 電話転送アプリ（RoleManager.ROLE_CALL_REDIRECTION）として登録されているかチェック
     */
    fun isCallRedirectionRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService<RoleManager>()
        return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
    }

    /**
     * 他のアプリの上に重ねて表示する権限（SYSTEM_ALERT_WINDOW）を持っているかチェック
     */
    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * 通知権限（POST_NOTIFICATIONS）を持っているかチェック (API 33+)
     */
    fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * フルスクリーンインテント権限（USE_FULL_SCREEN_INTENT）を持っているかチェックする (API 34+)
     * Android14以上の場合、CALL_PHONE権限があれば自動付与される.
     * カスタムOS採用の機種によっては自動付与されない場合がある.
     * 今回の実装では利用しないことにした.
     */
    fun hasFullScreenIntentPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager = context.getSystemService<NotificationManager>()
        try {
            notificationManager?.canUseFullScreenIntent() ?: true
        } catch (_: Exception) {
            true
        }
    } else {
        true
    }
}

// 便利に使えるよう Context の拡張関数として定義
fun Context.hasCallPhonePermission() = PermissionUtil.hasCallPhonePermission(this)
fun Context.isCallRedirectionRoleHeld() = PermissionUtil.isCallRedirectionRoleHeld(this)
fun Context.hasOverlayPermission() = PermissionUtil.hasOverlayPermission(this)
fun Context.hasNotificationPermission() = PermissionUtil.hasNotificationPermission(this)
fun Context.hasFullScreenIntentPermission() = PermissionUtil.hasFullScreenIntentPermission(this)
