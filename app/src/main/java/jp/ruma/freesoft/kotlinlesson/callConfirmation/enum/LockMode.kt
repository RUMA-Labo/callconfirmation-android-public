/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.enum

/**
 * 画面ロック時の発信動作モード Enum
 * AI Agentによる自動コーディング
 */
enum class LockMode(val rawValue: Int) {
    /** 確認なく発信 (0) */
    CALL_DIRECT(0),

    /** 発信禁止 (1) */
    BLOCK_CALL(1),

    /** 確認して発信 (2) */
    CONFIRM_CALL(2),
    ;

    companion object {
        /**
         * Int 値から LockMode を取得する (該当がない場合はデフォルトの CONFIRM_CALL を返す)
         */
        fun fromInt(value: Int): LockMode = entries.firstOrNull { it.rawValue == value } ?: CALL_DIRECT
    }
}
