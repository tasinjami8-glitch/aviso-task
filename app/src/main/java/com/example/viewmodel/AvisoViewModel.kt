package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    // Tab System States
    val selectedTabIndex: Int = 0, // 0 = Main Aviso, 1 = Video Tab
    val isVideoTabOpen: Boolean = false,
    val videoTabUrl: String? = null,
    val videoTabTitle: String = "ভিডিও প্লেয়ার",
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
        _uiState.update {
            it.copy(
                isAutoWorkRunning = true,
                autoWorkCountdownSeconds = 0,
                autoWorkTotalSeconds = 0,
                autoWorkStatus = "অটো কাজ শুরু হচ্ছে...",
                autoWorkTaskTitle = "",
                captchaDetectedAlert = null
            )
        }
    }

    fun stopAutoWork(reason: String = "") {
        _uiState.update {
            it.copy(
                isAutoWorkRunning = false,
                autoWorkCountdownSeconds = 0,
                autoWorkTotalSeconds = 0,
                autoWorkStatus = if (reason.isNotEmpty()) reason else "অটো কাজ বন্ধ"
            )
        }
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

    fun openVideoTab(url: String, durationSec: Int = 15, title: String = "") {
        val finalDur = if (durationSec > 0) durationSec else 15
        _uiState.update {
            it.copy(
                isVideoTabOpen = true,
                selectedTabIndex = 1,
                videoTabUrl = url,
                videoTabTitle = if (title.isNotEmpty()) title else "🎬 ভিডিও দেখা হচ্ছে...",
                videoTabDuration = finalDur,
                videoTabRemainingSec = finalDur
            )
        }
    }

    fun closeVideoTab() {
        _uiState.update {
            it.copy(
                isVideoTabOpen = false,
                selectedTabIndex = 0,
                videoTabUrl = null,
                videoTabDuration = 0,
                videoTabRemainingSec = 0
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun updateVideoTabCountdown(remainingSec: Int, totalSec: Int) {
        _uiState.update {
            it.copy(
                videoTabRemainingSec = remainingSec,
                videoTabDuration = totalSec
            )
        }
    }

    fun toggleExternalYouTubeAppMode() {
        _uiState.update { it.copy(openInExternalYouTubeApp = !it.openInExternalYouTubeApp) }
    }
}
