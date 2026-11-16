/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation

object Const {
    const val TAG = "callConfirmation"
    const val PREFERENCE_VAL_VERSION = "100"

    // ------------------
    const val CONFIRM_TIMEOUT_MILLIS = 10000L // 画面消去までの時間10秒
    const val CONFIRM_INTERVAL_MILLIS = 125L // プログレスバー再描画間隔
    const val BUTTON_ANIMATION_MILLIS = 400 // 発信ボタンのチャージアニメ時間
    const val BUTTON_DURATION_MILLIS = 100L // 発信ボタンのタメ時間
    const val CANCEL_DURATION_MILLIS = 2500L // 発信をキャンセルした場合の通知時間
}
