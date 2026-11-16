/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.service

import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.SecureLogger
import jp.ruma.freesoft.kotlinlesson.callConfirmation.enum.LockMode
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.CallStateHolder
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.NotificationUtil
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.getAppPreference
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasCallPhonePermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasFullScreenIntentPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasNotificationPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasOverlayPermission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 発信リダイレクトサービス。
 * 発信要求を一時的にキャンセルし、ユーザーに確認ダイアログを表示する。
 * AI Agentによるアシストコーディング
 */
abstract class AncestorRedirectionService : CallRedirectionService() {
    companion object {
        protected const val SCHEMA_TEL = "tel"
    }

    /**
     * プレフィクス付与の結果の戻り値クラス
     * @param isTransfer 転送発信の場合true
     * @param newNumber プレフィクス付与後の番号
     */
    data class AcrPrefixResult(
        val isTransfer: Boolean,
        val newNumber: String,
    )

    /**
     * プレフィクス付与するプロバイダ実装を戻す.
     */
    protected abstract fun acrPrefixProvider(orgNumber: String): AcrPrefixResult

    // -------------------------------------------------------
    // -- コールバック処理 --------------------------------------
    // -------------------------------------------------------

    /**
     * 発信リダイレクト時のアクションを定義しておく.
     */
    private sealed class RedirectionAction {
        data object PlaceUnmodified : RedirectionAction() // そのまま発信する
        data object CancelCall : RedirectionAction() // キャンセル
        data class Redirect(val newUri: Uri) : RedirectionAction() // 転送発信をする
        data class ShowConfirmation(val newUri: Uri, val orgUri: Uri) : RedirectionAction() // 確認ダイアログに進む
    }

    /**
     * 電話発信を継続する.
     * @param handle 発信中の電話番号 スキーマ込　国際番号が入る機種あり(tel:0ABJ... or tel:+81ABJ...)
     * @param initialTargetPhoneAccount 発信に選択したSIM
     * @param allowRedirection 非割り込みモード：実際にはfalseにならないので判定に使えない.
     */
    override fun onPlaceCall(
        handle: Uri,
        initialTargetPhoneAccount: PhoneAccountHandle,
        allowRedirection: Boolean,
    ) {
        SecureLogger.log("===onPlaceCall() called : handle = $handle, initialTargetPhoneAccount = $initialTargetPhoneAccount, allowRedirection = $allowRedirection")
        /*** Debug Code : for ANR TEST *********
         * Thread.sleep(5500)
         * cancelCall()
         * return
         ***************************************/

        // 1. 発信、転送、確認、キャンセルの４択を判定する.
        val action = try {
            evaluateRedirectionAction(handle, allowRedirection)
        } catch (e: Throwable) {
            // 例外が発生した場合はログを出力し、フォールバック（通常の電話として発信）
            Log.e(Const.TAG, "Error in evaluateRedirectionAction, falling back to unmodified call", e)
            RedirectionAction.PlaceUnmodified
        }
        SecureLogger.log("evaluateRedirectionAction = $action")

        // 2. 決定したアクションに応じてシステムへ応答する
        when (action) {
            is RedirectionAction.PlaceUnmodified -> { // 発信
                placeCallUnmodified()
            }

            is RedirectionAction.Redirect -> { // 転送
                redirectCall(action.newUri, initialTargetPhoneAccount, false)
            }

            is RedirectionAction.ShowConfirmation -> { // 確認画面表示
                startForegroundService(action.newUri, action.orgUri, initialTargetPhoneAccount) // 先にフォアグラウンドサービスを起動し、確認画面処理を委譲する.
                cancelCall() // 一旦電話を切る:　呼び出した時点で、BinderIPC）を介してTelecom サービスに通話の破棄が非同期通知される.電話転送状態が終わるとプロセス優先度が下がる.
            }

            is RedirectionAction.CancelCall -> { // キャンセル
                val locale = Locale.getDefault()
                val pattern = "MM/dd HH:mm:ss"
                val sdf = SimpleDateFormat(pattern, locale)
                val timeStr = sdf.format(Date())
                val contentText = getString(R.string.blocked_notify_time_format, timeStr)
                NotificationUtil.showBlockedNotification(this, contentText)
                cancelCall()
            }
        }
    }

    /**
     * API33から利用できる.
     * 呼ばれたときはすでにANRが発生している.
     * 追加で通知を出力する.
     */
    override fun onRedirectionTimeout() {
        Log.i(Const.TAG, "=== onRedirectionTimeout() called.")
        NotificationUtil.showBlockedNotification(this, getString(R.string.blocked_notify_anr))
    }

    // -------------------------------------------------------
    // -- 内部処理 --------------------------------------------
    // -------------------------------------------------------

