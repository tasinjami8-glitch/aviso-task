package com.example.util

import com.example.model.TaskInfo
import com.example.model.TaskScanResult
import org.json.JSONArray
import org.json.JSONObject

object AvisoTaskParser {

    /**
     * JavaScript code to be injected into Aviso WebView.
     * Evaluates every full site element, task cards, badges, and user session status.
     */
    const val JS_READER_CODE = """
        (function() {
            try {
                var bodyText = document.body ? document.body.innerText : '';
                
                // 1. Detect login status & username/ID
                var isLoggedIn = false;
                var username = '';
                var userMatch = bodyText.match(/Ник:\s*([^\n\r]+)/i) || bodyText.match(/ID:\s*(\d+)/i);
                if (userMatch) {
                    isLoggedIn = true;
                    username = userMatch[1].trim();
                } else if (document.querySelector('a[href*="logout"], a[href*="exit"], .user-avatar, #user-name')) {
                    isLoggedIn = true;
                }

                // 2. Extract Category counters (e.g., "Подписаться на канал 6", "Просмотр видео 1119")
                var subCount = 0;
                var watchCount = 0;
                var likesCount = 0;

                var subMatch = bodyText.match(/Подписаться на канал\s*(\d+)/i) || bodyText.match(/Подписки\s*\(?(\d+)\)?/i);
                if (subMatch) subCount = parseInt(subMatch[1], 10) || 0;

                var watchMatch = bodyText.match(/Просмотр видео\s*(\d+)/i) || bodyText.match(/Просмотры\s*\(?(\d+)\)?/i);
                if (watchMatch) watchCount = parseInt(watchMatch[1], 10) || 0;

                var likesMatch = bodyText.match(/Лайки\s*(\d+)/i);
                if (likesMatch) likesCount = parseInt(likesMatch[1], 10) || 0;

                // 3. Scan specific task elements & cards
                var tasks = [];
                // Look for task rows or cards
                var taskElements = document.querySelectorAll('tr[id^="task_"], div[id^="task_"], .work-serf, [data-task-id], tr.table-task');
                
                if (taskElements.length === 0) {
                    // Fallback to rows containing ruble symbol or task keywords
                    taskElements = document.querySelectorAll('.table tr, .tasks-list > div, .content-task');
                }

                for (var i = 0; i < taskElements.length; i++) {
                    var el = taskElements[i];
                    var text = el.innerText || '';
                    if (text.indexOf('₽') !== -1 || text.indexOf('руб') !== -1 || text.indexOf('сек') !== -1 || text.indexOf('видео') !== -1) {
                        var id = el.id || el.getAttribute('data-task-id') || ('task_' + i);
                        
                        // Extract reward
                        var rewardMatch = text.match(/([\d\.]+\s*(?:₽|руб))/);
                        var reward = rewardMatch ? rewardMatch[1] : '';

                        // Extract duration
                        var timeMatch = text.match(/(\d+\s*сек)/i);
                        var duration = timeMatch ? timeMatch[1] : '';

                        // Title
                        var titleEl = el.querySelector('a, b, strong, .title, .task-title');
                        var title = titleEl ? titleEl.innerText.trim() : text.split('\n')[0].trim();

                        tasks.push({
                            id: id,
                            title: title.substring(0, 100),
                            category: text.indexOf('Подписаться') !== -1 ? 'Subscribe' : 'Watch',
                            reward: reward,
                            duration: duration
                        });
                    }
                }

                var totalCount = Math.max(subCount + watchCount + likesCount, tasks.length);
                if (totalCount === 0 && (subCount > 0 || watchCount > 0)) {
                    totalCount = subCount + watchCount + likesCount;
                }

                // Send back payload to Android bridge
                var payload = {
                    totalTasks: totalCount,
                    subscribeCount: subCount,
                    watchCount: watchCount,
                    likesCount: likesCount,
                    tasks: tasks.slice(0, 50),
                    isLoggedIn: isLoggedIn,
                    username: username,
                    rawStatus: (tasks.length > 0 ? tasks.length + ' tasks visible' : 'No tasks or login needed')
                };

                if (window.AvisoBridge && window.AvisoBridge.onTasksScanned) {
                    window.AvisoBridge.onTasksScanned(JSON.stringify(payload));
                }
            } catch (err) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError(err.toString());
                }
            }
        })();
    """

