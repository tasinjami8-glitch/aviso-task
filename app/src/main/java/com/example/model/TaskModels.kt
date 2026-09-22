package com.example.model

enum class AutomationState {
    IDLE,
    TASK_FOUND,
    TASK_DISCOVERED,
    TASK_VALIDATED,
    TASK_LOCKED,
    ACTIVE_TASK_LOCKED,
    VIDEO_OPENING,
    YOUTUBE_OPENED,
    VIDEO_READY,
    VIEWING,
    VIEW_COMPLETE,
    DURATION_COMPLETED,
    RETURNING_TO_TASK,
    RETURNING_TO_TASK_PAGE,
    TASK_PAGE_VERIFICATION,
    TASK_RESTORED,
    TASK_VERIFIED,
    CONFIRM_VIEW_PENDING,
    CONFIRMATION_PENDING,
    CONFIRMING_VIEW,
    TASK_COMPLETED,
    NEXT_TASK,
    WRONG_PAGE_DETECTED,
    RECOVERY_IN_PROGRESS,
    TASK_PAGE_RESTORED,
    TASK_MISMATCH,
    TASK_LIST_EXHAUSTED,
    PAUSED,
    STOPPED,
    RECOVERY_REQUIRED
}

data class CurrentTaskSession(
    val taskId: String = "",
    val taskTitle: String = "",
    val durationSec: Int = 15,
    val videoUrl: String = "",
    val savedTaskPageUrl: String = "https://aviso.bz/tasks-youtube",
    val cellIndex: Int = -1,
    val containerId: String = "",
    val isLocked: Boolean = true,
    val sessionStartTime: Long = System.currentTimeMillis()
)

data class ActiveTaskContext(
    val taskId: String = "",
    val taskTitle: String = "",
    val durationSec: Int = 15,
    val videoUrl: String = "",
    val originalPageUrl: String = "https://aviso.bz/tasks-youtube",
    val savedTaskPageUrl: String = "https://aviso.bz/tasks-youtube",
    val cellIndex: Int = -1,
    val containerId: String = "",
    val isLocked: Boolean = true
)

data class PageVerificationResult(
    val isValidTaskPage: Boolean = false,
    val isWrongPage: Boolean = false,
    val wrongPageReason: String = "",
    val currentUrl: String = "",
    val currentDomain: String = "",
    val hasExpectedTask: Boolean = false,
    val matchedTaskId: String = "",
    val isTasksYoutubePage: Boolean = false
)

data class TaskInfo(
    val id: String,
    val title: String,
    val category: String,
    val reward: String,
    val durationSeconds: String = "",
    val link: String = ""
)

data class TaskScanResult(
    val totalTasks: Int = 0,
    val subscribeCount: Int = 0,
    val watchCount: Int = 0,
    val likesCount: Int = 0,
    val tasks: List<TaskInfo> = emptyList(),
    val lastScanTime: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val rawStatus: String = ""
)

data class AvisoSettings(
    val zoomPercent: Int = 100,
    val isDesktopMode: Boolean = false,
    val isBgMonitoringEnabled: Boolean = true,
    val checkIntervalMinutes: Int = 2,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Aviso.bz",
    val url: String = "https://aviso.bz/tasks-youtube",
    val isVideoTab: Boolean = false,
    val durationSec: Int = 0,
    val remainingSec: Int = 0
)

