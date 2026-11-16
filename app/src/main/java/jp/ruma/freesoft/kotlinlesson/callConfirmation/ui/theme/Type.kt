/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 *  AI Agentによる自動コーディング
 */

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
