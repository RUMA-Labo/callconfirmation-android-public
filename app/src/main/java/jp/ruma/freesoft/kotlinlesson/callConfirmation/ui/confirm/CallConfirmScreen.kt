/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme.CallConfirmationTheme
import kotlinx.coroutines.delay

/**
 * 電話番号の確認画面のコンポーネント
 * AI Agentによる自動コーディング
 */
@Composable
fun CallConfirmScreen(
    uiState: CallConfirmUiState,
    isActivity: Boolean = true,
    isOutsideDoubleTapDismiss: Boolean = true,
    onPhoneNumberSelected: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onTimerPause: () -> Unit,
    onTimerResume: () -> Unit,
) {
    // EXTRA_ORG_URI と EXTRA_NEW_URI とが異なっていれば、プルダウンを表示し背景を切り替える
    val isDropdownEligible = uiState.newUri != uiState.orgUri
    val backgroundColor = if (isDropdownEligible) {
        if (uiState.currentUri == uiState.newUri) {
            colorResource(R.color.call_confirm_prefix_bg) // 茶色 (付加番号)
        } else {
            colorResource(R.color.call_confirm_original_bg) // 青色 (オリジナル)
        }
    } else {
        colorResource(R.color.call_confirm_none_prefix_bg) // 黒色　(ACR　Not Used)
    }

    var expanded by remember { mutableStateOf(false) }

    if (isActivity) {
        BackHandler(onBack = onCancel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then( // 枠外ダブルクリックで終了（全体でダブルクリックを検知）
                if (isOutsideDoubleTapDismiss) {
                    Modifier.pointerInput(onCancel) {
                        detectTapGestures(
                            onDoubleTap = {
                                onCancel()
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    // 枠外ダブルクリックで終了（枠内ダブルクリックは無視）
                    detectTapGestures { /* consume taps inside dialog card */ }
                },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2) 電話番号を表示する行の設定 (フォント大、黄色、背景色変更可能、タップ可能)
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // ダイアログの横幅いっぱいに広げる
                        .clip(RoundedCornerShape(4.dp))
                        .background(backgroundColor)
                        .clickable(enabled = isDropdownEligible) {
                            expanded = true
                        }
                        .padding(vertical = 14.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        // 異なっている場合は現在の選択番号、同じ場合は displayNewNumber を表示
                        text = if (isDropdownEligible) uiState.displayPhoneNumber else uiState.displayNewNumber,
                        color = colorResource(R.color.call_confirm_number_text), // 文字色: 黄色
                        fontSize = 28.sp, // 大きなフォントサイズで表示
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        softWrap = true, // 長い場合は折り返しを許可
                    )

                    // 3) プルダウン用DropdownMenu
                    if (isDropdownEligible) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.wrapContentSize(),
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = uiState.displayNewNumber) },
                                onClick = {
                                    onPhoneNumberSelected(true) // New を選択
                                    expanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = uiState.displayOrgNumber) },
                                onClick = {
                                    onPhoneNumberSelected(false) // Original を選択
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { uiState.remainingTimeMillis.toFloat() / Const.CONFIRM_TIMEOUT_MILLIS },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    drawStopIndicator = {},
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_cancel),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    CallConfirmProgressButton(
                        modifier = Modifier.weight(1f),
                        onComplete = onConfirm,
                        onPressStart = onTimerPause,
                        onPressEnd = onTimerResume,
                    )
                }
            }
        }
    }
}

@Composable
private fun CallConfirmProgressButton(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
) {
    var isPressing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(Const.BUTTON_ANIMATION_MILLIS, easing = LinearEasing),
            )
            delay(timeMillis = Const.BUTTON_DURATION_MILLIS)
            onComplete()
            isPressing = false
        } else {
            progress.stop()
            progress.snapTo(0f)
        }
    }

    val baseColor = MaterialTheme.colorScheme.primaryContainer
    val liquidColors = listOf(
        colorResource(R.color.call_confirm_button_liquid_start),
        colorResource(R.color.call_confirm_button_liquid_center),
        colorResource(R.color.call_confirm_button_liquid_end),
    )

    val contentDesc = stringResource(R.string.confirm_apply).replace("\n", " ")

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(baseColor)
            .semantics {
                contentDescription = contentDesc
            }
            .drawBehind {
                val width = size.width
                val height = size.height
                val animatedProgress = progress.value
                val fillHeight = height * animatedProgress

                if (fillHeight > 0) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = liquidColors,
                            startY = height,
                            endY = height - fillHeight,
                        ),
                        topLeft = Offset(0f, height - fillHeight),
                        size = Size(width, fillHeight),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressing = true
                        onPressStart() // タイマー一時停止
                        try {
                            awaitRelease()
                        } finally {
                            isPressing = false
                            onPressEnd() // タイマー再開
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val color = if (progress.value > 0.5f) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null, // Text handles the description
                tint = color,
            )
            Text(
                text = stringResource(R.string.confirm_apply),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallConfirmScreenPreview() {
    CallConfirmationTheme {
        CallConfirmScreen(
            uiState = CallConfirmUiState(
                currentUri = "tel:09012345678".toUri(),
                orgUri = "tel:09012345678".toUri(),
                newUri = "tel:09012345678".toUri(),
                remainingTimeMillis = 3000L,
            ),
            isActivity = true,
            isOutsideDoubleTapDismiss = true,
            onPhoneNumberSelected = {},
            onConfirm = {},
            onCancel = {},
            onTimerPause = {},
            onTimerResume = {},
        )
    }
}
