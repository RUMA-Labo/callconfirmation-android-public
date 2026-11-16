/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.R

/**
 * 通知系ヘルパクラス
 * AI Agentによるアシストコーディング
 */
object
NotificationUtil {
    private const val CHANNEL_ID1_FSINTENT = "id1_fullscreen"
    private const val CHANNEL_ID2_ACTIVITY = "id2_activity"
    private const val CHANNEL_ID3_BLOCKED = "id3_blocked"
    private const val CHANNEL_ID4_CANCEL = "id4_cancel"
    private const val CHANNEL_ID5_KEEPALIVE = "id5_keepalive"

    private const val NOTIFICATION_ID1_FSINTENT = 2001
    private const val NOTIFICATION_ID2_ACTIVITY = 2002
    private const val NOTIFICATION_ID3_BLOCKED = 2003
    private const val NOTIFICATION_ID4_CANCELED = 2004
    private const val NOTIFICATION_ID5_KEEPALIVE = 2005

    /**
     * 通知チャネルが設定されていない場合に設定する.
     * @param appContext アプリケーションコンテキスト
     */
    fun setupNotificationChannels(appContext: Context) {
        val notificationManager =
            appContext.getSystemService(NotificationManager::class.java) ?: return

        // 全画面インテント用通知チャネルを作成する.
        val channel1 = NotificationChannel(
            CHANNEL_ID1_FSINTENT,
            appContext.getString(R.string.channel_id1_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.channel_id1_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel1)

        // 通常画面表示用通知チャネルを作成する.
        val channel2 = NotificationChannel(
            CHANNEL_ID2_ACTIVITY,
            appContext.getString(R.string.channel_id2_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.channel_id2_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel2)

        // アラート（発信自動ブロック）用通知チャネルを作成する.
        val channel3 = NotificationChannel(
            CHANNEL_ID3_BLOCKED,
            appContext.getString(R.string.channel_id3_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.channel_id3_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel3)

        // 手動キャンセル用通知チャネル（サイレント＋ヘッドアップ）を作成する.
        val channel4 = NotificationChannel(
            CHANNEL_ID4_CANCEL,
            appContext.getString(R.string.channel_id4_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.channel_id4_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel4)

        // プロセス優先度維持（常駐）用通知チャネルを作成する.
        val channel5 = NotificationChannel(
            CHANNEL_ID5_KEEPALIVE,
            appContext.getString(R.string.channel_id5_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.channel_id5_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel5)
    }

    /**
     * 直近の発信ブロック（誤操作）を通知で表示する.
     */
    fun showBlockedNotification(context: Context, message: String? = null) {
        val channelId = CHANNEL_ID3_BLOCKED
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val notification =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notify_app)
                .setContentTitle(context.getString(R.string.blocked_notify_title))
                .setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS).build()
        try {
            notificationManager?.notify(NOTIFICATION_ID3_BLOCKED, notification)
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to show cancel notification", e)
        }
    }

    /**
     * 前回の確認画面通知を解除する.
     */
    fun cancelConfirmationNotification(context: Context) {
        val notificationManager =
            context.getSystemService(NotificationManager::class.java) ?: return
        try {
            notificationManager.cancel(NOTIFICATION_ID1_FSINTENT)
            notificationManager.cancel(NOTIFICATION_ID2_ACTIVITY)
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to cancel confirmation notification", e)
        }
    }

    /**
     * 確認画面からのユーザ操作による発信中止を通知する.
     */
    fun showCancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID4_CANCEL)
            .setSmallIcon(R.drawable.ic_notify_app)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.confirm_notify_cancel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setAutoCancel(true)
            .setTimeoutAfter(Const.CANCEL_DURATION_MILLIS).build()
        try {
            notificationManager.notify(NOTIFICATION_ID4_CANCELED, notification)
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to show cancel notification", e)
        }
    }

    /**
     * サービスをフォアグラウンド化する
     * IMPORTANCE_LOWのプライオリティに表示する.
     */
    fun startForegroundServiceNotification(service: Service) {
        val notification = NotificationCompat.Builder(service, CHANNEL_ID2_ACTIVITY)
            .setContentTitle(service.getString(R.string.app_name))
            .setContentText("")
            .setSmallIcon(R.drawable.ic_notify_app)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(
                    NOTIFICATION_ID2_ACTIVITY,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
                )
            } else {
                service.startForeground(NOTIFICATION_ID2_ACTIVITY, notification)
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to start keepalive notification", e)
        }
        Log.v(Const.TAG, "+++ startForeground starting.  ID=$NOTIFICATION_ID2_ACTIVITY")
    }

    /**
     * 全画面インテント開くための通知を作成して、サービスを開始する.
     */
    fun startFullScreenConfirmationNotification(
        service: Service,
        intent: Intent,
    ) {
        val pendingIntent = PendingIntent.getActivity(
            service,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        @Suppress("FullScreenIntentPolicy")
        val notification = NotificationCompat.Builder(service, CHANNEL_ID1_FSINTENT)
            .setSmallIcon(R.drawable.ic_notify_app)
            .setContentTitle(service.getString(R.string.app_name))
            .setContentText(service.getString(R.string.confirm_title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true) // フルスクリーンインテント
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ロック画面にも表示する.
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(
                    NOTIFICATION_ID1_FSINTENT,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
                )
            } else {
                service.startForeground(NOTIFICATION_ID1_FSINTENT, notification)
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to start keepalive notification", e)
        }
        Log.v(Const.TAG, "+++ startForeground starting.  ID=$NOTIFICATION_ID1_FSINTENT")
    }

    /**
     * プロセス優先度維持用の常駐フォアグラウンド用通知を作成して、サービスを開始する.
     */
    fun startKeepAliveNotification(service: Service) {
        val mySelf = service.packageManager.getLaunchIntentForPackage(service.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = mySelf?.let { intent ->
            PendingIntent.getActivity(
                service,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(service, CHANNEL_ID5_KEEPALIVE)
            .setSmallIcon(R.drawable.ic_notify_app)
            .setContentTitle(service.getString(R.string.app_name))
            .setContentText(service.getString(R.string.notification_keepalive_compact_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(service.getString(R.string.notification_keepalive_big_text))
                    .setBigContentTitle(service.getString(R.string.notification_keepalive_bigcontent_text)),
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                pendingIntent?.let { setContentIntent(it) }
            }
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(
                    NOTIFICATION_ID5_KEEPALIVE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                service.startForeground(NOTIFICATION_ID5_KEEPALIVE, notification)
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Failed to start keepalive notification", e)
        }
        Log.v(Const.TAG, "+++ startKeepAliveNotification started. ID=$NOTIFICATION_ID5_KEEPALIVE")
    }
}