    /**
     * 発信に対するアクションを判定する（cancelCall呼び出し前の純粋なロジック処理）
     */
    private fun evaluateRedirectionAction(
        handle: Uri,
        allowRedirection: Boolean,
    ): RedirectionAction {
        val orgPhoneNumber = handle.schemeSpecificPart ?: ""

        // 1. 自アプリからの発信チェック
        if (CallStateHolder.consumeSelfInitiatedFlag()) {
            return RedirectionAction.PlaceUnmodified
        }
        // 2. 権限チェック
        if (!hasCallPhonePermission() || !hasOverlayPermission() || !hasNotificationPermission() || !hasFullScreenIntentPermission()) {
            Log.e(Const.TAG, "Permission denied")
            return RedirectionAction.PlaceUnmodified
        }
        // データストアから PREFERENCE_KEY_LOCKMODE を同期読み出して変数に格納
        val lockMode = getAppPreference().getLockModeEnumSync()
        val isLocked = isRemoteControlDeviceUsing() && (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true)
        // 3. 発信抑止チェック
        if (isOutgoingBlocked(isLocked, lockMode)) {
            Log.i(Const.TAG, "BLOCKING MODE : call cancelCall().")
            return RedirectionAction.CancelCall
        }
        // 4. ACR処理 (プレフィクス付与)
        val scheme = handle.scheme ?: SCHEMA_TEL
        val (finalNewUri, finalOrgUri, isTransfer) = if (scheme == SCHEMA_TEL) {
            val result = acrPrefixProvider(orgPhoneNumber)
            Triple(Uri.fromParts(SCHEMA_TEL, result.newNumber, null), handle, result.isTransfer)
        } else {
            Triple(handle, handle, false)
        }
        // 5. None UI モード判定
        if (!allowRedirection || isNoneUiMode(isLocked, lockMode)) {
            return if (isTransfer) {
                Log.i(Const.TAG, "None UI mode : call redirectCall().")
                RedirectionAction.Redirect(finalNewUri)
            } else {
                Log.i(Const.TAG, "None UI mode : call placeCallUnmodified().")
                RedirectionAction.PlaceUnmodified
            }
        }
        // 6. 通常時（確認UIを表示）
        Log.i(Const.TAG, "Call Confirmation dialog : isTransfer=$isTransfer.")
        return RedirectionAction.ShowConfirmation(finalNewUri, finalOrgUri)
    }

    /**
     * 発信抑止モード判定
     * @return 判定結果:
     * - `true`: 電話発信を禁止する.
     * - `false`: 電話発信を許可する(通常状態).
     */
    protected open fun isOutgoingBlocked(isLocked: Boolean, lockMode: LockMode): Boolean = isLocked && lockMode == LockMode.BLOCK_CALL

    /**
     * UIモード判定
     * @return 判定結果:
     * - `true`: 確認画面を出さずに電話発信する.
     * - `false`: 確認画面を表示する.
     */
    protected open fun isNoneUiMode(isLocked: Boolean, lockMode: LockMode): Boolean {
        // Android Auto または Car Mode（車載UIモード）
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
            return true
        }
        if (lockMode == LockMode.CALL_DIRECT && isLocked) {
            return true
        }
        return false
    }

    /**
     * Bluetooth機器またはヘッドセットのように、リモートで電話発信可能な装置を利用中か？
     * @return 判定結果:
     * - `true`: 電話発信に関わる機器が接続中.
     * - `false`: その他.
     */
    protected fun isRemoteControlDeviceUsing(): Boolean {
        // Bluetooth機器またはヘッドセットを利用中か？
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return false
        // メモ）ヘッドセットから電話発信した場合、onPlaceCall()処理中はTYPE_BLUETOOTH_SCOへの切り替えが
        // 間に合わず、待機状態であるTYPE_BLUETOOTH_A2DPが戻ってくる.
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
        return devices.any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, // Bluetooth機器（待機/音楽）
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, // Bluetooth機器（通話時）
                AudioDeviceInfo.TYPE_USB_HEADSET, // USBヘッドセット
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                -> true

                // 3.5mm有線ヘッドセット
                else -> false
            }
        }
    }

    /**
     * LMK対策のフォアグラウンドサービスを開始する.
     * @param newUri prefix付きの発信先URI
     * @param orgUri オリジナルの発信先URI
     * @param phoneAccount 発信に選択したSIM
     */
    private fun startForegroundService(
        newUri: Uri,
        orgUri: Uri,
        phoneAccount: PhoneAccountHandle,
    ) {
        // 確認画面のインテントを作成する.
        val confirmIntent = Intent(applicationContext, CallConfirmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
            putExtra(CallConfirmActivity.EXTRA_NEW_URI, newUri)
            putExtra(CallConfirmActivity.EXTRA_ORG_URI, orgUri)
            putExtra(CallConfirmActivity.EXTRA_PHONE_ACCT, phoneAccount)
        }
        CallConfirmForegroundService.startForeground(applicationContext, confirmIntent)
    }
}
