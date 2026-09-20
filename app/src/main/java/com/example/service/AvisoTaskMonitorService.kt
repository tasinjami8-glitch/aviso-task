package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.webkit.CookieManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.util.AvisoNotificationHelper
import com.example.util.AvisoTaskParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AvisoTaskMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var monitorJob: Job? = null
    private var lastKnownTaskCount: Int = -1
    private var checkIntervalMs: Long = 120_000L // 2 minutes default
    private var wakeLock: PowerManager.WakeLock? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_CHECK_NOW = "com.example.service.ACTION_CHECK_NOW"
        const val EXTRA_INTERVAL_MIN = "EXTRA_INTERVAL_MIN"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var lastCheckedTimeText: String = "এখনো চেক করা হয়নি"
            private set

        @Volatile
        var lastDetectedCount: Int = 0
            private set

        fun start(context: Context, intervalMinutes: Int = 2) {
            try {
                val intent = Intent(context, AvisoTaskMonitorService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_INTERVAL_MIN, intervalMinutes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Prevent crash if background start is restricted by Android OS
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AvisoTaskMonitorService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                // Prevent crash if stopping service is not allowed
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AvisoNotificationHelper.createNotificationChannels(this)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Aviso:BackgroundMonitorWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Exception) { }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        val intervalMin = intent?.getIntExtra(EXTRA_INTERVAL_MIN, 2) ?: 2
        checkIntervalMs = (intervalMin * 60 * 1000L).coerceAtLeast(30_000L)

        startForegroundNotification("Aviso ব্যাকগ্রাউন্ড মনিটরিং সক্রিয়")
        isServiceRunning = true

        try {
            wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max
        } catch (_: Exception) { }

        startMonitoringLoop()

        return START_STICKY
    }

    private fun startForegroundNotification(statusText: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_URL", "https://aviso.bz/tasks-youtube")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AvisoTaskMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, AvisoNotificationHelper.CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aviso YouTube Task Monitor")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .addAction(0, "বন্ধ করুন", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AvisoNotificationHelper.NOTIFICATION_ID_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(AvisoNotificationHelper.NOTIFICATION_ID_SERVICE, notification)
        }
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                checkAvisoTasks()
                delay(checkIntervalMs)
            }
        }
    }

    private suspend fun checkAvisoTasks() {
        try {
            val url = "https://aviso.bz/tasks-youtube"
            // Fetch cookies from system WebView CookieManager
            val cookies = withContext(Dispatchers.Main) {
                CookieManager.getInstance().getCookie(url) ?: ""
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru,en-US;q=0.9,en;q=0.8,bn;q=0.7")

            if (cookies.isNotBlank()) {
                requestBuilder.header("Cookie", cookies)
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val result = AvisoTaskParser.parseHtmlBackground(html)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val nowStr = timeFormat.format(Date())
                lastCheckedTimeText = "$nowStr (${result.totalTasks} টি কাজ)"
                lastDetectedCount = result.totalTasks

                if (result.totalTasks > 0) {
                    val isNewer = lastKnownTaskCount >= 0 && result.totalTasks > lastKnownTaskCount
                    val isFirstFind = lastKnownTaskCount == 0 && result.totalTasks > 0

                    if (isNewer || isFirstFind) {
                        val details = buildString {
                            append("নতুন কাজ পাওয়া গেছে! মোট: ${result.totalTasks} টি")
                            if (result.watchCount > 0) append("\n• ভিডিও দেখা: ${result.watchCount} টি")
                            if (result.subscribeCount > 0) append("\n• চ্যানেল সাবস্ক্রাইব: ${result.subscribeCount} টি")
                        }
                        AvisoNotificationHelper.sendTaskNotification(
                            this@AvisoTaskMonitorService,
                            result.totalTasks,
                            details,
                            isNewDetected = true
                        )
                    }
                    lastKnownTaskCount = result.totalTasks
                } else {
                    if (lastKnownTaskCount > 0) {
                        // All tasks completed / finished
                        AvisoNotificationHelper.sendNoTasksNotification(
                            this@AvisoTaskMonitorService,
                            wasCompleted = true
                        )
                    } else if (lastKnownTaskCount == -1) {
                        // First check and no tasks found
                        AvisoNotificationHelper.sendNoTasksNotification(
                            this@AvisoTaskMonitorService,
                            wasCompleted = false
                        )
                    }
                    lastKnownTaskCount = 0
                }

                startForegroundNotification("সর্বশেষ চেক: $nowStr | মোট কাজ: ${result.totalTasks} টি")
            }
        } catch (e: Exception) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            lastCheckedTimeText = "${timeFormat.format(Date())} (ত্রুটি: সার্ভার প্রতিক্রিয়া নেই)"
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        isServiceRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) { }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }
}
