package com.example.myapplication.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

object NotificationHelper {

    private const val CHANNEL_ID_PERSISTENT = "budget_persistent"
    private const val CHANNEL_ID_ALERT = "budget_alert"

    const val NOTIFICATION_ID_PERSISTENT = 1001
    const val NOTIFICATION_ID_ALERT = 1002

    // 创建通知渠道
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 常驻通知渠道
            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                context.getString(R.string.notif_channel_persistent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_persistent_desc)
                setShowBadge(false)
            }

            // 警告通知渠道
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                context.getString(R.string.notif_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alert_desc)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(persistentChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    // 显示常驻通知
    // 显示常驻通知（增强版 - 带快速操作）
    fun showPersistentNotification(
        context: Context,
        monthlyTotal: Double,
        monthlyBudget: Double,
        pinnedTemplates: List<com.example.myapplication.data.ExpenseTemplate> = emptyList()
    ) {
        // 检查权限
        if (!hasNotificationPermission(context)) {
            return
        }
        val percentage = (monthlyTotal / monthlyBudget * 100).coerceIn(0.0, 100.0)
        val remaining = monthlyBudget - monthlyTotal

        // 打开应用的 Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 快速记账 Intent（打开记账页面）
        val quickRecordIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "record")
        }

        val quickRecordPendingIntent = PendingIntent.getActivity(
            context,
            1,
            quickRecordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                context.getString(R.string.notif_persistent_title, percentage.toInt())
            )
            .setContentText(
                if (remaining >= 0) {
                    context.getString(
                        R.string.notif_persistent_used,
                        String.format("%.2f", monthlyTotal),
                        String.format("%.2f", monthlyBudget)
                    )
                } else {
                    context.getString(
                        R.string.notif_persistent_over,
                        String.format("%.2f", -remaining)
                    )
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setProgress(100, percentage.toInt(), false)
            // 添加快速记账按钮
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.notif_quick_record),
                quickRecordPendingIntent
            )

        // 如果有置顶模板，添加快捷记账按钮（最多2个）
        pinnedTemplates.take(2).forEachIndexed { index, template ->
            val templateIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("quick_template_id", template.id)
            }

            val templatePendingIntent = PendingIntent.getActivity(
                context,
                100 + index,
                templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.addAction(
                R.drawable.ic_launcher_foreground,
                "¥${template.amount.toInt()}",
                templatePendingIntent
            )
        }

        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_PERSISTENT,
                    notificationBuilder.build()
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    // 取消常驻通知
    fun cancelPersistentNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PERSISTENT)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // 显示预算警告通知
    fun showBudgetAlertNotification(
        context: Context,
        monthlyTotal: Double,
        monthlyBudget: Double,
        isOverBudget: Boolean
    ) {
        // 检查权限
        if (!hasNotificationPermission(context)) {
            return
        }
        val percentage = (monthlyTotal / monthlyBudget * 100).toInt()

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = if (isOverBudget) {
            context.getString(R.string.notif_alert_over_title) to context.getString(
                R.string.notif_alert_over_text,
                String.format("%.2f", monthlyTotal - monthlyBudget)
            )
        } else {
            context.getString(R.string.notif_alert_usage_title) to context.getString(
                R.string.notif_alert_usage_text,
                percentage,
                String.format("%.2f", monthlyBudget - monthlyTotal)
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_ALERT,
                    notification
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // 检查通知权限
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }
}