    fun parseJsonScanResult(jsonString: String): TaskScanResult {
        return try {
            val json = JSONObject(jsonString)
            val totalTasks = json.optInt("totalTasks", 0)
            val subscribeCount = json.optInt("subscribeCount", 0)
            val watchCount = json.optInt("watchCount", 0)
            val likesCount = json.optInt("likesCount", 0)
            val isLoggedIn = json.optBoolean("isLoggedIn", false)
            val username = json.optString("username", "")
            val rawStatus = json.optString("rawStatus", "")

            val taskList = mutableListOf<TaskInfo>()
            val tasksArray = json.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArray.length()) {
                val item = tasksArray.getJSONObject(i)
                taskList.add(
                    TaskInfo(
                        id = item.optString("id", "task_$i"),
                        title = item.optString("title", "YouTube Task"),
                        category = item.optString("category", "Watch"),
                        reward = item.optString("reward", "0.026 ₽"),
                        durationSeconds = item.optString("duration", "")
                    )
                )
            }

            TaskScanResult(
                totalTasks = if (totalTasks > 0) totalTasks else taskList.size,
                subscribeCount = subscribeCount,
                watchCount = watchCount,
                likesCount = likesCount,
                tasks = taskList,
                lastScanTime = System.currentTimeMillis(),
                isLoggedIn = isLoggedIn,
                username = username,
                rawStatus = rawStatus
            )
        } catch (e: Exception) {
            TaskScanResult(
                totalTasks = 0,
                lastScanTime = System.currentTimeMillis(),
                rawStatus = "পাসিং সমস্যা: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Parses raw HTML in background HTTP calls when checking without visible WebView.
     */
    fun parseHtmlBackground(html: String): TaskScanResult {
        var subCount = 0
        var watchCount = 0
        var likesCount = 0
        var isLoggedIn = false
        var username = ""

        // Check login
        if (html.contains("Ник:") || html.contains("ID:") || html.contains("logout") || html.contains("exit")) {
            isLoggedIn = true
            val nickMatch = Regex("Ник:\\s*([^<\\n\\r]+)", RegexOption.IGNORE_CASE).find(html)
            if (nickMatch != null) {
                username = nickMatch.groupValues[1].trim()
            }
        }

        // Parse category numbers
        val subRegex = Regex("Подписаться на канал\\s*(?:<[^>]+>)*\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val subMatch = subRegex.find(html)
        if (subMatch != null) {
            subCount = subMatch.groupValues[1].toIntOrNull() ?: 0
        }

        val watchRegex = Regex("Просмотр видео\\s*(?:<[^>]+>)*\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val watchMatch = watchRegex.find(html)
        if (watchMatch != null) {
            watchCount = watchMatch.groupValues[1].toIntOrNull() ?: 0
        }

        val likesRegex = Regex("Лайки\\s*(?:<[^>]+>)*\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val likesMatch = likesRegex.find(html)
        if (likesMatch != null) {
            likesCount = likesMatch.groupValues[1].toIntOrNull() ?: 0
        }

        // Count occurrences of task rows or ₽ symbols
        val taskRowMatches = Regex("(?:id=\"task_\\d+\"|class=\"[^\"]*work-serf[^\"]*\")").findAll(html).count()
        val total = maxOf(subCount + watchCount + likesCount, taskRowMatches)

        return TaskScanResult(
            totalTasks = total,
            subscribeCount = subCount,
            watchCount = watchCount,
            likesCount = likesCount,
            lastScanTime = System.currentTimeMillis(),
            isLoggedIn = isLoggedIn,
            username = username,
            rawStatus = "Background check: $total tasks"
        )
    }
}
