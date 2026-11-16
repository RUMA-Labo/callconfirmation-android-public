/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.main

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.enum.LockMode
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme.CallConfirmationTheme

/**
 * メイン画面の Jetpack Compose Screen
 * XMLベース画面からのAI Agentによる自動移行　＋　一部手修正
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onLockModeSelected: (LockMode) -> Unit,
    onKeepAliveChanged: (Boolean) -> Unit,
    onOutsideDismissChanged: (Boolean) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onGrantPermissionClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val headerBgColor = if (uiState.allGranted && uiState.isServiceEnabled) {
        colorResource(R.color.main_header_bg)
    } else {
        colorResource(R.color.main_header_bg_ng)
    }
    val colorGreen = colorResource(R.color.status_green)
    val colorRed = colorResource(R.color.status_red)
    val enabled = uiState.allGranted && uiState.isServiceEnabled
    val lockModeOptions = stringArrayResource(R.array.keyguard_mode_options)
    val explainTexts = stringArrayResource(R.array.keyguard_mode_explains)

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.main_button_close),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // 1) Overview
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.main_title1),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                        Modifier.weight(2f)
                            .background(headerBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (uiState.allGranted && uiState.isServiceEnabled) {
                                stringResource(R.string.main_status_active)
                            } else {
                                stringResource(R.string.main_status_inactive)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        if (enabled) {
                            Switch(
                                checked = uiState.allGranted && uiState.isServiceEnabled,
                                onCheckedChange = { isChecked -> onActiveChanged(isChecked) },
                                enabled = uiState.allGranted,
                                modifier = Modifier.height(32.dp).scale(0.8f),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.main_overview),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
                if (enabled) {
                    // 2) Outgoing Call Behavior on Lock Screen
                    Text(
                        text = stringResource(R.string.main_title2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    // Use Outside Double Tap for dismiss
                    CheckboxRow(
                        checked = uiState.outsideDismissEnabled,
                        onCheckedChange = { onOutsideDismissChanged(it) },
                        enabled = uiState.allGranted,
                        label = stringResource(R.string.main_outside_dismiss_label),
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        text = stringResource(R.string.main_locked_action),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Spinner for LockMode using CompactDropdownSpinner
                        CompactDropdownSpinner(
                            selectedValue = lockModeOptions.getOrElse(uiState.lockMode.rawValue) { "" },
                            options = lockModeOptions,
                            onSelected = { index -> onLockModeSelected(LockMode.fromInt(index)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = explainTexts.getOrElse(uiState.lockMode.rawValue) { "" },
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        // KeepAlive Switch
                        SwitchRow(
                            checked = uiState.keepAliveEnabled,
                            onCheckedChange = { onKeepAliveChanged(it) },
                            enabled = uiState.allGranted,
                            label = stringResource(R.string.main_keepalive_label),
                        )
                        Text(
                            text = stringResource(R.string.main_keepalive_explain),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                // 3) Permission Status
                Text(
                    text = stringResource(R.string.main_title3),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.main_header_bg))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    PermissionRow(
                        checked = uiState.isCallRedirection,
                        text = stringResource(R.string.main_permit1_redirection),
                        textColor = if (uiState.isCallRedirection) colorGreen else colorRed,
                    )
                    PermissionRow(
                        checked = uiState.hasCallPermission,
                        text = stringResource(R.string.main_permit2_call),
                        textColor = if (uiState.hasCallPermission) colorGreen else colorRed,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            checked = uiState.hasNotificationPermission,
                            text = stringResource(R.string.main_permit4_notification),
                            textColor = if (uiState.hasNotificationPermission) colorGreen else colorRed,
                        )
                    }
                    PermissionRow(
                        checked = uiState.hasOverlayPermission,
                        text = stringResource(R.string.main_permit3_overlay),
                        textColor = if (uiState.hasOverlayPermission) colorGreen else colorRed,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        PermissionRow(
                            checked = uiState.hasFullScreenIntentPermission,
                            text = stringResource(R.string.main_permit5_full_screen_intent),
                            textColor = if (uiState.hasFullScreenIntentPermission) colorGreen else colorRed,
                        )
                    }
                }

                if (!uiState.allGranted) {
                    Button(
                        onClick = onGrantPermissionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    ) {
                        Text(text = stringResource(R.string.main_button_permit))
                    }
                }

                if (!enabled) {
                    // 4)Explain Section
                    Text(
                        text = stringResource(R.string.main_title4),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(R.color.main_header_bg))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        text = stringArrayResource(R.array.permission_explain).joinToString("\n"),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                // 5) Privacy Policy Section
                Text(
                    text = stringResource(R.string.main_title5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.main_header_bg))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Text(
                    text = stringArrayResource(R.array.privacy_policy).joinToString("\n"),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDropdownSpinner(
    selectedValue: String,
    options: Array<String>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedValue,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(start = 8.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onCheckedChange(it) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
        Text(
            text = label,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(start = 8.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onCheckedChange(it) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.height(32.dp).scale(0.8f),
        )
    }
}

@Composable
fun PermissionRow(
    checked: Boolean,
    text: String,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = checked,
            onClick = null,
            modifier = Modifier.height(24.dp).scale(0.8f),
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CallConfirmationTheme {
        MainScreen(
            uiState = MainUiState(
                lockMode = LockMode.CALL_DIRECT,
                allGranted = true,
                outsideDismissEnabled = true,
                hasCallPermission = true,
                isCallRedirection = true,
                hasNotificationPermission = true,
                hasOverlayPermission = true,
                hasFullScreenIntentPermission = true,
                keepAliveEnabled = false,
                isServiceEnabled = true,
            ),
            onLockModeSelected = {},
            onKeepAliveChanged = {},
            onActiveChanged = {},
            onGrantPermissionClick = {},
            onCloseClick = {},
            onOutsideDismissChanged = {},
        )
    }
}
