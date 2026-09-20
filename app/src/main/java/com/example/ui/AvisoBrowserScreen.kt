package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.widget.Toast
import com.example.util.AvisoNotificationHelper
import com.example.util.AvisoTaskParser
import com.example.viewmodel.AvisoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private var lastNativeTapTime = 0L

private fun simulateTap(webView: WebView?, x: Float, y: Float) {
    if (webView == null || x <= 0f || y <= 0f) return
    val now = SystemClock.uptimeMillis()
    if (now - lastNativeTapTime < 1500L) return
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
    private val onInterstitialHandled: (Int) -> Unit = {}
) {
    @JavascriptInterface
    fun onTasksScanned(json: String) {
        onResult(json)
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
    var canGoBack by remember { mutableStateOf(false) }

    // Synchronize service state and notification preferences on launch
    LaunchedEffect(Unit) {
        viewModel.syncServiceState()
        viewModel.loadNotificationPreferences(context)
    }

    // Connect WebView to Android lifecycle to properly pause/resume audio & js threads
    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.pauseTimers()
                    webViewRef?.onPause()
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
    var realInterstitialDurationSignal by remember { mutableStateOf<Int?>(null) }
    var manualWatchCountdown by remember { mutableStateOf<Int?>(null) }
    var manualWatchTotal by remember { mutableStateOf<Int?>(null) }

    // Manual watching countdown loop (when user clicks blue link manually and interstitial appears)
    LaunchedEffect(manualWatchTotal) {
        val total = manualWatchTotal ?: return@LaunchedEffect
        if (uiState.isAutoWorkRunning) return@LaunchedEffect

        var rem = total
        while (rem > 0 && !uiState.isAutoWorkRunning) {
            manualWatchCountdown = rem
            // Ensure video playback is triggered and unmuted
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
            delay(1000L)
            rem--
        }
        manualWatchCountdown = 0
        if (!uiState.isAutoWorkRunning) {
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_CLICK_CONFIRM, null)
            Toast.makeText(context, "ভিডিও দেখা সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
        }
        delay(2000L)
        manualWatchTotal = null
        manualWatchCountdown = null
    }

    // Auto Work Execution Engine
    LaunchedEffect(uiState.isAutoWorkRunning) {
        if (!uiState.isAutoWorkRunning) return@LaunchedEffect

        // Navigate to YouTube tasks page if not already there
        if (webViewRef?.url?.contains("tasks-youtube") != true) {
            viewModel.setAutoWorkStatus("ইউটিউব টাস্ক পেজে যাওয়া হচ্ছে...")
            webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
            delay(3500L)
        }

        while (isActive && uiState.isAutoWorkRunning) {
            autoWorkTaskStartedSignal = null
            autoWorkNoTasksSignal = false
            realTimerSecondsSignal = -1
            taskCompletedSignal = false
            realInterstitialDurationSignal = null

            // Step 1: Check Captcha
            viewModel.setAutoWorkStatus("ক্যাপচা চেক করা হচ্ছে...")
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_CHECK_CAPTCHA, null)
            delay(500L)
            if (!uiState.isAutoWorkRunning) break

            // Step 2: Find next video task & click task link
            viewModel.setAutoWorkStatus("ভিডিও কাজ খোঁজা ও শুরু করা হচ্ছে...")
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_FIND_AND_CLICK, null)

            // Wait for JS to detect task and trigger start (up to 7 seconds)
            var waited = 0
            while (autoWorkTaskStartedSignal == null && !autoWorkNoTasksSignal && waited < 70 && uiState.isAutoWorkRunning) {
                delay(100L)
                waited++
            }

            if (!uiState.isAutoWorkRunning) break

            // If no tasks are available
            if (autoWorkNoTasksSignal) {
                viewModel.setAutoWorkStatus("বর্তমানে কোনো কাজ নেই!")
                AvisoNotificationHelper.sendNoTasksNotification(context, wasCompleted = true)
                viewModel.stopAutoWork("সব কাজ শেষ হয়েছে!")
                Toast.makeText(context, "সবগুলো YouTube কাজ সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_LONG).show()
                break
            }

            var expectedDuration = (realInterstitialDurationSignal ?: autoWorkTaskStartedSignal ?: 10).coerceAtLeast(5)
            viewModel.setAutoWorkStatus("ভিডিও লোড হচ্ছে...")

            // Allow video player / page to load
            delay(2000L)

            // Step 3: Active Video Watching & Verification Loop
            // Repeatedly triggers playback, un-mutes, simulates native touch on YouTube play button,
            // and tracks Aviso's real on-screen timer.
            val maxWaitSeconds = { (realInterstitialDurationSignal ?: expectedDuration) + 25 }
            var elapsedSec = 0

            while (elapsedSec < maxWaitSeconds() && uiState.isAutoWorkRunning && !taskCompletedSignal) {
                // If interstitial duration was detected (e.g. 90 seconds), dynamically adapt expectedDuration
                if (realInterstitialDurationSignal != null && realInterstitialDurationSignal!! > expectedDuration) {
                    expectedDuration = realInterstitialDurationSignal!!
                }

                // Execute active watcher to trigger playback, unmute, native tap and read real timer
                webViewRef?.evaluateJavascript(AvisoTaskParser.JS_START_AND_WATCH_VIDEO, null)
                delay(1000L)
                elapsedSec++

                // Update countdown display with real timer if available, otherwise countdown fallback
                val currentRemaining = if (realTimerSecondsSignal > 0) {
                    realTimerSecondsSignal
                } else {
                    (expectedDuration - elapsedSec).coerceAtLeast(0)
                }
                viewModel.updateAutoWorkCountdown(currentRemaining, expectedDuration)

                // If real timer reached 0 or confirm was triggered
                if (realTimerSecondsSignal == 0 || taskCompletedSignal) {
                    webViewRef?.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_CLICK_CONFIRM, null)
                    delay(1500L)
                    break
                }
            }

            if (!uiState.isAutoWorkRunning) break
            viewModel.updateAutoWorkCountdown(0, expectedDuration)

            // Step 4: Final Confirm View Click check
            viewModel.setAutoWorkStatus("ভিউ নিশ্চিতকরণ চেক করা হচ্ছে...")
            webViewRef?.evaluateJavascript(AvisoTaskParser.JS_AUTO_WORK_CLICK_CONFIRM, null)
            delay(1500L)

            // Step 5: Return to tasks-youtube page
            viewModel.setAutoWorkStatus("ভিডিও দেখা সফল! পরবর্তী কাজে যাওয়া হচ্ছে...")
            if (webViewRef?.url?.contains("tasks-youtube") != true) {
                webViewRef?.loadUrl("https://aviso.bz/tasks-youtube")
                delay(3000L)
            } else {
                webViewRef?.reload()
                delay(3000L)
            }

            // Step 6: Brief interval before scanning next task
            delay(1000L)
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
                                        viewModel.startAutoWork()
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
                                        if (uiState.autoWorkCountdownSeconds > 0) "${uiState.autoWorkCountdownSeconds}s বাকি" else "চলছে..."
                                    } else {
                                        "Start Work"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isAutoWorkRunning) Color(0xFFDC2626) else Color(0xFF047857)
                                )
                            }
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Web Browser View
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
                            setSupportMultipleWindows(false)
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
                                            viewModel.setAutoWorkStatus("Подтвердить просмотр সফল হয়েছে")
                                        }
                                    }
                                },
                                onVideoPositionFound = { x, y ->
                                    post {
                                        simulateTap(this, x, y)
                                    }
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

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = view
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    return false // Load inside this WebView
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                canGoBack = view?.canGoBack() == true
                                viewModel.setErrorMessage(null)
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

                                // Inject task reader script
                                view?.evaluateJavascript(AvisoTaskParser.JS_READER_CODE, null)

                                // If Auto-work is running, check for captcha presence
                                if (uiState.isAutoWorkRunning) {
                                    view?.evaluateJavascript(AvisoTaskParser.JS_CHECK_CAPTCHA, null)
                                }
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
                    canGoBack = wv.canGoBack()
                }
            )

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
        }
    }
}
