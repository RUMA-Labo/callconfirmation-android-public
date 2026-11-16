/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation

import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.main.MainActivity

/**
 * MainActivityの実装
 */
class LauncherActivity : MainActivity() {

    /**
     * 必須実装：CallRedirectionの実装クラスを戻す.
     */
    override fun tellMeCallRedirectionService(): Class<*> = RedirectionServiceImpl::class.java
}
