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

    /**
     * Checks if any captcha is visible or text present.
     */
    const val JS_CHECK_CAPTCHA = """
        (function() {
            try {
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var textLower = bodyText.toLowerCase();
                var captchaPhrases = ['капч', 'captcha', 'recaptcha', 'hcaptcha', 'я не робот', 'пройдите проверку', 'неверная капча', 'введите код', 'выберите картинку'];
                for (var i = 0; i < captchaPhrases.length; i++) {
                    if (textLower.indexOf(captchaPhrases[i]) !== -1) {
                        if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                            window.AvisoBridge.onCaptchaFound('ক্যাপচা লেখা সনাক্ত: ' + captchaPhrases[i]);
                        }
                        return;
                    }
                }
                var selectors = [
                    'iframe[src*="recaptcha"]', 'iframe[src*="hcaptcha"]', 'iframe[src*="turnstile"]',
                    'iframe[src*="challenges.cloudflare"]', '.g-recaptcha', '.h-captcha', '#captcha',
                    '[name="captcha"]', '.captcha-block', '.captcha_wrap'
                ];
                for (var j = 0; j < selectors.length; j++) {
                    var el = document.querySelector(selectors[j]);
                    if (el && el.offsetParent !== null) {
                        if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                            window.AvisoBridge.onCaptchaFound('ক্যাপচা উইজেট সনাক্ত');
                        }
                        return;
                    }
                }
            } catch (e) { }
        })();
    """

    /**
     * Finds the next uncompleted YouTube task:
     * - Reads duration (e.g. 5 сек, 10 сек, 90 сек).
     * - Clicks the blue task link.
     * - If "Подтвердить просмотр" appears, clicks it.
     * - Reports duration and title to Kotlin bridge.
     */
    const val JS_AUTO_WORK_FIND_AND_CLICK = """
        (function() {
            try {
                // 1. Captcha Check First
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var textLower = bodyText.toLowerCase();
                var captchaPhrases = ['капч', 'captcha', 'recaptcha', 'hcaptcha', 'я не робот', 'пройдите проверку', 'неверная капча'];
                for (var c = 0; c < captchaPhrases.length; c++) {
                    if (textLower.indexOf(captchaPhrases[c]) !== -1) {
                        if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                            window.AvisoBridge.onCaptchaFound('ক্যাপচা সনাক্ত');
                        }
                        return;
                    }
                }
                var captchaEls = document.querySelectorAll('iframe[src*="recaptcha"], iframe[src*="hcaptcha"], iframe[src*="turnstile"], .g-recaptcha, .h-captcha, #captcha');
                for (var ce = 0; ce < captchaEls.length; ce++) {
                    if (captchaEls[ce].offsetParent !== null) {
                        if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                            window.AvisoBridge.onCaptchaFound('ক্যাপচা উইজেট সনাক্ত');
                        }
                        return;
                    }
                }

                // 2. Locate task rows
                var rows = document.querySelectorAll('tr[id^="task_"], div[id^="task_"], .work-serf, [data-task-id], tr.table-task');
                if (rows.length === 0) {
                    rows = document.querySelectorAll('.table tr, .tasks-list > div, .content-task, tbody tr');
                }

                var foundRow = null;
                var durationSec = 10;
                var taskTitle = '';
                var blueLink = null;
                var confirmBtn = null;

                for (var i = 0; i < rows.length; i++) {
                    var row = rows[i];
                    var rText = (row.innerText || '');
                    
                    if (rText.indexOf('₽') === -1 && rText.indexOf('руб') === -1 && rText.indexOf('сек') === -1 && rText.indexOf('видео') === -1) {
                        continue;
                    }

                    if (row.style.display === 'none' || row.classList.contains('task-done') || row.classList.contains('completed')) {
                        continue;
                    }

                    // Extract seconds (e.g. 5 сек, 10 сек, 90 сек)
                    var secMatch = rText.match(/(\d+)\s*(?:сек|sec)/i);
                    var sec = 10;
                    if (secMatch) {
                        sec = parseInt(secMatch[1], 10) || 10;
                    }

                    // Check if row already has "Подтвердить просмотр" or "Приступить к выполнению"
                    var allEls = row.querySelectorAll('*');
                    var existingBtn = null;
                    for (var k = 0; k < allEls.length; k++) {
                        var elT = (allEls[k].innerText || allEls[k].value || '').trim();
                        if (elT.indexOf('Подтвердить просмотр') !== -1 || elT.indexOf('Приступить к выполнению') !== -1) {
                            existingBtn = allEls[k];
                            break;
                        }
                    }

                    // Look for the blue task link in row
                    var links = row.querySelectorAll('a');
                    var candidateLink = null;
                    for (var l = 0; l < links.length; l++) {
                        var lt = (links[l].innerText || '').trim();
                        var href = links[l].getAttribute('href') || '';
                        if (lt === 'Инструкция' || lt.indexOf('жалоб') !== -1 || href.indexOf('delete') !== -1) {
                            continue;
                        }
                        if (lt.length > 0 || links[l].className.indexOf('serf') !== -1 || href.indexOf('javascript') !== -1) {
                            candidateLink = links[l];
                            break;
                        }
                    }

                    if (existingBtn || candidateLink) {
                        foundRow = row;
                        durationSec = sec;
                        taskTitle = (candidateLink ? candidateLink.innerText.trim() : rText.split('\n')[0].trim()).substring(0, 80);
                        blueLink = candidateLink;
                        confirmBtn = existingBtn;
                        break;
                    }
                }

                if (!foundRow) {
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkNoTasks) {
                        window.AvisoBridge.onAutoWorkNoTasks();
                    }
                    return;
                }

                if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskFound) {
                    window.AvisoBridge.onAutoWorkTaskFound(taskTitle || 'YouTube Task', durationSec);
                }

                // If "Подтвердить просмотр" button is already there, click it directly
                if (confirmBtn) {
                    confirmBtn.click();
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                        window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                    }
                } else if (blueLink) {
                    // Click blue link
                    blueLink.click();

                    // Check if clicking blue link revealed "Подтвердить просмотр"
                    setTimeout(function() {
                        var innerEls = foundRow.querySelectorAll('*');
                        for (var m = 0; m < innerEls.length; m++) {
                            var t = (innerEls[m].innerText || innerEls[m].value || '').trim();
                            if (t.indexOf('Подтвердить просмотр') !== -1 || t.indexOf('Приступить к выполнению') !== -1) {
                                innerEls[m].click();
                                break;
                            }
                        }
                        if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                            window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                        }
                    }, 500);
                }
            } catch (err) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError('AutoWork start error: ' + err.toString());
                }
            }
        })();
    """

    /**
     * Clicks "Подтвердить просмотр" (Confirm view) or "Подтвердить" button.
     */
    const val JS_AUTO_WORK_CLICK_CONFIRM = """
        (function() {
            try {
                // Captcha check first
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var textLower = bodyText.toLowerCase();
                var captchaPhrases = ['капч', 'captcha', 'recaptcha', 'hcaptcha', 'я не робот'];
                for (var c = 0; c < captchaPhrases.length; c++) {
                    if (textLower.indexOf(captchaPhrases[c]) !== -1) {
                        if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                            window.AvisoBridge.onCaptchaFound('ক্যাপচা সনাক্ত');
                        }
                        return;
                    }
                }

                var clicked = false;
                var candidates = document.querySelectorAll('button, a, input[type="button"], span, div');
                for (var i = 0; i < candidates.length; i++) {
                    var el = candidates[i];
                    var txt = (el.innerText || el.value || '').trim();
                    if (txt.indexOf('Подтвердить просмотр') !== -1 || txt.indexOf('Подтвердить') !== -1 || txt.indexOf('Приступить к выполнению') !== -1) {
                        el.click();
                        clicked = true;
                        break;
                    }
                }

                if (window.AvisoBridge && window.AvisoBridge.onAutoWorkConfirmClicked) {
                    window.AvisoBridge.onAutoWorkConfirmClicked(clicked);
                }
            } catch (e) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError('Confirm click error: ' + e.toString());
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
