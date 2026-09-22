package com.example.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.AvisoPermissionHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.model.AutomationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.widget.Toast
import com.example.model.BrowserTab
import com.example.util.AvisoNotificationHelper
import com.example.util.AvisoTaskParser
import com.example.viewmodel.AvisoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private var lastNativeTapTime = 0L

private fun simulateTap(webView: WebView?, x: Float, y: Float) {
    if (webView == null || x <= 0f || y <= 0f) return
    val now = SystemClock.uptimeMillis()
    if (now - lastNativeTapTime < 300L) return
    lastNativeTapTime = now
    try {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(downTime, eventTime + 40, MotionEvent.ACTION_UP, x, y, 0)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    } catch (e: Exception) {
        // ignore tap errors
    }
}

private fun launchYouTubeApp(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)
        val ytIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(ytIntent)
    } catch (_: Exception) {
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
        } catch (_: Exception) {}
    }
}

private fun bringAppToFront(context: Context) {
    try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Aviso:BringToFrontWakeLock"
        )
        wakeLock?.acquire(3000L)
    } catch (_: Exception) {}

    try {
        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            pendingIntent.send()
        } catch (_: Exception) {
            context.startActivity(intent)
        }
    } catch (_: Exception) {}
}

class AvisoBridge(
    private val onResult: (String) -> Unit,
    private val onCaptchaFound: (String) -> Unit = {},
    private val onTaskFound: (String, Int) -> Unit = { _, _ -> },
    private val onTaskStarted: (Int) -> Unit = {},
    private val onNoTasks: () -> Unit = {},
    private val onConfirmClicked: (Boolean) -> Unit = {},
    private val onVideoPositionFound: (Float, Float) -> Unit = { _, _ -> },
    private val onRealTimerUpdate: (Int) -> Unit = {},
    private val onTaskCompleted: () -> Unit = {},
    private val onInterstitialHandled: (Int) -> Unit = {},
    private val onOpenYouTube: (String) -> Unit = {},
    private val onOpenNewTab: (String, Int) -> Unit = { _, _ -> },
    private val onAutomationLog: (String) -> Unit = {},
    private val onTaskSessionLocked: (String, String, Int, String) -> Unit = { _, _, _, _ -> },
    private val onWrongPageDetected: (String, String) -> Unit = { _, _ -> },
    private val onTaskMismatchDetected: (String, String) -> Unit = { _, _ -> },
    private val onTaskRestored: (String) -> Unit = {}
) {
    @JavascriptInterface
    fun onAutomationLog(message: String) {
        onAutomationLog.invoke(message)
    }

    @JavascriptInterface
    fun onTaskSessionLocked(taskId: String, taskTitle: String, durationSec: Int, containerId: String) {
        onTaskSessionLocked.invoke(taskId, taskTitle, durationSec, containerId)
    }

    @JavascriptInterface
    fun onWrongPageDetected(reason: String, url: String) {
        onWrongPageDetected.invoke(reason, url)
    }

    @JavascriptInterface
    fun onTaskMismatchDetected(expectedId: String, foundId: String) {
        onTaskMismatchDetected.invoke(expectedId, foundId)
    }

    @JavascriptInterface
    fun onTaskRestored(taskId: String) {
        onTaskRestored.invoke(taskId)
    }

    @JavascriptInterface
    fun onTasksScanned(json: String) {
        onResult(json)
    }

    @JavascriptInterface
    fun openInYouTubeApp(url: String) {
        onOpenYouTube.invoke(url)
    }

    @JavascriptInterface
    fun openNewTab(url: String, durationSec: Int) {
        onOpenNewTab.invoke(url, durationSec)
    }

    @JavascriptInterface
    fun onCaptchaFound(reason: String) {
        onCaptchaFound.invoke(reason)
    }

    @JavascriptInterface
    fun onAutoWorkTaskFound(title: String, durationSec: Int) {
        onTaskFound.invoke(title, durationSec)
    }

    @JavascriptInterface
    fun onAutoWorkTaskStarted(durationSec: Int) {
        onTaskStarted.invoke(durationSec)
    }

    @JavascriptInterface
    fun onAutoWorkNoTasks() {
        onNoTasks.invoke()
    }

    @JavascriptInterface
    fun onAutoWorkConfirmClicked(clicked: Boolean) {
        onConfirmClicked.invoke(clicked)
    }

    @JavascriptInterface
    fun onVideoPositionFound(x: Float, y: Float) {
        onVideoPositionFound.invoke(x, y)
    }

    @JavascriptInterface
    fun onRealTimerUpdate(secondsLeft: Int) {
        onRealTimerUpdate.invoke(secondsLeft)
    }

    @JavascriptInterface
    fun onTaskCompleted() {
        onTaskCompleted.invoke()
    }

    @JavascriptInterface
    fun onInterstitialHandled(durationSec: Int) {
        onInterstitialHandled.invoke(durationSec)
    }

    @JavascriptInterface
    fun onError(err: String) {
        // Log or handle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AvisoBrowserScreen(
    viewModel: AvisoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var secondaryWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // Synchronize service state and notification preferences on launch
    LaunchedEffect(Unit) {
        viewModel.syncServiceState()
        viewModel.loadNotificationPreferences(context)
    }

    // Connect WebView to Android lifecycle to properly pause/resume audio & js threads
    // IMPORTANT: When isAutoWorkRunning is true, NEVER pause timers so countdown and execution continue
    DisposableEffect(lifecycleOwner, webViewRef, uiState.isAutoWorkRunning) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (!uiState.isAutoWorkRunning) {
                        webViewRef?.pauseTimers()
                        webViewRef?.onPause()
                    }
                }
                else -> Unit
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Handle back button: if sidebar open, close it; else navigate webview back if possible
    BackHandler {
        if (uiState.isSidebarOpen) {
            viewModel.closeSidebar()
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        }
    }

    // Reload trigger effect
    LaunchedEffect(uiState.reloadTrigger) {
        if (uiState.reloadTrigger > 0) {
            webViewRef?.reload()
        }
    }

    // Apply Zoom (Scale & TextZoom) dynamically to WebView
    LaunchedEffect(uiState.zoomPercent) {
        webViewRef?.let { wv ->
            wv.settings.textZoom = uiState.zoomPercent
            val zoomScale = uiState.zoomPercent / 100f
            wv.evaluateJavascript("document.body.style.zoom = '$zoomScale';", null)
        }
    }

    // Apply Desktop / Mobile User Agent
    LaunchedEffect(uiState.isDesktopMode) {
        webViewRef?.let { wv ->
            val userAgent = if (uiState.isDesktopMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            } else {
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            }
            wv.settings.userAgentString = userAgent
            wv.settings.useWideViewPort = true
            wv.settings.loadWithOverviewMode = true
            wv.reload()
        }
    }

    // When Video Tab URL changes or is opened, load in secondary video webview
    var lastLoadedVideoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isVideoTabOpen) {
        if (!uiState.isVideoTabOpen) {
            lastLoadedVideoUrl = null
        }
    }

    LaunchedEffect(uiState.videoTabUrl) {
        val url = uiState.videoTabUrl
        if (!url.isNullOrBlank() && url != lastLoadedVideoUrl) {
            lastLoadedVideoUrl = url
            secondaryWebViewRef?.loadUrl(url)
        }
    }

    // Periodic task scan while active (only when auto-work is not busy clicking)
    LaunchedEffect(Unit) {
        while (true) {
            delay(12_000L)
            if (!uiState.isAutoWorkRunning) {
                webViewRef?.evaluateJavascript(AvisoTaskParser.JS_READER_CODE, null)
            }
        }
    }

    var autoWorkTaskStartedSignal by remember { mutableStateOf<Int?>(null) }
    var autoWorkNoTasksSignal by remember { mutableStateOf(false) }
    var realTimerSecondsSignal by remember { mutableStateOf(-1) }
    var taskCompletedSignal by remember { mutableStateOf(false) }
    var confirmClickedSignal by remember { mutableStateOf(false) }
    var didReloadForTasks by remember { mutableStateOf(false) }
    var realInterstitialDurationSignal by remember { mutableStateOf<Int?>(null) }
    var manualWatchCountdown by remember { mutableStateOf<Int?>(null) }
    var manualWatchTotal by remember { mutableStateOf<Int?>(null) }
    var showOverlayPromptDialog by remember { mutableStateOf(false) }
    var didDismissOverlayPrompt by remember { mutableStateOf(false) }
    var showTabsOverviewDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    // Manual watching countdown loop (when user clicks blue link manually and interstitial appears)
    LaunchedEffect(manualWatchTotal, uiState.isAutoWorkRunning) {
        val total = manualWatchTotal ?: return@LaunchedEffect
        if (uiState.isAutoWorkRunning) return@LaunchedEffect

        val fullDurationWithBuffer = total + 2 // +2 seconds extra wait as requested
        var rem = fullDurationWithBuffer
        AvisoNotificationHelper.showAutoWorkProgressNotification(context, rem, fullDurationWithBuffer)
        while (rem > 0 && !uiState.isAutoWorkRunning) {
            manualWatchCountdown = rem
            AvisoNotificationHelper.showAutoWorkProgressNotification(context, rem, fullDurationWithBuffer)
            // Ensure video playback is triggered and unmuted
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
            secondaryWebViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
            delay(1000L)
            rem--
        }
        manualWatchCountdown = 0
        AvisoNotificationHelper.cancelAutoWorkProgressNotification(context)
        bringAppToFront(context)
        AvisoNotificationHelper.sendAutoWorkFinishedNotification(context)
        if (!uiState.isAutoWorkRunning) {
            delay(1000L)
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_HIGHLIGHT_CONFIRM_BUTTON, null)
            secondaryWebViewRef?.evaluateJavascript(AvisoTaskParser.JS_HIGHLIGHT_CONFIRM_BUTTON, null)
            Toast.makeText(context, "Viewing time completed. Click Confirm view for this task.", Toast.LENGTH_LONG).show()
        }
        delay(2000L)
        manualWatchTotal = null
        manualWatchCountdown = null
    }

    // Secondary Video Tab Countdown & Auto-Close Engine for Manual Mode
    LaunchedEffect(uiState.isVideoTabOpen, uiState.videoTabUrl, uiState.videoTabDuration, uiState.isAutoWorkRunning) {
        if (!uiState.isVideoTabOpen || uiState.videoTabUrl.isNullOrEmpty() || uiState.isAutoWorkRunning) {
            return@LaunchedEffect
        }
        var taskSec = (uiState.videoTabDuration.takeIf { it > 0 } ?: realInterstitialDurationSignal ?: autoWorkTaskStartedSignal ?: 20).coerceAtLeast(5)
        var totalSec = taskSec + 1 // +1 second extra wait buffer as requested
        var remaining = totalSec
        taskCompletedSignal = false
        confirmClickedSignal = false
        realTimerSecondsSignal = -1
        viewModel.updateVideoTabCountdown(remaining, totalSec)

        while (isActive && uiState.isVideoTabOpen && remaining > 0 && !uiState.isAutoWorkRunning) {
            // Check if page interstitial or bridge reported a higher duration
            if (realInterstitialDurationSignal != null && realInterstitialDurationSignal!! > taskSec) {
                val newDur = realInterstitialDurationSignal!!
                val diff = newDur - taskSec
                remaining += diff
                taskSec = newDur
                totalSec = newDur + 1
            }

            viewModel.updateVideoTabCountdown(remaining, totalSec)
            AvisoNotificationHelper.showAutoWorkProgressNotification(context, remaining, totalSec)

            // Keep video playing and unmuted
            secondaryWebViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)

            delay(1000L)
            remaining--
        }

        if (uiState.isVideoTabOpen && !uiState.isAutoWorkRunning) {
            // Video countdown finished! Bring app to front
            bringAppToFront(context)
            AvisoNotificationHelper.sendAutoWorkFinishedNotification(context)

            // Step 1: Return to original Aviso tab (Tab 1) and close Video Tab
            viewModel.closeVideoTab()
            viewModel.selectTab(0)

            // Step 2: Wait for Tab 1 to regain focus and highlight Confirm View control without clicking
            delay(1200L)
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_HIGHLIGHT_CONFIRM_BUTTON, null)

            Toast.makeText(context, "Viewing time completed. Click Confirm view for this task.", Toast.LENGTH_LONG).show()
        }
    }

    // Active Standard Tab Switch Listener
    LaunchedEffect(uiState.activeTabId) {
        val activeTab = uiState.tabs.find { it.id == uiState.activeTabId }
        if (activeTab != null && !activeTab.isVideoTab) {
            val currentLoadedUrl = webViewRef?.url ?: ""
            if (activeTab.url.isNotEmpty() && currentLoadedUrl.isNotEmpty() && currentLoadedUrl != activeTab.url) {
                webViewRef?.loadUrl(activeTab.url)
            }
        }
    }

    // Auto Work Execution Engine (Strict Task-Only Automation with Safe Page Lock & Auto-Confirm)
    LaunchedEffect(uiState.isAutoWorkRunning) {
        if (!uiState.isAutoWorkRunning) {
            AvisoNotificationHelper.cancelAutoWorkProgressNotification(context)
            viewModel.setAutomationState(AutomationState.IDLE)
            return@LaunchedEffect
        }

        // Ensure we are on YouTube tasks page in Tab 1
        if (uiState.isVideoTabOpen) {
            viewModel.closeVideoTab()
            viewModel.selectTab(0)
            delay(500L)
        }

        if (webViewRef?.url?.contains("tasks-youtube") != true) {
            viewModel.setAutomationState(AutomationState.TASK_DISCOVERED)
            viewModel.setAutoWorkStatus("ইউটিউব টাস্ক পেজে যাওয়া হচ্ছে...")
            viewModel.addAutomationLog("[Browser] Navigating to https://aviso.bz/tasks-youtube")
            webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
            delay(3500L)
        }

        while (isActive && uiState.isAutoWorkRunning) {
            // Handle Paused state gracefully
            while (isActive && uiState.isAutoWorkRunning && uiState.isAutoWorkPaused) {
                delay(500L)
            }
            if (!uiState.isAutoWorkRunning) break

            autoWorkTaskStartedSignal = null
            autoWorkNoTasksSignal = false
            realTimerSecondsSignal = -1
            taskCompletedSignal = false
            confirmClickedSignal = false
            realInterstitialDurationSignal = null

            // Step 0: Pre-Scan Wrong Page Safety Verification
            val curUrl = webViewRef?.url.orEmpty()
            if (curUrl.isNotEmpty() && !curUrl.contains("tasks-youtube") && !curUrl.contains("/vl/") && !curUrl.contains("/go/") && !curUrl.contains("youtube")) {
                viewModel.reportWrongPage("NOT_ON_TASK_PAGE", curUrl)
                viewModel.addAutomationLog("[Protection] Pre-check: Wrong page ($curUrl). Triggering safe recovery.")
                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                delay(3500L)
                viewModel.reportSafeRecovery()
            }

            // Step 1: Scan & Find ONE task only
            viewModel.setAutomationState(AutomationState.TASK_FOUND)
            viewModel.setAutoWorkStatus("টাস্ক খোঁজা হচ্ছে (Step 1: One Task Only)...")
            viewModel.addAutomationLog("[Task] Step 1: Scanning for single active task session")
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_FIND_AND_CLICK, null)

            // Step 2 & 3: Read viewing time & copy exact video link
            viewModel.setAutomationState(AutomationState.READ_VIEW_TIME)
            viewModel.setAutoWorkStatus("টাস্কের নির্ধারিত সময় ও ভিডিও লিংক রিড করা হচ্ছে...")

            // Wait for task detection (up to 4 seconds)
            var waited = 0
            while (autoWorkTaskStartedSignal == null && !confirmClickedSignal && !uiState.isVideoTabOpen && !autoWorkNoTasksSignal && waited < 40 && uiState.isAutoWorkRunning) {
                delay(100L)
                waited++
                val cur = webViewRef?.url.orEmpty()
                if (!cur.contains("tasks-youtube") || cur.contains("/go/") || cur.contains("/vl/") || cur.contains("youtube") || cur.contains("create_session") || uiState.isVideoTabOpen) {
                    break
                }
            }

            if (!uiState.isAutoWorkRunning) break

            // If no tasks are currently detected on page, attempt one reload to see if new tasks appeared
            if (autoWorkNoTasksSignal && !uiState.isVideoTabOpen) {
                if (!didReloadForTasks) {
                    didReloadForTasks = true
                    viewModel.setAutomationState(AutomationState.REFRESH_ONCE)
                    viewModel.setAutoWorkStatus("নতুন কাজ চেক করতে পেজ রিফ্রেশ হচ্ছে...")
                    viewModel.addAutomationLog("[Task] No immediate tasks, reloading tasks-youtube")
                    webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                    delay(3500L)
                    continue
                } else {
                    viewModel.setAutomationState(AutomationState.TASK_LIST_EXHAUSTED)
                    viewModel.setAutoWorkStatus("বর্তমানে কোনো কাজ নেই!")
                    viewModel.addAutomationLog("[Task] All available tasks completed / list exhausted")
                    AvisoNotificationHelper.sendNoTasksNotification(context, wasCompleted = true)
                    viewModel.stopAutoWork("সব কাজ শেষ হয়েছে!")
                    Toast.makeText(context, "সবগুলো YouTube কাজ সম্পন্ন হয়েছে!", Toast.LENGTH_LONG).show()
                    break
                }
            }
            didReloadForTasks = false

            // Check if task actually started
            val currentUrlAfterClick = webViewRef?.url.orEmpty()
            val hasNavigatedAway = !currentUrlAfterClick.contains("tasks-youtube") ||
                    currentUrlAfterClick.contains("/go/") ||
                    currentUrlAfterClick.contains("/vl/") ||
                    currentUrlAfterClick.contains("youtube") ||
                    currentUrlAfterClick.contains("create_session")

            val isStarted = uiState.isVideoTabOpen ||
                    (autoWorkTaskStartedSignal != null) ||
                    hasNavigatedAway ||
                    (realInterstitialDurationSignal != null) ||
                    (realTimerSecondsSignal > 0)

            if (!isStarted) {
                viewModel.setAutomationState(AutomationState.REFRESH_ONCE)
                viewModel.setAutoWorkStatus("টাস্ক শুরু হয়নি, পেজ রিফ্রেশ করা হচ্ছে...")
                viewModel.addAutomationLog("[Task] No active video task started. Reloading task list to recover.")
                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                delay(3500L)
                continue
            }

            // Step 3 & 4: Copy exact video URL & Open Video
            viewModel.setAutomationState(AutomationState.COPY_EXACT_VIDEO_URL)
            viewModel.addAutomationLog("[Task] Step 3: Exact video link copied for current task")
            delay(300L)

            viewModel.setAutomationState(AutomationState.OPEN_VIDEO)
            viewModel.setAutoWorkStatus("ইউটিউব ভিডিও ওপেন করা হচ্ছে...")
            viewModel.addAutomationLog("[Task] Step 4: Opening verified YouTube video")

            // Step 4 Verification: Verify YouTube page & video loaded
            viewModel.setAutomationState(AutomationState.VERIFY_VIDEO)
            delay(1000L)

            // Step 5: Exact Second Counting dynamically based on current task's duration
            var taskDuration = (autoWorkTaskStartedSignal?.takeIf { it > 0 } ?: realInterstitialDurationSignal?.takeIf { it > 0 } ?: uiState.videoTabDuration.takeIf { it > 0 } ?: 15).coerceAtLeast(5)
            var totalDurationWithBuffer = taskDuration + 1 // +1 second buffer

            viewModel.setAutomationState(AutomationState.START_EXACT_TIMER)
            viewModel.setAutoWorkStatus("ভিডিও দেখা হচ্ছে ($totalDurationWithBuffer সেক)...")
            viewModel.addAutomationLog("[Timer] Step 5: Starting exact dynamic timer: ${totalDurationWithBuffer}s")

            // Step 6: Active Video Watching & Background Countdown Loop
            var remainingSec = totalDurationWithBuffer
            taskCompletedSignal = false
            confirmClickedSignal = false

            while (uiState.isAutoWorkRunning && remainingSec > 0) {
                // Pause support during viewing
                if (uiState.isAutoWorkPaused) {
                    viewModel.setAutomationState(AutomationState.PAUSED)
                    AvisoNotificationHelper.showAutoWorkProgressNotification(
                        context,
                        remainingSec,
                        totalDurationWithBuffer,
                        isPaused = true
                    )
                    while (uiState.isAutoWorkPaused && uiState.isAutoWorkRunning) {
                        delay(500L)
                    }
                    if (!uiState.isAutoWorkRunning) break
                    viewModel.setAutomationState(AutomationState.START_EXACT_TIMER)
                }

                // Update countdown display in UI for both Auto Work and Video Tab
                viewModel.updateAutoWorkCountdown(remainingSec, totalDurationWithBuffer)
                viewModel.updateVideoTabCountdown(remainingSec, totalDurationWithBuffer)

                // Show live background notification in Android status bar
                AvisoNotificationHelper.showAutoWorkProgressNotification(
                    context,
                    remainingSec,
                    totalDurationWithBuffer,
                    isPaused = false
                )

                // Continuously keep video playing
                secondaryWebViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)

                delay(1000L)
                remainingSec--
            }

            if (!uiState.isAutoWorkRunning) {
                AvisoNotificationHelper.cancelAutoWorkProgressNotification(context)
                break
            }
            viewModel.updateAutoWorkCountdown(0, totalDurationWithBuffer)
            viewModel.updateVideoTabCountdown(0, totalDurationWithBuffer)

            // Step 6 & 7: Timer Completion & Return to the SAME task
            viewModel.setAutomationState(AutomationState.VIEW_TIME_COMPLETED)
            viewModel.addAutomationLog("[Timer] Step 6: Exact viewing duration complete.")
            bringAppToFront(context)
            AvisoNotificationHelper.sendAutoWorkFinishedNotification(context)

            // Step 7: Return to the SAME Task & Verify Task ID
            viewModel.setAutomationState(AutomationState.RETURN_TO_SAME_TASK)
            if (uiState.isVideoTabOpen) {
                viewModel.setAutoWorkStatus("ভিডিও দেখা শেষ! মূল টাস্কে ফিরে যাওয়া হচ্ছে...")
                viewModel.closeVideoTab()
                viewModel.selectTab(0)
                delay(1500L)
            } else {
                delay(1000L)
            }

            viewModel.setAutomationState(AutomationState.VERIFY_TASK_ID)
            val returnUrl = webViewRef?.url.orEmpty()
            if (!returnUrl.contains("tasks-youtube") && !returnUrl.contains("aviso.bz")) {
                viewModel.reportWrongPage("TASK_ID_MISMATCH", returnUrl)
                viewModel.addAutomationLog("[Protection] Step 7: Returned to unexpected page ($returnUrl). Task ID mismatch. Recovering.")
                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                delay(3000L)
                viewModel.reportSafeRecovery()
            }

            // Step 8: Confirm View Remains PENDING for user
            viewModel.setAutomationState(AutomationState.CONFIRM_VIEW_PENDING)
            viewModel.setAutoWorkStatus("কনফার্ম ভিউ পেন্ডিং (User interaction required)")
            viewModel.addAutomationLog("[Task] Step 8: Highlighted Confirm View button. Left pending for user.")
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_HIGHLIGHT_CONFIRM_BUTTON, null)
            delay(1500L)

            // Step 9: Refresh only after current task is complete to scan fresh task list
            viewModel.setAutomationState(AutomationState.REFRESH_ONCE)
            viewModel.setAutoWorkStatus("পেজ রিফ্রেশ করে নতুন টাস্ক তালিকা আপডেট করা হচ্ছে...")
            viewModel.addAutomationLog("[Task] Step 9: Refreshing page to fetch updated task list.")
            webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
            delay(3500L)

            // Step 10: Scan fresh task list and repeat
            viewModel.setAutomationState(AutomationState.SCAN_FRESH_TASK_LIST)
            delay(1000L)
            viewModel.setAutomationState(AutomationState.NEXT_TASK)
            viewModel.addAutomationLog("[Task] Step 10: Ready for next task in sequence.")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.navigateTo("https://aviso.bz/tasks-youtube")
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFF0033)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Aviso Pro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "YouTube Tasks Automation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(
                                onClick = { webViewRef?.goBack() },
                                modifier = Modifier.testTag("nav_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পিছনে যান")
                            }
                        }
                    },
                    actions = {
                        // Premium TopBar Start / Stop Work Button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.isAutoWorkRunning) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                            border = BorderStroke(
                                1.5.dp,
                                if (uiState.isAutoWorkRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (uiState.isAutoWorkRunning) {
                                        viewModel.stopAutoWork("ব্যবহারকারী বন্ধ করেছেন")
                                    } else {
                                        if (!AvisoPermissionHelper.canDrawOverlays(context) && !didDismissOverlayPrompt) {
                                            showOverlayPromptDialog = true
                                        } else {
                                            viewModel.startAutoWork()
                                        }
                                    }
                                }
                                .testTag("topbar_auto_work_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isAutoWorkRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAutoWorkRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (uiState.isAutoWorkRunning) "Stop Work" else "Start Work",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isAutoWorkRunning) {
                                        if (uiState.isAutoWorkPaused) "Paused" else if (uiState.autoWorkCountdownSeconds > 0) "${uiState.autoWorkCountdownSeconds}s" else "Running"
                                    } else {
                                        "Start Work"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isAutoWorkRunning) Color(0xFFDC2626) else Color(0xFF047857)
                                )
                            }
                        }

                        // Pause / Resume Button during Active Auto Work
                        if (uiState.isAutoWorkRunning) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (uiState.isAutoWorkPaused) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (uiState.isAutoWorkPaused) Color(0xFFD97706) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (uiState.isAutoWorkPaused) viewModel.resumeAutoWork() else viewModel.pauseAutoWork()
                                    }
                                    .testTag("topbar_pause_resume_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAutoWorkPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (uiState.isAutoWorkPaused) "Resume" else "Pause",
                                        tint = if (uiState.isAutoWorkPaused) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (uiState.isAutoWorkPaused) "Resume" else "Pause",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isAutoWorkPaused) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        // Automation Logs Button
                        IconButton(
                            onClick = { showLogsDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("topbar_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = "Automation Logs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Tasks Count Badge
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (uiState.scanResult.totalTasks > 0) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (uiState.scanResult.totalTasks > 0) Color(0xFFBBF7D0) else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.openSidebar() }
                                .testTag("task_badge_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.scanResult.totalTasks > 0) Color(0xFF10B981) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${uiState.scanResult.totalTasks} কাজ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.scanResult.totalTasks > 0) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Home Button
                        IconButton(
                            onClick = {
                                viewModel.navigateTo("https://aviso.bz/tasks-youtube")
                                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                            },
                            modifier = Modifier.testTag("home_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "হোম")
                        }

                        // Chrome-style Tabs Switcher Button (showing count e.g. 2)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showTabsOverviewDialog = true }
                                .testTag("btn_tabs_overview")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${uiState.tabs.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Refresh Button
                        IconButton(
                            onClick = { viewModel.reloadPage() },
                            modifier = Modifier.testTag("reload_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ")
                        }

                        // Right Sidebar Toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleSidebar() }
                                .testTag("toggle_sidebar_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "সাইডবার",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.zoomPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                )

                // Loading Progress Bar
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        progress = { uiState.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }

                // Real Browser Multi-Tab Bar (Chrome style with '+' Add Tab button)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("browser_tab_bar_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scrollable List of Open Tabs
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(uiState.tabs, key = { it.id }) { tab ->
                                val isActive = tab.id == uiState.activeTabId
                                Surface(
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                                    color = if (isActive) {
                                        if (tab.isVideoTab) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
                                    } else {
                                        if (tab.isVideoTab) Color(0xFFFFEBEE).copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isActive) {
                                            if (tab.isVideoTab) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        }
                                    ),
                                    shadowElevation = if (isActive) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .widthIn(min = 100.dp, max = 160.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                        .clickable {
                                            viewModel.selectTabById(tab.id)
                                        }
                                        .testTag("tab_item_${tab.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (tab.isVideoTab) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444))
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = if (tab.remainingSec > 0) "🎬 ${tab.remainingSec}s" else "🎬 ভিডিও",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isActive) Color(0xFFF87171) else Color(0xFFDC2626),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = tab.title.ifEmpty { "Aviso.bz" },
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Close tab button (✕)
                                        if (uiState.tabs.size > 1 || tab.isVideoTab) {
                                            IconButton(
                                                onClick = {
                                                    if (tab.isVideoTab) {
                                                        viewModel.closeVideoTab()
                                                    } else {
                                                        viewModel.closeTabById(tab.id)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .testTag("close_tab_${tab.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "ট্যাব বন্ধ করুন",
                                                    tint = if (tab.isVideoTab) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Browser '+' Add New Tab Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val newId = viewModel.addNewTab("https://aviso.bz/tasks-youtube", "ট্যাব ${uiState.tabs.size + 1}")
                                    Toast.makeText(context, "নতুন ট্যাব খোলা হয়েছে (+)", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("btn_browser_add_tab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "নতুন ট্যাব যোগ করুন",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeTab = uiState.tabs.find { it.id == uiState.activeTabId }
            val isCurrentTabVideo = (activeTab?.isVideoTab == true || uiState.selectedTabIndex == 1) && uiState.isVideoTabOpen && !uiState.videoTabUrl.isNullOrEmpty()

            // Layer 1: Main Web Browser View (Aviso Tab) - ALWAYS in hierarchy, never destroyed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (!isCurrentTabVideo) 2f else 0f)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("aviso_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.WHITE)

                            // Cookie Setup for Aviso Session & Login
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // WebSettings optimization
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                allowFileAccess = true
                                allowContentAccess = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(true)
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                textZoom = uiState.zoomPercent
                                // Allow video playback to start without requiring manual physical user gesture
                                mediaPlaybackRequiresUserGesture = false
                            }

                            // Register Bridge for Full Site Task Detection & Auto Work
                            addJavascriptInterface(
                                AvisoBridge(
                                    onResult = { json ->
                                        post {
                                            viewModel.onScanResultReceived(ctx, json)
                                        }
                                    },
                                    onCaptchaFound = { reason ->
                                        post {
                                            viewModel.onCaptchaDetected(ctx, reason)
                                        }
                                    },
                                    onTaskFound = { title, durationSec ->
                                        post {
                                            viewModel.setAutoWorkStatus("কাজ পাওয়া গেছে ($durationSec সেক)", title)
                                        }
                                    },
                                    onTaskStarted = { durationSec ->
                                        post {
                                            autoWorkTaskStartedSignal = durationSec
                                        }
                                    },
                                    onNoTasks = {
                                        post {
                                            autoWorkNoTasksSignal = true
                                        }
                                    },
                                    onConfirmClicked = { clicked ->
                                        post {
                                            if (clicked) {
                                                confirmClickedSignal = true
                                                viewModel.setAutoWorkStatus("Подтвердить просмотр সফল হয়েছে")
                                            }
                                        }
                                    },
                                    onVideoPositionFound = { _, _ ->
                                        // Player iframe is controlled cleanly via JS postMessage
                                    },
                                    onRealTimerUpdate = { secondsLeft ->
                                        post {
                                            realTimerSecondsSignal = secondsLeft
                                        }
                                    },
                                    onTaskCompleted = {
                                        post {
                                            taskCompletedSignal = true
                                        }
                                    },
                                    onInterstitialHandled = { durationSec ->
                                        post {
                                            realInterstitialDurationSignal = durationSec
                                            if (uiState.isAutoWorkRunning) {
                                                autoWorkTaskStartedSignal = durationSec
                                                viewModel.setAutoWorkStatus("Start Watching সফল! $durationSec সেক...")
                                            } else {
                                                manualWatchTotal = durationSec
                                                manualWatchCountdown = durationSec
                                                Toast.makeText(ctx, "Start Watching ক্লিক হয়েছে! $durationSec সেকেন্ড দেখা হচ্ছে...", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onOpenYouTube = { url ->
                                        post {
                                            if (uiState.openInExternalYouTubeApp) {
                                                launchYouTubeApp(ctx, url)
                                            } else {
                                                viewModel.openVideoTab(url, 20)
                                            }
                                        }
                                    },
                                    onOpenNewTab = { url, durationSec ->
                                        post {
                                            if (uiState.openInExternalYouTubeApp) {
                                                launchYouTubeApp(ctx, url)
                                            }
                                            viewModel.openVideoTab(url, durationSec)
                                        }
                                    },
                                    onAutomationLog = { logMsg ->
                                        post {
                                            viewModel.addAutomationLog(logMsg)
                                        }
                                    },
                                    onTaskSessionLocked = { taskId, taskTitle, durationSec, containerId ->
                                        post {
                                            viewModel.setCurrentTaskSession(taskId, taskTitle, durationSec, containerId)
                                            viewModel.addAutomationLog("[TaskLock] Locked container: $containerId | Task: $taskId ($durationSec s)")
                                        }
                                    },
                                    onWrongPageDetected = { reason, url ->
                                        post {
                                            viewModel.reportWrongPage(reason, url)
                                        }
                                    },
                                    onTaskMismatchDetected = { expected, found ->
                                        post {
                                            viewModel.reportTaskMismatch(expected, found)
                                        }
                                    },
                                    onTaskRestored = { taskId ->
                                        post {
                                            viewModel.reportTaskRestored(taskId)
                                        }
                                    }
                                ),
                                "AvisoBridge"
                            )

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    viewModel.setProgress(newProgress)
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    title?.let { viewModel.setPageTitle(it) }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    fileUploadCallback?.onReceiveValue(null)
                                    fileUploadCallback = filePathCallback
                                    return try {
                                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "*/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                        fileChooserLauncher.launch(intent)
                                        true
                                    } catch (_: Exception) {
                                        fileUploadCallback?.onReceiveValue(null)
                                        fileUploadCallback = null
                                        false
                                    }
                                }

                                 override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val mainWv = view ?: return false
                                    CookieManager.getInstance().flush()
                                    val tempWebView = WebView(mainWv.context).apply {
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.setAcceptCookie(true)
                                        cookieManager.setAcceptThirdPartyCookies(this, true)
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        settings.userAgentString = mainWv.settings.userAgentString
                                    }
                                    tempWebView.webViewClient = object : WebViewClient() {
                                        private fun handleNewWindow(targetUrl: String, v: WebView?) {
                                            if (targetUrl.isNotEmpty() && targetUrl != "about:blank") {
                                                mainWv.post {
                                                    val isVideoLink = targetUrl.contains("youtube.com") ||
                                                            targetUrl.contains("youtu.be") ||
                                                            targetUrl.contains("/vl/") ||
                                                            targetUrl.contains("/go/") ||
                                                            targetUrl.contains("create_session") ||
                                                            targetUrl.contains("youtube.php")
                                                    if (isVideoLink) {
                                                        if (uiState.openInExternalYouTubeApp && (targetUrl.contains("youtube.com") || targetUrl.contains("youtu.be"))) {
                                                             launchYouTubeApp(ctx, targetUrl)
                                                        } else {
                                                            viewModel.openVideoTab(targetUrl, 20)
                                                        }
                                                    } else if (targetUrl.contains("tasks-youtube") || targetUrl.contains("tasks-vk") || targetUrl.contains("tasks")) {
                                                        mainWv.loadUrl(targetUrl)
                                                    } else {
                                                        viewModel.addNewTab(targetUrl, "ট্যাব ${uiState.tabs.size + 1}")
                                                    }
                                                }
                                                v?.postDelayed({ v.destroy() }, 800L)
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                            val targetUrl = request?.url?.toString().orEmpty()
                                            handleNewWindow(targetUrl, v)
                                            return true
                                        }

                                        override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                                            val targetUrl = url.orEmpty()
                                            handleNewWindow(targetUrl, v)
                                        }
                                    }
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                                    transport?.webView = tempWebView
                                    resultMsg?.sendToTarget()
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    
                                    // Handle intent / deep links
                                    if (url.startsWith("intent:") || url.startsWith("vnd.youtube:")) {
                                        try {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            val fallback = intent.getStringExtra("browser_fallback_url")
                                            val target = if (!fallback.isNullOrEmpty()) fallback else url
                                            if (target.contains("youtube.com") || target.contains("youtu.be")) {
                                                if (uiState.openInExternalYouTubeApp) {
                                                    launchYouTubeApp(ctx, target)
                                                } else {
                                                    viewModel.openVideoTab(target, 20)
                                                }
                                                return true
                                            }
                                            if (!fallback.isNullOrEmpty() && fallback.contains("aviso.bz")) {
                                                view?.loadUrl(fallback)
                                                return true
                                            }
                                            ctx.startActivity(intent)
                                        } catch (_: Exception) {}
                                        return true
                                    }

                                    // Intercept video tasks (/vl/, /go/, youtube, create_session) and open in Tab 2
                                    val isVideoLink = url.contains("youtube.com") ||
                                            url.contains("youtu.be") ||
                                            url.contains("/vl/") ||
                                            url.contains("/go/") ||
                                            url.contains("create_session") ||
                                            url.contains("youtube.php")

                                    if (isVideoLink) {
                                        if (uiState.openInExternalYouTubeApp && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                                            launchYouTubeApp(ctx, url)
                                        } else {
                                            viewModel.openVideoTab(url, 20)
                                        }
                                        return true // Keep main tab on task list, video opens in Tab 2!
                                    }

                                    // All Aviso.bz internal navigation pages load in this WebView
                                    if (url.contains("aviso.bz")) {
                                        return false
                                    }

                                    // For other URLs
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        val uri = Uri.parse(url)
                                        val host = uri.host.orEmpty().lowercase()
                                        // If external non-aviso site, open in new tab
                                        if (host.isNotEmpty() && !host.contains("aviso.bz") && !host.contains("google.com") && !host.contains("recaptcha") && !host.contains("hcaptcha") && !host.contains("cloudflare")) {
                                            viewModel.addNewTab(url, uri.host ?: "ট্যাব")
                                            return true
                                        }
                                        return false // Load inside this WebView
                                    }
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    val currentUrlStr = url.orEmpty()
                                    val isYT = currentUrlStr.contains("youtube.com") || currentUrlStr.contains("youtu.be")

                                    // Safety fallback: if somehow main tab navigated to YouTube domain directly
                                    if (isYT) {
                                        view?.stopLoading()
                                        if (view?.canGoBack() == true) {
                                            view.goBack()
                                        }
                                        if (uiState.openInExternalYouTubeApp) {
                                            launchYouTubeApp(ctx, currentUrlStr)
                                        } else {
                                            viewModel.openVideoTab(currentUrlStr, 20)
                                        }
                                        return
                                    }

                                    canGoBack = view?.canGoBack() == true
                                    viewModel.setErrorMessage(null)
                                    // Pre-override window.open early
                                    view?.evaluateJavascript(AvisoTaskParser.JS_SETUP_OVERRIDE, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    canGoBack = view?.canGoBack() == true
                                    CookieManager.getInstance().flush()

                                    // Override window.open and blank links to keep all tasks inside this WebView
                                    view?.evaluateJavascript(AvisoTaskParser.JS_SETUP_OVERRIDE, null)

                                    // Apply user zoom scale
                                    val zoomScale = uiState.zoomPercent / 100f
                                    view?.evaluateJavascript("document.body.style.zoom = '$zoomScale';", null)

                                    // Check and handle "Start Watching" interstitial screen if present
                                    view?.evaluateJavascript(AvisoTaskParser.JS_CHECK_AND_HANDLE_INTERSTITIAL, null)
                                    view?.postDelayed({
                                        view.evaluateJavascript(AvisoTaskParser.JS_CHECK_AND_HANDLE_INTERSTITIAL, null)
                                    }, 700L)

                                    // If this is a video or session page, trigger start and watch immediately
                                    val pageUrl = url.orEmpty()
                                    if (pageUrl.contains("/vl/") || pageUrl.contains("/go/") || pageUrl.contains("youtube") || pageUrl.contains("create_session")) {
                                        view?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                        view?.postDelayed({
                                            view.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                        }, 1000L)
                                    }

                                    // Inject task reader script
                                    view?.evaluateJavascript(AvisoTaskParser.JS_READER_CODE, null)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        viewModel.setErrorMessage(error?.description?.toString() ?: "পেজ লোড হতে ব্যর্থ হয়েছে")
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    // Proceed to avoid blocking redirects
                                    handler?.proceed()
                                }
                            }

                            loadUrl(uiState.currentUrl)
                            webViewRef = this
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                        wv.visibility = if (!isCurrentTabVideo) View.VISIBLE else View.INVISIBLE
                        canGoBack = wv.canGoBack()
                    }
                )
            }

            // Layer 2: Video Player Tab View
            if (uiState.isVideoTabOpen && !uiState.videoTabUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isCurrentTabVideo) 2f else 0f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .testTag("video_tab_container")
                    ) {
                        // Video WebView inside Tab 2 (full screen without overlay banner)
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("secondary_video_webview"),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        allowContentAccess = true
                                        allowFileAccess = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        mediaPlaybackRequiresUserGesture = false
                                        setSupportMultipleWindows(false)
                                        javaScriptCanOpenWindowsAutomatically = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                    }

                                    addJavascriptInterface(
                                        AvisoBridge(
                                            onResult = {},
                                            onCaptchaFound = { msg ->
                                                post { viewModel.onCaptchaDetected(ctx, msg) }
                                            },
                                            onTaskStarted = { dur ->
                                                post {
                                                    autoWorkTaskStartedSignal = dur
                                                    viewModel.updateVideoTabCountdown(dur, dur)
                                                }
                                            },
                                            onRealTimerUpdate = { sec ->
                                                post {
                                                    realTimerSecondsSignal = sec
                                                    if (sec >= 0) {
                                                        val total = if (uiState.videoTabDuration > 0) uiState.videoTabDuration.coerceAtLeast(sec) else sec
                                                        viewModel.updateVideoTabCountdown(sec, total)
                                                        if (uiState.isAutoWorkRunning) {
                                                            viewModel.updateAutoWorkCountdown(sec, total)
                                                        }
                                                    }
                                                }
                                            },
                                            onTaskCompleted = {
                                                post {
                                                    taskCompletedSignal = true
                                                    viewModel.incrementSuccessCount()
                                                }
                                            },
                                            onConfirmClicked = { clicked ->
                                                post {
                                                    if (clicked) {
                                                        confirmClickedSignal = true
                                                        viewModel.incrementSuccessCount()
                                                    }
                                                }
                                            },
                                            onInterstitialHandled = { dur ->
                                                post {
                                                    realInterstitialDurationSignal = dur
                                                    viewModel.updateVideoTabCountdown(dur, dur)
                                                }
                                            },
                                            onOpenYouTube = { ytUrl ->
                                                post {
                                                    if (uiState.openInExternalYouTubeApp && ytUrl.isNotEmpty()) {
                                                        launchYouTubeApp(ctx, ytUrl)
                                                    }
                                                }
                                            },
                                            onOpenNewTab = { newUrl, dur ->
                                                post {
                                                    if (newUrl.isNotBlank()) {
                                                        loadUrl(newUrl)
                                                    }
                                                }
                                            },
                                            onAutomationLog = { logMsg ->
                                                post {
                                                    viewModel.addAutomationLog(logMsg)
                                                }
                                            }
                                        ),
                                        "AvisoBridge"
                                    )

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onPermissionRequest(request: PermissionRequest?) {
                                            request?.grant(request.resources)
                                        }
                                        override fun getDefaultVideoPoster(): Bitmap? {
                                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            if (url.startsWith("intent:") || url.startsWith("vnd.youtube:") || url.startsWith("market:")) {
                                                try {
                                                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                                    val fallback = intent.getStringExtra("browser_fallback_url")
                                                    val target = if (!fallback.isNullOrEmpty()) fallback else intent.dataString
                                                    if (!target.isNullOrEmpty() && (target.startsWith("http://") || target.startsWith("https://"))) {
                                                        view?.loadUrl(target)
                                                    }
                                                } catch (_: Exception) {}
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            view?.evaluateJavascript(AvisoTaskParser.JS_SETUP_OVERRIDE, null)
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            view?.evaluateJavascript(AvisoTaskParser.JS_SETUP_OVERRIDE, null)
                                            view?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                            view?.postDelayed({
                                                view.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                            }, 400L)
                                            view?.postDelayed({
                                                view.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                                view.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_CLICK_CONFIRM, null)
                                            }, 1200L)
                                            view?.postDelayed({
                                                view.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                                            }, 2200L)
                                        }

                                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                            handler?.proceed()
                                        }
                                    }

                                    uiState.videoTabUrl?.let {
                                        lastLoadedVideoUrl = it
                                        loadUrl(it)
                                    }
                                    secondaryWebViewRef = this
                                }
                            },
                            update = { wv ->
                                secondaryWebViewRef = wv
                                wv.visibility = if (isCurrentTabVideo) View.VISIBLE else View.INVISIBLE
                                val curVideoUrl = uiState.videoTabUrl
                                if (!curVideoUrl.isNullOrEmpty() && curVideoUrl != lastLoadedVideoUrl) {
                                    lastLoadedVideoUrl = curVideoUrl
                                    wv.loadUrl(curVideoUrl)
                                }
                            }
                        )
                    }
                }
            }

            // Error Overlay if Connection Fails
            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "পেজ লোডিং সমস্যা হয়েছে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.errorMessage ?: "ইন্টারনেট সংযোগ চেক করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.reloadPage()
                                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("পুনরায় চেষ্টা করুন (Reload)")
                        }
                    }
                }
            }

            // Captcha Alert Banner (Shown when Captcha is detected)
            if (uiState.captchaDetectedAlert != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("captcha_alert_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "ক্যাপচা সতর্কবার্তা",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ ক্যাপচা (Captcha) সনাক্ত হয়েছে!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB71C1C),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.captchaDetectedAlert ?: "অটো কাজ বন্ধ করা হয়েছে এবং ৫ সেকেন্ড ধরে অ্যালার্ম বাজছে। দয়া করে নিজে ক্যাপচাটি সমাধান করুন।",
                                fontSize = 11.sp,
                                color = Color(0xFF424242)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.dismissCaptchaAlert() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বন্ধ করুন",
                                tint = Color(0xFFB71C1C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Overlay Permission Recommendation Dialog (For Auto-Return from YouTube)
            if (showOverlayPromptDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showOverlayPromptDialog = false
                        didDismissOverlayPrompt = true
                        viewModel.startAutoWork()
                    },
                    title = {
                        Text(
                            text = "অন্যান্য অ্যাপের উপর প্রদর্শন পারমিশন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = "YouTube অ্যাপে ভিডিও দেখা শেষ হওয়ার সাথে সাথে Aviso অ্যাপ যেন নিজে থেকেই স্ক্রিনে ফিরে এসে 'Подтвердить просмотр' কনফার্ম করতে পারে, সেজন্য 'Display over other apps' অনুমতি দেওয়া সুপারিশ করা হচ্ছে।\n\nআপনি কি এখনই এই অনুমতি চালু করতে চান?",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                showOverlayPromptDialog = false
                                didDismissOverlayPrompt = true
                                viewModel.requestOverlayPermission(context)
                                viewModel.startAutoWork()
                            }
                        ) {
                            Text("অনুমতি দিন")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showOverlayPromptDialog = false
                                didDismissOverlayPrompt = true
                                viewModel.startAutoWork()
                            }
                        ) {
                            Text("পরে (এখনই শুরু করুন)")
                        }
                    }
                )
            }

            // Live Countdown HUD (Shown only when Auto Work is active - no bottom start button as user requested)
            if (uiState.isAutoWorkRunning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_work_running_hud"),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shadowElevation = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // On-Screen Live Countdown Badge
                                    if (uiState.autoWorkCountdownSeconds > 0) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(50.dp)
                                        ) {
                                            val total = if (uiState.autoWorkTotalSeconds > 0) uiState.autoWorkTotalSeconds else 10
                                            val progress = (uiState.autoWorkCountdownSeconds.toFloat() / total).coerceIn(0f, 1f)
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.size(50.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeWidth = 4.5.dp
                                            )
                                            Text(
                                                text = "${uiState.autoWorkCountdownSeconds}s",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(26.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = if (uiState.autoWorkCountdownSeconds > 0) "ভিডিও চলছে: ${uiState.autoWorkCountdownSeconds} সেকেন্ড বাকি" else "অটো কাজ প্রক্রিয়াধীন...",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = uiState.autoWorkStatus.ifEmpty { "টাস্ক প্রসেসিং চলছে..." },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Pause / Resume Button in HUD
                                Surface(
                                    onClick = {
                                        if (uiState.isAutoWorkPaused) viewModel.resumeAutoWork() else viewModel.pauseAutoWork()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (uiState.isAutoWorkPaused) Color(0xFFD97706) else MaterialTheme.colorScheme.secondaryContainer,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.testTag("hud_pause_resume_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isAutoWorkPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = if (uiState.isAutoWorkPaused) "Resume" else "Pause",
                                            tint = if (uiState.isAutoWorkPaused) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.isAutoWorkPaused) "Resume" else "Pause",
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isAutoWorkPaused) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Stop Work Button
                                Surface(
                                    onClick = { viewModel.stopAutoWork("ব্যবহারকারী বন্ধ করেছেন") },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFEF4444),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.testTag("stop_work_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop Work",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Stop",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Linear progress bar for timer
                            if (uiState.autoWorkCountdownSeconds > 0 && uiState.autoWorkTotalSeconds > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { (uiState.autoWorkCountdownSeconds.toFloat() / uiState.autoWorkTotalSeconds).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Live Countdown HUD for Manual Click Mode (when user clicks blue link manually)
            if (!uiState.isAutoWorkRunning && manualWatchCountdown != null && manualWatchCountdown!! > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_watch_countdown_hud"),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.6f)),
                        shadowElevation = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    val total = (manualWatchTotal ?: 10).coerceAtLeast(1)
                                    val progress = (manualWatchCountdown!!.toFloat() / total).coerceIn(0f, 1f)
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(50.dp),
                                        color = Color(0xFF2563EB),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 4.5.dp
                                    )
                                    Text(
                                        text = "${manualWatchCountdown}s",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Start Watching সক্রিয়",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ভিডিও চলছে: $manualWatchCountdown সেকেন্ড বাকি (মোট ${manualWatchTotal}s)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (manualWatchTotal != null && manualWatchTotal!! > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { (manualWatchCountdown!!.toFloat() / manualWatchTotal!!).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF2563EB),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom-Right Small Success / Failure Counter
            // Requested: "কয়টা কাজ সফল হইছে আর কয়টা হয়নি তা নিচে একদম ডান এ ছোট করে থাকবে একদম ছোট"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp)
                    .clickable {
                        Toast.makeText(
                            context,
                            "সফল: ${uiState.successfulTasksCount} টি | ব্যর্থ: ${uiState.failedTasksCount} টি",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .testTag("task_counter_bottom_right_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Success counter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "সফল কাজ",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        Text(
                            text = "${uiState.successfulTasksCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }

                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    // Failed counter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "ব্যর্থ কাজ",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        Text(
                            text = "${uiState.failedTasksCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Right-docked Floating Sidebar Handle Button (Easy access to open sidebar)
            if (!uiState.isSidebarOpen) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .clickable { viewModel.openSidebar() }
                        .testTag("floating_sidebar_tab"),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "সাইজ পরিবর্তন সাইডবার",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.zoomPercent}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scrim Backdrop when Sidebar is Open
            if (uiState.isSidebarOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.closeSidebar()
                        }
                )
            }

            // Animated Right Sidebar (Slide-in from Right)
            AnimatedVisibility(
                visible = uiState.isSidebarOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                AvisoSidebar(
                    uiState = uiState,
                    viewModel = viewModel,
                    onClose = { viewModel.closeSidebar() }
                )
            }

            // Real Browser Tabs Overview Dialog ("ঘরের মত / ট্যাবস আইকন ভিউ")
            if (showTabsOverviewDialog) {
                AlertDialog(
                    onDismissRequest = { showTabsOverviewDialog = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "খোলা ট্যাবসমূহ (${uiState.tabs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showTabsOverviewDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.tabs, key = { it.id }) { tab ->
                                    val isSelected = tab.id == uiState.activeTabId
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.selectTabById(tab.id)
                                                showTabsOverviewDialog = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (tab.isVideoTab) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (tab.isVideoTab) Icons.Default.PlayArrow else Icons.Default.Language,
                                                        contentDescription = null,
                                                        tint = if (tab.isVideoTab) Color.White else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = if (tab.isVideoTab) {
                                                            if (tab.remainingSec > 0) "🎬 ভিডিও দেখা হচ্ছে (${tab.remainingSec}s)" else "🎬 ভিডিও ট্যাব"
                                                        } else {
                                                            tab.title.ifEmpty { "Aviso.bz" }
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (isSelected) "🟢 সক্রিয় ট্যাব" else tab.url,
                                                        fontSize = 11.5.sp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (uiState.tabs.size > 1 || tab.isVideoTab) {
                                                IconButton(
                                                    onClick = {
                                                        if (tab.isVideoTab) {
                                                            viewModel.closeVideoTab()
                                                        } else {
                                                            viewModel.closeTabById(tab.id)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "ট্যাব বন্ধ করুন",
                                                        tint = if (tab.isVideoTab) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.addNewTab("https://aviso.bz/tasks-youtube", "ট্যাব ${uiState.tabs.size + 1}")
                                    showTabsOverviewDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ নতুন ট্যাব তৈরি করুন")
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // Realtime Automation Logs Dialog ([Task], [Browser], [Timer], [Protection])
            if (showLogsDialog) {
                AlertDialog(
                    onDismissRequest = { showLogsDialog = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ListAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Automation Logs (${uiState.automationLogs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showLogsDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current State: ${uiState.automationState.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { viewModel.clearAutomationLogs() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear Logs", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (uiState.automationLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No logs recorded yet.\nStart Work to see structured live logs.",
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.automationLogs) { logLine ->
                                        val textColor = when {
                                            logLine.contains("[Protection]") -> Color(0xFFF87171)
                                            logLine.contains("[Task]") -> Color(0xFF60A5FA)
                                            logLine.contains("[Timer]") -> Color(0xFFFBBF24)
                                            logLine.contains("[Browser]") -> Color(0xFF34D399)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                        Text(
                                            text = logLine,
                                            color = textColor,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.onPause()
                    wv.pauseTimers()
                    wv.destroy()
                } catch (_: Exception) { }
            }
            webViewRef = null
            secondaryWebViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.onPause()
                    wv.pauseTimers()
                    wv.destroy()
                } catch (_: Exception) { }
            }
            secondaryWebViewRef = null
        }
    }
}
