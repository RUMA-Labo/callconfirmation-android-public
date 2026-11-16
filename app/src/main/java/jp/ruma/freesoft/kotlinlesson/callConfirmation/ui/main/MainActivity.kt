/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.main

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R
import jp.ruma.freesoft.kotlinlesson.callConfirmation.receiver.BootReceiver
import jp.ruma.freesoft.kotlinlesson.callConfirmation.service.CallConfirmKeepAliveService
import jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.theme.CallConfirmationTheme
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.NotificationUtil
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.getAppPreference
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasCallPhonePermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasFullScreenIntentPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasNotificationPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.hasOverlayPermission
import jp.ruma.freesoft.kotlinlesson.callConfirmation.util.isCallRedirectionRoleHeld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import java.time.Duration

private const val UPDATE_WAIT_LIMIT = 4000L
private const val UPDATE_WAIT_INTERVAL = 500L
private const val RESTART_MAX_DELAY = 3000L

/**
 * メイン画面の Activity (Jetpack Compose 移行版)
 * AI Agentによるアシストコーディング
 */
abstract class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var initFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        initFlag = true
        super.onCreate(savedInstanceState)
        // 通知を初期化する.
        NotificationUtil.setupNotificationChannels(applicationContext)
        // LMKなどで発報したままのとなる通知残骸を刈り取る.
        NotificationUtil.cancelConfirmationNotification(applicationContext)

        // バージョン番号を取り込む.
        val versionNo = getAppPreference().getVersionSync()
        // バージョン情報を DataStore に保存
        lifecycleScope.launch {
            if (versionNo != Const.PREFERENCE_VAL_VERSION) {
                getAppPreference().saveVersion(Const.PREFERENCE_VAL_VERSION)
            }
        }
        // AppPreferenceの変更を監視してViewModelのUiStateへ同期する
        lifecycleScope.launch {
            getAppPreference().lockModeEnumFlow.collect { mode ->
                viewModel.updateLockMode(mode)
            }
        }
        lifecycleScope.launch {
            getAppPreference().keepAliveFlow.collect { enabled ->
                viewModel.updateKeepAlive(enabled)
            }
        }
        lifecycleScope.launch {
            getAppPreference().outsideDismissFlow.collect { enabled ->
                viewModel.updateOutsideDismiss(enabled)
            }
        }

        // 権限状態とサービス状態の初期チェック
        checkPermissionStatus()
        // 常駐サービスの状態の状態を同期する
        val isKeepAliveEnabled = getAppPreference().isKeepAliveSync()
        controlActivateKeepAliveService(isKeepAliveEnabled)

        setContent {
            CallConfirmationTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(
                    uiState = uiState,
                    onLockModeSelected = { mode ->
                        viewModel.updateLockMode(mode)
                        lifecycleScope.launch {
                            getAppPreference().saveLockMode(mode)
                        }
                    },
                    onKeepAliveChanged = { isChecked ->
                        viewModel.updateKeepAlive(isChecked)
                        lifecycleScope.launch {
                            getAppPreference().saveKeepAlive(isChecked)
                        }
                        controlActivateKeepAliveService(isChecked)
                    },
                    onActiveChanged = { isChecked ->
                        if (!isChecked) {
                            restartWithDisabled()
                        }
                    },
                    onGrantPermissionClick = {
                        doRequestNextPermission()
                    },
                    onCloseClick = {
                        finish()
                    },
                    onOutsideDismissChanged = { isChecked ->
                        viewModel.updateOutsideDismiss(isChecked)
                        lifecycleScope.launch {
                            getAppPreference().saveOutsideDismiss(isChecked)
                        }
                    },
                )
            }
        }

        // ViewModelのStateFlowを監視して、権限や設定変更時に再評価する
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { _ ->
                checkPermissionStatus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!initFlag) {
            checkPermissionStatus()
        }
        initFlag = false
    }

    /**
     *  CallRedirection Serviceを戻す
     */
    protected abstract fun tellMeCallRedirectionService(): Class<*>

    /**
     *  CallRedirection Serviceを無効化し、反映をまって自分を再起動する.
     */
    private fun restartWithDisabled() {
        val myService = ComponentName(this, tellMeCallRedirectionService())
        try {
            packageManager.setComponentEnabledSetting(
                myService,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            Log.e(Const.TAG, "callRedirection Service=$myService not found. ${e.message}")
            return
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        )
        Toast.makeText(applicationContext, R.string.main_toast_disabled, Toast.LENGTH_LONG).show()
        viewModel.setLoading(true)
        // パッケージマネージャへの反映を待ち合わせる.
        lifecycleScope.launch {
            try {
                if (viewModel.uiState.value.keepAliveEnabled) {
                    viewModel.updateKeepAlive(false)
                    getAppPreference().saveKeepAlive(false)
                }

                val success = withContext(Dispatchers.Default) {
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < UPDATE_WAIT_LIMIT) {
                        if (!isCallRedirectionRoleHeld()) {
                            Log.d(Const.TAG, "Time until reflected=${System.currentTimeMillis() - startTime}")
                            return@withContext true
                        }
                        delay(Duration.ofMillis(UPDATE_WAIT_INTERVAL))
                    }
                    false
                }

                val mySelf = packageManager.getLaunchIntentForPackage(packageName)
                if (mySelf != null) {
                    Log.d(Const.TAG, "watchdog=$success, delay=$RESTART_MAX_DELAY")
                    if (!success) { // 反映がされない場合、ディレイスタートする.
                        delay(Duration.ofMillis(RESTART_MAX_DELAY))
                    }
                    mySelf.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(mySelf)
                }
            } finally {
                viewModel.setLoading(false)
                finish()
            }
        }
    }

    private fun checkPermissionStatus() {
        val hasCallPermission = hasCallPhonePermission()
        val isCallRedirection = isCallRedirectionRoleHeld()
        val hasOverlayPermission = hasOverlayPermission()
        val hasNotificationPermission = hasNotificationPermission()
        val hasFullScreenIntentPermission = hasFullScreenIntentPermission()
        val allGranted = hasCallPermission && isCallRedirection && hasOverlayPermission && hasNotificationPermission && hasFullScreenIntentPermission

        val service = ComponentName(this, tellMeCallRedirectionService())
        val isServiceEnabled = packageManager.getComponentEnabledSetting(service) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        viewModel.updatePermissionsAndServiceState(
            allGranted = allGranted,
            hasCall = hasCallPermission,
            isRedirection = isCallRedirection,
            hasOverlay = hasOverlayPermission,
            hasNotification = hasNotificationPermission,
            hasFullScreen = hasFullScreenIntentPermission,
            isServiceEnabled = isServiceEnabled,
        )

        val isKeepAliveEnabled = getAppPreference().isKeepAliveSync()
        if (!allGranted && isKeepAliveEnabled) {
            viewModel.updateKeepAlive(false)
            lifecycleScope.launch {
                getAppPreference().saveKeepAlive(false)
            }
            controlActivateKeepAliveService(false)
        }
    }

    private fun doRequestNextPermission() {
        when {
            !isCallRedirectionRoleHeld() -> requestCallRedirectionRole()
            !hasCallPhonePermission() -> requestPhonePermission()
            !hasNotificationPermission() -> requestNotificationPermission()
            !hasOverlayPermission() -> requestOverlayPermission()
            !hasFullScreenIntentPermission() -> requestFullScreenIntentPermission()
            else -> checkPermissionStatus()
        }
    }

    private fun controlActivateKeepAliveService(isKeepAliveEnabled: Boolean) {
        val hasCallPermission = hasCallPhonePermission()
        val isCallRedirection = isCallRedirectionRoleHeld()
        val hasOverlayPermission = hasOverlayPermission()
        val hasNotificationPermission = hasNotificationPermission()
        val hasFullScreenIntentPermission = hasFullScreenIntentPermission()
        val allGranted = hasCallPermission && isCallRedirection && hasOverlayPermission && hasNotificationPermission && hasFullScreenIntentPermission

        if (allGranted && isKeepAliveEnabled) {
            CallConfirmKeepAliveService.enableComponent(applicationContext)
            BootReceiver.enableComponent(applicationContext)
            CallConfirmKeepAliveService.startForeground(applicationContext)
        } else {
            CallConfirmKeepAliveService.stopForeground(applicationContext)
            BootReceiver.disableComponent(applicationContext)
            CallConfirmKeepAliveService.disableComponent(applicationContext)
        }
    }

    private fun requestPhonePermission() {
        _callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun requestCallRedirectionRole() {
        val service = ComponentName(this, tellMeCallRedirectionService())
        Log.d(Const.TAG, "service=$service")
        packageManager.setComponentEnabledSetting(
            service,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
            _roleRequestLauncher.launch(intent)
        } else {
            Log.e(Const.TAG, "isRoleAvailable = false")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri(),
        )
        _overlayPermissionLauncher.launch(intent)
    }

    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.fromParts("package", packageName, null),
            )
            _fullScreenIntentPermissionLauncher.launch(intent)
        }
    }

    private val _callPhonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        checkPermissionStatus()
        if (isGranted) {
            doRequestNextPermission()
        } else {
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)
            Log.d(Const.TAG, "shouldShowRequestPermissionRationale=$showRationale")
            if (!showRationale) {
                Toast.makeText(this, R.string.main_toast_permit1_recover, Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.main_toast_permit1_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val _roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        checkPermissionStatus()
        if (isCallRedirectionRoleHeld()) {
            doRequestNextPermission()
        } else {
            startActivity(
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private val _notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        checkPermissionStatus()
        if (isGranted) {
            doRequestNextPermission()
        }
    }

    private val _overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        checkPermissionStatus()
        if (hasOverlayPermission()) {
            doRequestNextPermission()
        }
    }

    private val _fullScreenIntentPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        checkPermissionStatus()
        if (hasFullScreenIntentPermission()) {
            doRequestNextPermission()
        }
    }
}
