package com.example.model

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

