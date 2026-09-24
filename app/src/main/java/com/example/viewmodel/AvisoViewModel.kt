package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveTaskContext
import com.example.model.AutomationState
import com.example.model.BrowserTab
import com.example.model.CurrentTaskSession
import com.example.model.TaskScanResult
import com.example.service.AvisoTaskMonitorService
import com.example.util.AvisoNotificationHelper
import com.example.util.AvisoPermissionHelper
import com.example.util.AvisoTaskParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AvisoUiState(
    val currentUrl: String = "https://aviso.bz/tasks-youtube",
    val pageTitle: String = "Aviso - YouTube Tasks",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val zoomPercent: Int = 100,
    val isDesktopMode: Boolean = false,
    val isSidebarOpen: Boolean = false,
    val scanResult: TaskScanResult = TaskScanResult(),
    val isBgServiceRunning: Boolean = false,
    val checkIntervalMinutes: Int = 2,
    val isNotificationsEnabled: Boolean = true,
    val isNotificationSoundEnabled: Boolean = true,
    val isNotifyOnTaskEndEnabled: Boolean = true,
    val errorMessage: String? = null,
    val reloadTrigger: Long = 0L,
    val lastKnownTasksCount: Int = 0,
    val isAutoWorkRunning: Boolean = false,
    val isAutoWorkPaused: Boolean = false,
    val automationState: AutomationState = AutomationState.IDLE,
    val currentTaskContext: ActiveTaskContext? = null,
    val currentTaskSession: CurrentTaskSession? = null,
    val totalQueuedTasksCount: Int = 0,
    val currentTaskIndex: Int = 0,
    val automationLogs: List<String> = emptyList(),
    val autoWorkCountdownSeconds: Int = 0,
    val autoWorkTotalSeconds: Int = 0,
    val autoWorkStatus: String = "",
    val autoWorkTaskTitle: String = "",
    val successfulTasksCount: Int = 0,
    val failedTasksCount: Int = 0,
    val isOverlayPermissionGranted: Boolean = false,
    val isBatteryOptimizationExempted: Boolean = false,
    val isNotificationGranted: Boolean = true,
    val captchaDetectedAlert: String? = null,
    // Real Browser Multi-Tab System
    val tabs: List<BrowserTab> = listOf(
        BrowserTab(id = "tab_main_aviso", title = "Aviso.bz", url = "https://aviso.bz/tasks-youtube")
    ),
    val activeTabId: String = "tab_main_aviso",
    val selectedTabIndex: Int = 0, // backwards compat
    val isVideoTabOpen: Boolean = false,
    val videoTabUrl: String? = null,
    val videoTabTitle: String = "🎬 ভিডিও",
    val videoTabDuration: Int = 0,
    val videoTabRemainingSec: Int = 0,
    val openInExternalYouTubeApp: Boolean = false
)

class AvisoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AvisoUiState())
    val uiState: StateFlow<AvisoUiState> = _uiState.asStateFlow()

    fun loadNotificationPreferences(context: Context) {
        _uiState.update {
            it.copy(
                isNotificationsEnabled = AvisoNotificationHelper.isNotificationsEnabled(context),
                isNotificationSoundEnabled = AvisoNotificationHelper.isSoundEnabled(context),
                isNotifyOnTaskEndEnabled = AvisoNotificationHelper.isNotifyTaskFinishEnabled(context)
            )
        }
    }

    fun toggleNotifications(context: Context) {
        val newState = !_uiState.value.isNotificationsEnabled
        AvisoNotificationHelper.setNotificationsEnabled(context, newState)
        _uiState.update { it.copy(isNotificationsEnabled = newState) }
    }

    fun toggleNotificationSound(context: Context) {
        val newState = !_uiState.value.isNotificationSoundEnabled
        AvisoNotificationHelper.setSoundEnabled(context, newState)
        _uiState.update { it.copy(isNotificationSoundEnabled = newState) }
    }

    fun toggleNotifyOnTaskEnd(context: Context) {
        val newState = !_uiState.value.isNotifyOnTaskEndEnabled
        AvisoNotificationHelper.setNotifyTaskFinishEnabled(context, newState)
        _uiState.update { it.copy(isNotifyOnTaskEndEnabled = newState) }
    }

    fun setZoom(percent: Int) {
        val clamped = percent.coerceIn(50, 250)
        _uiState.update { it.copy(zoomPercent = clamped) }
    }

    fun zoomIn() {
        setZoom(_uiState.value.zoomPercent + 15)
    }

    fun zoomOut() {
        setZoom(_uiState.value.zoomPercent - 15)
    }

    fun resetZoom() {
        setZoom(100)
    }

    fun toggleDesktopMode() {
        _uiState.update { it.copy(isDesktopMode = !it.isDesktopMode) }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(isSidebarOpen = !it.isSidebarOpen) }
    }

    fun openSidebar() {
        _uiState.update { it.copy(isSidebarOpen = true) }
    }

    fun closeSidebar() {
        _uiState.update { it.copy(isSidebarOpen = false) }
    }

    fun reloadPage() {
        _uiState.update { it.copy(reloadTrigger = System.currentTimeMillis(), errorMessage = null) }
    }

    fun navigateTo(url: String) {
        _uiState.update { it.copy(currentUrl = url, errorMessage = null) }
    }

    fun setProgress(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun setPageTitle(title: String) {
        _uiState.update { it.copy(pageTitle = title) }
    }

    fun setErrorMessage(msg: String?) {
        _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
    }

    fun onScanResultReceived(context: Context, jsonString: String) {
        viewModelScope.launch {
            val result = AvisoTaskParser.parseJsonScanResult(jsonString)
            val previousCount = _uiState.value.lastKnownTasksCount

            if (result.totalTasks > 0 && result.totalTasks > previousCount && previousCount >= 0) {
                // Trigger notification if newly added tasks detected
                val details = buildString {
                    append("ইউটিউবে নতুন কাজ পাওয়া গেছে! মোট: ${result.totalTasks} টি")
                    if (result.watchCount > 0) append("\n• ভিডিও দেখা: ${result.watchCount} টি")
                    if (result.subscribeCount > 0) append("\n• চ্যানেল সাবস্ক্রাইব: ${result.subscribeCount} টি")
                }
                AvisoNotificationHelper.sendTaskNotification(
                    context,
                    result.totalTasks,
                    details,
                    isNewDetected = true
                )
            } else if (previousCount > 0 && result.totalTasks == 0) {
                // All tasks finished
                AvisoNotificationHelper.sendNoTasksNotification(
                    context,
                    wasCompleted = true
                )
            }

            _uiState.update {
                it.copy(
                    scanResult = result,
                    lastKnownTasksCount = result.totalTasks
                )
            }
        }
    }

    fun toggleBgService(context: Context) {
        val currentRunning = AvisoTaskMonitorService.isServiceRunning
        if (currentRunning) {
            AvisoTaskMonitorService.stop(context)
            _uiState.update { it.copy(isBgServiceRunning = false) }
        } else {
            AvisoTaskMonitorService.start(context, _uiState.value.checkIntervalMinutes)
            _uiState.update { it.copy(isBgServiceRunning = true) }
        }
    }

    fun setCheckInterval(context: Context, minutes: Int) {
        _uiState.update { it.copy(checkIntervalMinutes = minutes) }
        if (AvisoTaskMonitorService.isServiceRunning) {
            AvisoTaskMonitorService.start(context, minutes)
        }
    }

    fun sendTestNotification(context: Context) {
        AvisoNotificationHelper.sendTestNotification(context)
    }

    fun openAppNotificationSettings(context: Context) {
        AvisoNotificationHelper.openAppNotificationSettings(context)
    }

    fun syncServiceState() {
        _uiState.update {
            it.copy(isBgServiceRunning = AvisoTaskMonitorService.isServiceRunning)
        }
    }

    fun startAutoWork() {
        addAutomationLog("[Task] Automation session started")
        _uiState.update {
            it.copy(
                isAutoWorkRunning = true,
                isAutoWorkPaused = false,
                automationState = AutomationState.TASK_DISCOVERED,
                autoWorkCountdownSeconds = 0,
                autoWorkTotalSeconds = 0,
                autoWorkStatus = "অটো কাজ শুরু হচ্ছে...",
                autoWorkTaskTitle = "",
                captchaDetectedAlert = null
            )
        }
    }

    fun pauseAutoWork() {
        addAutomationLog("[Task] Automation paused by user")
        _uiState.update {
            it.copy(
                isAutoWorkPaused = true,
                automationState = AutomationState.PAUSED,
                autoWorkStatus = "অটো কাজ সাময়িক বিরতি (Paused)"
            )
        }
    }

    fun resumeAutoWork() {
        addAutomationLog("[Task] Automation resumed by user")
        _uiState.update {
            it.copy(
                isAutoWorkPaused = false,
                automationState = if (it.currentTaskContext != null) AutomationState.VIEWING else AutomationState.TASK_DISCOVERED,
                autoWorkStatus = "অটো কাজ পুনরায় চালু হচ্ছে..."
            )
        }
    }

    fun setCurrentTaskSession(
        taskId: String,
        taskTitle: String = "",
        durationSec: Int = 15,
        containerId: String = "",
        savedTaskPageUrl: String = "https://aviso.bz/tasks-youtube"
    ) {
        val session = CurrentTaskSession(
            taskId = taskId,
            taskTitle = taskTitle,
            durationSec = durationSec,
            savedTaskPageUrl = savedTaskPageUrl,
            containerId = containerId,
            isLocked = true
        )
        setCurrentTaskSession(session)
    }

    fun setCurrentTaskSession(session: CurrentTaskSession?) {
        _uiState.update {
            it.copy(
                currentTaskSession = session,
                currentTaskContext = session?.let { s ->
                    ActiveTaskContext(
                        taskId = s.taskId,
                        taskTitle = s.taskTitle,
                        durationSec = s.durationSec,
                        videoUrl = s.videoUrl,
                        originalPageUrl = s.savedTaskPageUrl,
                        savedTaskPageUrl = s.savedTaskPageUrl,
                        cellIndex = s.cellIndex,
                        containerId = s.containerId,
                        isLocked = s.isLocked
                    )
                }
            )
        }
    }

    fun reportWrongPage(reason: String, url: String) {
        addAutomationLog("[Protection] WRONG_PAGE_DETECTED: $reason (URL: $url). All clicks stopped immediately.")
        _uiState.update {
            it.copy(
                automationState = AutomationState.WRONG_PAGE_DETECTED,
                autoWorkStatus = "ভুল পেজ সনাক্ত! নিরাপদ রিকভারি চলছে..."
            )
        }
    }

    fun reportTaskMismatch(expectedId: String, foundId: String) {
        addAutomationLog("[Protection] TASK_MISMATCH: Expected Task ID = '$expectedId', Found Task ID = '$foundId'. Stopping clicks safely.")
        _uiState.update {
            it.copy(
                automationState = AutomationState.TASK_MISMATCH,
                autoWorkStatus = "টাস্ক মিসম্যাচ সনাক্ত! অটোমেশন নিরাপদভাবে স্থগিত করা হয়েছে।"
            )
        }
    }

    fun reportSafeRecovery(restoredUrl: String = "https://aviso.bz/tasks-youtube") {
        addAutomationLog("[Browser] Safe direct return initiated to saved task page: $restoredUrl")
        _uiState.update {
            it.copy(
                automationState = AutomationState.RECOVERY_IN_PROGRESS,
                autoWorkStatus = "সংরক্ষিত টাস্ক পেজে সরাসরি ফেরত যাওয়া হচ্ছে..."
            )
        }
    }

    fun reportTaskRestored(taskId: String) {
        addAutomationLog("[Task] TASK_PAGE_RESTORED: Expected task '$taskId' successfully verified on task page.")
        _uiState.update {
            it.copy(
                automationState = AutomationState.TASK_PAGE_RESTORED,
                autoWorkStatus = "টাস্ক পেজ সফলভাবে রিস্টোর ও ভেরিফাই করা হয়েছে ✓"
            )
        }
    }

    fun stopAutoWork(reason: String = "") {
        addAutomationLog("[Task] Automation stopped: ${reason.ifEmpty { "Manual stop" }}")
        _uiState.update {
            it.copy(
                isAutoWorkRunning = false,
                isAutoWorkPaused = false,
                automationState = AutomationState.STOPPED,
                currentTaskContext = null,
                currentTaskSession = null,
                autoWorkCountdownSeconds = 0,
                autoWorkTotalSeconds = 0,
                autoWorkStatus = if (reason.isNotEmpty()) reason else "অটো কাজ বন্ধ"
            )
        }
    }

    fun setAutomationState(state: AutomationState) {
        _uiState.update { it.copy(automationState = state) }
    }

    fun setActiveTaskContext(context: ActiveTaskContext?) {
        _uiState.update { it.copy(currentTaskContext = context) }
    }

    private var lastLogPruneTimestamp = System.currentTimeMillis()

    fun addAutomationLog(message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        _uiState.update { current ->
            // Keep recent current execution information visible (max 35 items)
            val updated = (listOf(entry) + current.automationLogs).take(35)
            current.copy(automationLogs = updated)
        }

        // Periodic auto-clear: automatically prune old logs every 45s so they do not build up
        val now = System.currentTimeMillis()
        if (now - lastLogPruneTimestamp > 45_000L) {
            lastLogPruneTimestamp = now
            pruneOldLogsPeriodically()
        }
    }

    fun pruneOldLogsPeriodically() {
        _uiState.update { current ->
            // Retain recent execution information without interrupting active task
            val pruned = current.automationLogs.take(20)
            current.copy(automationLogs = pruned)
        }
    }

    fun onTaskFinishedCleanLogs() {
        _uiState.update { current ->
            // After a task is fully finished, old debug logs may be cleared,
            // keeping only recent milestone/status logs (last 8 entries)
            val cleaned = current.automationLogs.take(8)
            current.copy(automationLogs = cleaned)
        }
    }

    fun clearAutomationLogs() {
        _uiState.update { it.copy(automationLogs = emptyList()) }
    }

    fun updateAutoWorkCountdown(remainingSec: Int, totalSec: Int) {
        _uiState.update {
            it.copy(
                autoWorkCountdownSeconds = remainingSec,
                autoWorkTotalSeconds = totalSec,
                autoWorkStatus = if (remainingSec > 0) "ভিডিও চলছে: $remainingSec সেকেন্ড বাকি" else "ভিডিও দেখা শেষ!"
            )
        }
    }

    fun setAutoWorkStatus(status: String, taskTitle: String = "") {
        _uiState.update {
            it.copy(
                autoWorkStatus = status,
                autoWorkTaskTitle = if (taskTitle.isNotEmpty()) taskTitle else it.autoWorkTaskTitle
            )
        }
    }

    fun onCaptchaDetected(context: Context, reason: String) {
        stopAutoWork("ক্যাপচা সনাক্ত: $reason")
        _uiState.update {
            it.copy(
                captchaDetectedAlert = "⚠️ ক্যাপচা (Captcha) সনাক্ত হয়েছে! ৫ সেকেন্ড সাউন্ড বাজছে এবং অটো কাজ নিজে থেকেই বন্ধ করা হয়েছে। ক্যাপচা সমাধান করুন।"
            )
        }
        AvisoNotificationHelper.playCaptchaAlertSound(context, 5000L)
    }

    fun dismissCaptchaAlert() {
        _uiState.update { it.copy(captchaDetectedAlert = null) }
    }

    fun incrementSuccessCount() {
        _uiState.update { it.copy(successfulTasksCount = it.successfulTasksCount + 1) }
    }

    fun incrementFailedCount() {
        _uiState.update { it.copy(failedTasksCount = it.failedTasksCount + 1) }
    }

    fun resetTaskCounts() {
        _uiState.update { it.copy(successfulTasksCount = 0, failedTasksCount = 0) }
    }

    fun refreshPermissions(context: Context) {
        _uiState.update {
            it.copy(
                isOverlayPermissionGranted = AvisoPermissionHelper.canDrawOverlays(context),
                isBatteryOptimizationExempted = AvisoPermissionHelper.isIgnoringBatteryOptimizations(context),
                isNotificationGranted = AvisoPermissionHelper.isNotificationPermissionGranted(context)
            )
        }
    }

    fun requestOverlayPermission(context: Context) {
        AvisoPermissionHelper.requestOverlayPermission(context)
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        AvisoPermissionHelper.requestIgnoreBatteryOptimizations(context)
    }

    fun openAppSettings(context: Context) {
        AvisoPermissionHelper.openAppSettings(context)
    }

    fun addNewTab(url: String = "https://aviso.bz/tasks-youtube", title: String = "নতুন ট্যাব"): String {
        val newId = "tab_" + System.currentTimeMillis()
        val newTab = BrowserTab(
            id = newId,
            title = title,
            url = url
        )
        _uiState.update { current ->
            val updatedTabs = current.tabs + newTab
            val newIdx = updatedTabs.indexOfFirst { it.id == newId }.coerceAtLeast(0)
            current.copy(
                tabs = updatedTabs,
                activeTabId = newId,
                selectedTabIndex = newIdx
            )
        }
        return newId
    }

    fun selectTabById(tabId: String) {
        _uiState.update { current ->
            val index = current.tabs.indexOfFirst { it.id == tabId }
            val tab = current.tabs.find { it.id == tabId }
            current.copy(
                activeTabId = tabId,
                selectedTabIndex = if (index >= 0) index else 0,
                isVideoTabOpen = tab?.isVideoTab == true,
                videoTabUrl = if (tab?.isVideoTab == true) tab.url else current.videoTabUrl,
                videoTabTitle = if (tab?.isVideoTab == true) tab.title else current.videoTabTitle,
                videoTabDuration = if (tab?.isVideoTab == true) tab.durationSec else current.videoTabDuration,
                videoTabRemainingSec = if (tab?.isVideoTab == true) tab.remainingSec else current.videoTabRemainingSec
            )
        }
    }

    fun closeTabById(tabId: String) {
        _uiState.update { current ->
            val remainingTabs = current.tabs.filterNot { it.id == tabId }
            val newTabs = if (remainingTabs.isEmpty()) {
                listOf(BrowserTab(id = "tab_main_aviso", title = "Aviso.bz", url = "https://aviso.bz/tasks-youtube"))
            } else {
                remainingTabs
            }
            val newActiveId = if (current.activeTabId == tabId) {
                newTabs.last().id
            } else {
                if (newTabs.any { it.id == current.activeTabId }) current.activeTabId else newTabs.first().id
            }
            val newActiveTab = newTabs.find { it.id == newActiveId }
            val newIndex = newTabs.indexOfFirst { it.id == newActiveId }.coerceAtLeast(0)
            current.copy(
                tabs = newTabs,
                activeTabId = newActiveId,
                selectedTabIndex = newIndex,
                isVideoTabOpen = newTabs.any { it.isVideoTab },
                videoTabUrl = if (newActiveTab?.isVideoTab == true) newActiveTab.url else null,
                videoTabRemainingSec = if (newActiveTab?.isVideoTab == true) newActiveTab.remainingSec else 0
            )
        }
    }

    fun updateTabInfo(tabId: String, title: String? = null, url: String? = null) {
        _uiState.update { current ->
            val updated = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        title = title ?: tab.title,
                        url = url ?: tab.url
                    )
                } else tab
            }
            current.copy(tabs = updated)
        }
    }

    fun openVideoTab(url: String, durationSec: Int = 15, title: String = "") {
        val finalDur = if (durationSec > 0) durationSec else 15
        val videoTitle = if (title.isNotEmpty()) title else "🎬 ভিডিও ($finalDur s)"
        _uiState.update { current ->
            val existingVideoTab = current.tabs.find { it.isVideoTab }
            val updatedTabs = if (existingVideoTab != null) {
                current.tabs.map {
                    if (it.id == existingVideoTab.id) {
                        it.copy(
                            url = url,
                            title = videoTitle,
                            durationSec = finalDur,
                            remainingSec = finalDur
                        )
                    } else it
                }
            } else {
                current.tabs + BrowserTab(
                    id = "tab_video_" + System.currentTimeMillis(),
                    title = videoTitle,
                    url = url,
                    isVideoTab = true,
                    durationSec = finalDur,
                    remainingSec = finalDur
                )
            }
            val activeId = existingVideoTab?.id ?: updatedTabs.last().id
            val idx = updatedTabs.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            current.copy(
                tabs = updatedTabs,
                activeTabId = activeId,
                selectedTabIndex = idx,
                isVideoTabOpen = true,
                videoTabUrl = url,
                videoTabTitle = videoTitle,
                videoTabDuration = finalDur,
                videoTabRemainingSec = finalDur
            )
        }
    }

    fun closeVideoTab() {
        _uiState.update { current ->
            val remainingTabs = current.tabs.filterNot { it.isVideoTab }
            val newTabs = if (remainingTabs.isEmpty()) {
                listOf(BrowserTab(id = "tab_main_aviso", title = "Aviso.bz", url = "https://aviso.bz/tasks-youtube"))
            } else {
                remainingTabs
            }
            val mainTab = newTabs.firstOrNull { it.id == "tab_main_aviso" } ?: newTabs.first()
            val idx = newTabs.indexOfFirst { it.id == mainTab.id }.coerceAtLeast(0)
            current.copy(
                tabs = newTabs,
                activeTabId = mainTab.id,
                selectedTabIndex = idx,
                isVideoTabOpen = false,
                videoTabUrl = null,
                videoTabDuration = 0,
                videoTabRemainingSec = 0
            )
        }
    }

    fun selectTab(index: Int) {
        val tabs = _uiState.value.tabs
        if (index in tabs.indices) {
            selectTabById(tabs[index].id)
        }
    }

    fun updateVideoTabCountdown(remainingSec: Int, totalSec: Int) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.isVideoTab) {
                    tab.copy(
                        remainingSec = remainingSec,
                        durationSec = totalSec,
                        title = if (remainingSec > 0) "🎬 ভিডিও (${remainingSec}s)" else "🎬 ভিডিও শেষ"
                    )
                } else tab
            }
            current.copy(
                tabs = updatedTabs,
                videoTabRemainingSec = remainingSec,
                videoTabDuration = totalSec,
                videoTabTitle = if (remainingSec > 0) "🎬 ভিডিও (${remainingSec}s)" else "🎬 ভিডিও শেষ"
            )
        }
    }

    fun toggleExternalYouTubeAppMode() {
        _uiState.update { it.copy(openInExternalYouTubeApp = !it.openInExternalYouTubeApp) }
    }
}
