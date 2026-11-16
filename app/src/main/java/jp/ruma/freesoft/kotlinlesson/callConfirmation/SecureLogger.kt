/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation

import android.util.Log

/**
 * # セキュア情報をデバッグ出力するラッパークラス.
 *
 * リリース物件ではこのクラス自体をproguardにより抹消する.
 * ```proguard
 * -assumenosideeffects class jp.ruma.freesoft.**.SecureLogger {
 *     *;
 *  }
 *  -assumenosideeffects class android.util.Log {
 *     public static *** d(...);
 *     public static *** v(...);
 *     public static *** i(...);
 * }
 * ```
 */
object SecureLogger {
    init {
        Log.v(Const.TAG, "=== Debug Logger Mode : SecureLogger is Enabled. ===")
    }

    fun log(message: String) {
        Log.v(Const.TAG, message)
    }
}
