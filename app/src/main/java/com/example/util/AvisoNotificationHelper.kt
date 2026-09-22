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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AvisoNotificationHelper {
    const val CHANNEL_TASKS_ID = "aviso_tasks_alert_channel_v2"
    const val CHANNEL_TASKS_NAME = "নতুন ইউটিউব কাজ নোটিফিকেশন"

    const val CHANNEL_TASKS_SILENT_ID = "aviso_tasks_silent_channel_v2"
    const val CHANNEL_TASKS_SILENT_NAME = "ইউটিউব কাজ সাইলেন্ট নোটিফিকেশন"

    const val CHANNEL_SERVICE_ID = "aviso_bg_service_channel"
    const val CHANNEL_SERVICE_NAME = "Aviso ব্যাকগ্রাউন্ড মনিটরিং"

    private const val NOTIFICATION_ID_TASK = 1001
    const val NOTIFICATION_ID_SERVICE = 1002
    private const val NOTIFICATION_ID_TASK_FINISH = 1003

    private const val PREFS_NAME = "aviso_notification_preferences"
    private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
    private const val KEY_SOUND_ENABLED = "key_sound_enabled"
    private const val KEY_NOTIFY_TASK_FINISH = "key_notify_task_finish"

    fun isNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isNotifyTaskFinishEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFY_TASK_FINISH, true)
    }

    fun setNotifyTaskFinishEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFY_TASK_FINISH, enabled).apply()
    }

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

            // Silent channel for notifications without sound or vibration
            val silentChannel = NotificationChannel(
                CHANNEL_TASKS_SILENT_ID,
                CHANNEL_TASKS_SILENT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "শব্দহীনভাবে ইউটিউব কাজের নোটিফিকেশন দেয়"
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(silentChannel)

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
        if (!isNotificationsEnabled(context)) return
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val soundEnabled = isSoundEnabled(context)
        val channelId = if (soundEnabled) CHANNEL_TASKS_ID else CHANNEL_TASKS_SILENT_ID

        if (soundEnabled) {
            // Wake screen briefly so user sees the notification alert
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Aviso:NotificationWakeLock"
                )
                wakeLock?.acquire(3000L)
            } catch (_: Exception) { }
        }

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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundEnabled) {
            notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 400, 200, 400))
        } else {
            notificationBuilder
                .setSound(null)
                .setVibrate(null)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_TASK, notificationBuilder.build())
        } catch (e: SecurityException) {
            // Permission revoked
        }

        if (soundEnabled) {
            // Active sound fallback: guarantees sound plays even in emulator or non-standard ROMs
            playAlertSound(context)
        }
    }

    fun sendNoTasksNotification(
        context: Context,
        wasCompleted: Boolean = true
    ) {
        if (!isNotificationsEnabled(context)) return
        if (!isNotifyTaskFinishEnabled(context)) return
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val soundEnabled = isSoundEnabled(context)
        val channelId = if (soundEnabled) CHANNEL_TASKS_ID else CHANNEL_TASKS_SILENT_ID

        if (soundEnabled) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Aviso:NotificationWakeLock"
                )
                wakeLock?.acquire(3000L)
            } catch (_: Exception) { }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_URL", "https://aviso.bz/tasks-youtube")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (wasCompleted) {
            "✅ সব YouTube কাজ শেষ হয়েছে!"
        } else {
            "ℹ️ বর্তমানে কোনো YouTube কাজ নেই"
        }

        val message = if (wasCompleted) {
            "সবগুলো YouTube কাজ সফলভাবে সম্পন্ন হয়েছে। নতুন কোনো কাজ আসলে আপনাকে সাথে সাথে জানানো হবে।"
        } else {
            "বর্তমানে কোনো নতুন YouTube কাজ অবশিষ্ট নেই। ব্যাকগ্রাউন্ডে চেক চলমান রয়েছে।"
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundEnabled) {
            notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 300, 150, 300))
        } else {
            notificationBuilder
                .setSound(null)
                .setVibrate(null)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_TASK_FINISH, notificationBuilder.build())
        } catch (e: SecurityException) {
            // Permission revoked
        }

        if (soundEnabled) {
            playAlertSound(context)
        }
    }

    fun playAlertSound(context: Context) {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
            ringtone?.play()
        } catch (_: Exception) { }
    }

    /**
     * Plays loud alarm sound continuously for 5 seconds when Captcha is detected.
     */
    fun playCaptchaAlertSound(context: Context, durationMs: Long = 5000L) {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
            ringtone?.play()

            // Vibrate pattern for alert
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 400, 200, 400, 200, 400, 200, 400, 200, 400),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            } catch (_: Exception) {}

            // Stop ringtone precisely after durationMs (5 seconds)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (ringtone?.isPlaying == true) {
                        ringtone.stop()
                    }
                } catch (_: Exception) {}
            }, durationMs)
        } catch (_: Exception) { }
    }

    fun sendTestNotification(context: Context) {
        if (!isNotificationsEnabled(context)) {
            android.widget.Toast.makeText(
                context,
                "নোটিফিকেশন অপশন বন্ধ আছে। অনুগ্রহ করে প্রথমে নোটিফিকেশন চালু করুন।",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val soundStatus = if (isSoundEnabled(context)) "সাউন্ড সহ" else "সাউন্ড ছাড়া (সাইলেন্ট)"
        sendTaskNotification(
            context,
            5,
            "টেস্ট নোটিফিকেশন ($soundStatus): নোটিফিকেশন সফলভাবে পরীক্ষা করা হয়েছে! নতুন কাজ আসলে ঠিক এমন নোটিফিকেশন আসবে।",
            isNewDetected = true
        )
    }

    private const val NOTIFICATION_ID_AUTOWORK_PROGRESS = 1004

    /**
     * Shows a live countdown progress notification while Auto Work is viewing a video.
     * Enables user to leave app or view YouTube app while keeping track of the countdown.
     */
    fun showAutoWorkProgressNotification(
        context: Context,
        remainingSec: Int,
        totalSec: Int,
        isPaused: Boolean = false,
        taskTitle: String = ""
    ) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                11,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progressPercent = if (totalSec > 0) ((totalSec - remainingSec) * 100 / totalSec).coerceIn(0, 100) else 0

            val titleText = if (isPaused) {
                "⏸️ অটো কাজ বিরতি (Paused): ${remainingSec}s বাকি"
            } else {
                "🎬 YouTube এ ভিডিও চলছে: ${remainingSec}s বাকি"
            }

            val descText = if (isPaused) {
                "কাজের কাউন্টডাউন সাময়িক বন্ধ আছে। পুনরায় চালু করতে অ্যাপে ফিরুন।"
            } else {
                "ভিডিও দেখার সময় শেষ হলে অ্যাপে ফিরে কনফার্ম বাটন হাইলাইট করা হবে।"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_SILENT_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titleText)
                .setContentText(descText)
                .setProgress(100, progressPercent, isPaused)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_AUTOWORK_PROGRESS, notification)
        } catch (_: Exception) {}
    }

    /**
     * Cancels the live countdown progress notification.
     */
    fun cancelAutoWorkProgressNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID_AUTOWORK_PROGRESS)
        } catch (_: Exception) {}
    }

    /**
     * Sends a completion notification with high priority & sound when video watching finishes.
     * Instructs user to manually confirm the view for the task.
     */
    fun sendAutoWorkFinishedNotification(context: Context, taskTitle: String = "") {
        cancelAutoWorkProgressNotification(context)
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                12,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎬 Viewing time completed!")
                .setContentText("Click Confirm view for this task. (কনফার্ম করতে বাটনে ক্লিক করুন)")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Viewing time completed. Click Confirm view for this task to finalize earnings."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_TASK_FINISH, notification)
        } catch (_: Exception) {}
    }
}
