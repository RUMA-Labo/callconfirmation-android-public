/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation

import jp.ruma.freesoft.kotlinlesson.callConfirmation.service.AncestorRedirectionService

/**
 * 転送サービスの実装
 */
class RedirectionServiceImpl : AncestorRedirectionService() {
    /**
     * Prefixをつけない発信の実装.
     */
    override fun acrPrefixProvider(orgNumber: String): AcrPrefixResult = AcrPrefixResult(isTransfer = false, newNumber = orgNumber)
}
