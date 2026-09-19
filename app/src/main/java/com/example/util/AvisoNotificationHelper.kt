package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AvisoNotificationHelper {
    const val CHANNEL_TASKS_ID = "aviso_tasks_alert_channel_v2"
    const val CHANNEL_TASKS_NAME = "নতুন ইউটিউব কাজ নোটিফিকেশন"

    const val CHANNEL_SERVICE_ID = "aviso_bg_service_channel"
    const val CHANNEL_SERVICE_NAME = "Aviso ব্যাকগ্রাউন্ড মনিটরিং"

    private const val NOTIFICATION_ID_TASK = 1001
    const val NOTIFICATION_ID_SERVICE = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // Delete legacy channel without audio attributes if present
            try {
                notificationManager.deleteNotificationChannel("aviso_tasks_alert_channel")
            } catch (_: Exception) { }

            // High-importance channel with explicit Sound & Vibration
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                CHANNEL_TASKS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso.bz-তে নতুন YouTube কাজ আসলে সাথে সাথে নোটিফিকেশন দেয়"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(taskChannel)

            // Lower-priority channel for persistent background service
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                CHANNEL_SERVICE_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "অ্যাপ ব্যাকগ্রাউন্ডে কাজ চেক করার সময় সচল থাকে"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) { }
        }
    }

    fun sendTaskNotification(
        context: Context,
        taskCount: Int,
        message: String,
        isNewDetected: Boolean = true
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        // Wake screen briefly so user sees the notification alert
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Aviso:NotificationWakeLock"
            )
            wakeLock?.acquire(3000L)
        } catch (_: Exception) { }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_URL", "https://aviso.bz/tasks-youtube")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val title = if (isNewDetected) {
            "⚡ নতুন YouTube কাজ এসেছে! ($taskCount টি কাজ)"
        } else {
            "📢 YouTube কাজের আপডেট ($taskCount টি উপলব্ধ)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_TASK, notification)
        } catch (e: SecurityException) {
            // Permission revoked
        }

        // Active sound fallback: guarantees sound plays even in emulator or non-standard ROMs
        playAlertSound(context)
    }

    fun playAlertSound(context: Context) {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
            ringtone?.play()
        } catch (_: Exception) { }
    }

    fun sendTestNotification(context: Context) {
        sendTaskNotification(
            context,
            5,
            "টেস্ট নোটিফিকেশন: নোটিফিকেশন সাউন্ড ও অ্যালার্ট সফলভাবে পরীক্ষা করা হয়েছে! নতুন ইউটিউব কাজ পেলে ঠিক এমন সাউন্ড ও নোটিফিকেশন আসবে।",
            isNewDetected = true
        )
    }
}
