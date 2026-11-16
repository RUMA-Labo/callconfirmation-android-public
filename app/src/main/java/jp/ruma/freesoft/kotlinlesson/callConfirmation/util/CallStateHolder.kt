/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.util

/**
 * アプリ自身が掛け直した発信かどうかをメモリ上で保持するシングルトンオブジェクト
 * AI Agentによる自動コーディング
 */
object CallStateHolder {

    @Volatile
    private var isSelfInitiatedCall: Boolean = false

    /**
     * アプリからの再発信フラグをセットする
     */
    fun markAsSelfInitiated() {
        isSelfInitiatedCall = true
    }

    /**
     * アプリからの発信かどうかを確認し、確認後は自動でフラグをリセット（false）する
     */
    fun consumeSelfInitiatedFlag(): Boolean {
        val wasSelfInitiated = isSelfInitiatedCall
        isSelfInitiatedCall = false
        return wasSelfInitiated
    }
}
