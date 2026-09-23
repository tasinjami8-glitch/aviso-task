package com.example.util

import com.example.model.TaskInfo
import com.example.model.TaskScanResult
import org.json.JSONArray
import org.json.JSONObject

object AvisoTaskParser {

    /**
     * JavaScript verification routine:
     * - Validates whether the current page is a valid Aviso task page.
     * - Detects wrong pages: Balance, Profile, Account, Settings, Dashboard, Finance, Payout, External Ads, Rules, Support, Login.
     * - Verifies whether the expected locked Task ID exists in the DOM.
     * - Returns a JSON object with full safety details.
     */
    const val JS_VERIFY_PAGE_AND_TASK = """
        (function() {
            try {
                var currentUrl = window.location.href || '';
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var lowerUrl = currentUrl.toLowerCase();
                var lowerBody = bodyText.toLowerCase();

                var isWrong = false;
                var wrongReason = '';

                // 1. Check domain
                if (lowerUrl.indexOf('youtube.com') === -1 && lowerUrl.indexOf('youtu.be') === -1 && lowerUrl.indexOf('aviso.bz') === -1) {
                    isWrong = true;
                    wrongReason = 'EXTERNAL_DOMAIN_DETECTED: ' + currentUrl;
                }
                // 2. Check finance / balance / payout sections
                else if (lowerUrl.indexOf('/pay') !== -1 || lowerUrl.indexOf('/finance') !== -1 || lowerUrl.indexOf('/payout') !== -1 || 
                         lowerUrl.indexOf('/deposit') !== -1 || lowerUrl.indexOf('/balance') !== -1 || lowerUrl.indexOf('vivod') !== -1 || lowerUrl.indexOf('popolnit') !== -1) {
                    isWrong = true;
                    wrongReason = 'BALANCE_OR_FINANCE_PAGE';
                }
                // 3. Check profile / user / account sections
                else if (lowerUrl.indexOf('/profile') !== -1 || lowerUrl.indexOf('/user/') !== -1 || lowerUrl.indexOf('/account') !== -1 || lowerUrl.indexOf('/wm/') !== -1) {
                    isWrong = true;
                    wrongReason = 'PROFILE_OR_ACCOUNT_PAGE';
                }
                // 4. Check settings / options
                else if (lowerUrl.indexOf('/settings') !== -1 || lowerUrl.indexOf('/options') !== -1 || lowerUrl.indexOf('/edit') !== -1) {
                    isWrong = true;
                    wrongReason = 'SETTINGS_PAGE';
                }
                // 5. Check dashboard / stats / cabinet
                else if (lowerUrl.indexOf('/dashboard') !== -1 || lowerUrl.indexOf('/cabinet') !== -1 || lowerUrl.indexOf('/stat') !== -1) {
                    isWrong = true;
                    wrongReason = 'DASHBOARD_PAGE';
                }
                // 6. Check login / authentication
                else if (lowerUrl.indexOf('/login') !== -1 || lowerUrl.indexOf('/auth') !== -1 || lowerUrl.indexOf('/register') !== -1) {
                    isWrong = true;
                    wrongReason = 'LOGIN_PAGE';
                }
                // 7. Check rules / support / faq
                else if (lowerUrl.indexOf('/rules') !== -1 || lowerUrl.indexOf('/faq') !== -1 || lowerUrl.indexOf('/support') !== -1) {
                    isWrong = true;
                    wrongReason = 'RULES_OR_SUPPORT_PAGE';
                }
                // 8. Check if not in valid task path
                else if (lowerUrl.indexOf('tasks-youtube') === -1 && lowerUrl.indexOf('/vl/') === -1 && lowerUrl.indexOf('/go/') === -1 && 
                         lowerUrl.indexOf('create_session') === -1 && lowerUrl.indexOf('youtube.com') === -1 && lowerUrl.indexOf('youtu.be') === -1) {
                    isWrong = true;
                    wrongReason = 'UNRECOGNIZED_AVISO_SECTION: ' + currentUrl;
                }

                // Verify active session & task ID if on Aviso
                var expectedTaskId = window._avisoLastTaskId || (window._avisoActiveSession ? window._avisoActiveSession.taskId : '');
                var hasExpectedTask = false;
                var matchedTaskId = '';
                var isConfirmButtonPresent = false;

                if (!isWrong && (lowerUrl.indexOf('tasks-youtube') !== -1 || lowerUrl.indexOf('aviso.bz') !== -1)) {
                    if (expectedTaskId) {
                        var taskEl = document.getElementById(expectedTaskId) || document.querySelector('[data-task-id="' + expectedTaskId + '"]');
                        if (taskEl) {
                            hasExpectedTask = true;
                            matchedTaskId = expectedTaskId;
                            var conf = taskEl.querySelector('button, a, input, [role="button"]');
                            if (conf) isConfirmButtonPresent = true;
                        }
                    }
                }

                var res = {
                    currentUrl: currentUrl,
                    isWrongPage: isWrong,
                    wrongReason: wrongReason,
                    hasExpectedTask: hasExpectedTask,
                    matchedTaskId: matchedTaskId,
                    isTasksYoutubePage: lowerUrl.indexOf('tasks-youtube') !== -1,
                    isConfirmButtonPresent: isConfirmButtonPresent
                };

                if (isWrong && window.AvisoBridge && window.AvisoBridge.onWrongPageDetected) {
                    window.AvisoBridge.onWrongPageDetected(wrongReason, currentUrl);
                }

                return JSON.stringify(res);
            } catch(e) {
                return JSON.stringify({ isWrongPage: false, error: e.toString() });
            }
        })();
    """

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
     * Checks if a genuine, blocking captcha widget is visible on screen.
     * Note: Avoids loose substring matching on document.body to prevent false positives from task titles (e.g. "Без капчи").
     */
    const val JS_CHECK_CAPTCHA = """
        (function() {
            try {
                var selectors = [
                    'iframe[src*="recaptcha/api2/bframe"]',
                    'iframe[src*="hcaptcha.com/checkbox"]',
                    'iframe[src*="challenges.cloudflare"]',
                    '#captcha_block:not([style*="none"])',
                    '#modal_captcha:not([style*="none"])',
                    '.g-recaptcha[data-sitekey]:not([style*="none"])'
                ];
                for (var j = 0; j < selectors.length; j++) {
                    var el = document.querySelector(selectors[j]);
                    if (el && el.offsetParent !== null && (el.offsetWidth > 30 || el.offsetHeight > 30)) {
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
     * Injected immediately into every page.
     * Integrates with Android Tab system & handles popups / new tab requests cleanly.
     */
    const val JS_SETUP_OVERRIDE = """
        (function() {
            try {
                window._avisoLastExtractedSec = 0;
                window._avisoLastTaskRow = null;
                window._avisoLastTaskId = '';
                window._avisoLastTaskLink = null;
                window._avisoLastClickCoords = null;

                function resolveFullUrl(u) {
                    if (!u) return '';
                    try {
                        var dummy = document.createElement('a');
                        dummy.href = u;
                        return dummy.href;
                    } catch(e) {
                        return u;
                    }
                }

                function parseExactDuration(el, rawText) {
                    try {
                        var row = el ? (el.closest ? (el.closest('tr') || el.closest('.work-serf') || el.closest('.task-item') || el.closest('[id^="adv_"]') || el.closest('[id^="task_"]') || el.closest('[id^="bl_"]') || el.closest('div')) : null) : null;
                        
                        function parseFromStr(str) {
                            if (!str) return 0;
                            var s = str.toString().trim();
                            if (!s) return 0;

                            // 1. Min + Sec (e.g. "1 мин 30 сек", "1 min 20 sec", "1m 30s")
                            var mMS = s.match(/(\d+)\s*(?:мин|минут|min|minute|m)[\s.,]+(\d+)\s*(?:сек|секунд|sec|second|s|с|c|cek)/i);
                            if (mMS) {
                                var mn = parseInt(mMS[1], 10) || 0;
                                var sc = parseInt(mMS[2], 10) || 0;
                                var total = mn * 60 + sc;
                                if (total >= 3 && total <= 1800) return total;
                            }

                            // 2. Clock format "01:30", "00:20", "0:05", "00:05"
                            var mClock = s.match(/\b(\d{1,2}):(\d{2})\b/);
                            if (mClock) {
                                var cMin = parseInt(mClock[1], 10) || 0;
                                var cSec = parseInt(mClock[2], 10) || 0;
                                var cTotal = cMin * 60 + cSec;
                                if (cTotal >= 3 && cTotal <= 1800) return cTotal;
                            }

                            // 3. Seconds with various suffixes: "5 сек", "10 сек.", "20 секунд", "15с", "5 с", "10 sec", "20 secs", "5s", "10s", "20s", "5 cek", "10 cek", "20 cek"
                            var mS = s.match(/(\d+)\s*(?:сек|секунд|секунды|секунда|sec|secs|second|seconds|cek|с|s|c)(?:[^\w\dа-яА-ЯёЁ]|$)/i);
                            if (mS) {
                                var valS = parseInt(mS[1], 10) || 0;
                                if (valS >= 3 && valS <= 1800) return valS;
                            }

                            // 4. Minutes: "1 мин", "2 минуты", "1 min", "2 mins"
                            var mM = s.match(/(\d+)\s*(?:мин|минут|минуты|минута|min|mins|minute|minutes)(?:[^\w\dа-яА-ЯёЁ]|$)/i);
                            if (mM) {
                                var valM = parseInt(mM[1], 10) || 0;
                                if (valM >= 1 && valM <= 60) return valM * 60;
                            }

                            // 5. Standalone number inside a dedicated time cell/badge (e.g. "5", "10", "15", "20", "30", "60", "90", "120")
                            var mPure = s.match(/^\s*(\d{1,3})\s*$/);
                            if (mPure) {
                                var pureNum = parseInt(mPure[1], 10) || 0;
                                if (pureNum >= 3 && pureNum <= 600) return pureNum;
                            }

                            return 0;
                        }

                        var candidates = [];
                        if (el) candidates.push(el);
                        if (row) {
                            var allElements = row.querySelectorAll('td, th, span, div, b, font, [class*="time"], [class*="sec"], [class*="timer"], [class*="badge"], [data-sec], [data-time], [data-timer]');
                            for (var i = allElements.length - 1; i >= 0; i--) {
                                candidates.push(allElements[i]);
                            }
                            candidates.push(row);
                        }

                        for (var c = 0; c < candidates.length; c++) {
                            var cand = candidates[c];
                            if (!cand) continue;

                            var dSec = cand.getAttribute ? (cand.getAttribute('data-sec') || cand.getAttribute('data-time') || cand.getAttribute('data-timer') || cand.getAttribute('data-duration') || cand.getAttribute('data-seconds')) : null;
                            if (dSec) {
                                var n1 = parseInt(dSec, 10);
                                if (n1 >= 3 && n1 <= 1800) return n1;
                            }

                            var tAttr = cand.getAttribute ? cand.getAttribute('title') : null;
                            if (tAttr) {
                                var n2 = parseFromStr(tAttr);
                                if (n2 > 0) return n2;
                            }

                            var ocAttr = cand.getAttribute ? cand.getAttribute('onclick') : null;
                            if (ocAttr) {
                                var mOc = ocAttr.match(/(?:start|youtube|watch|session|vl|view|func)[^(]*\(\s*[^,)]*,\s*(\d+)/i) ||
                                          ocAttr.match(/(?:start|youtube|watch|session|vl|view|func)[^(]*\(\s*(\d+)\s*\)/i);
                                if (mOc) {
                                    var n3 = parseInt(mOc[1], 10);
                                    if (n3 >= 3 && n3 <= 1800) return n3;
                                }
                            }

                            var txt = (cand.innerText || cand.textContent || '').trim();
                            if (txt) {
                                var n4 = parseFromStr(txt);
                                if (n4 > 0) return n4;
                            }
                        }

                        if (rawText) {
                            var nRaw = parseFromStr(rawText);
                            if (nRaw > 0) return nRaw;
                        }
                        if (row) {
                            var nRow = parseFromStr(row.innerText || row.textContent || '');
                            if (nRow > 0) return nRow;
                        }
                    } catch(e) {}
                    return 0;
                }

                function extractDur(el) {
                    var d = parseExactDuration(el);
                    return (d && d > 0) ? d : (window._avisoLastExtractedSec || 15);
                }

                // Global click listener to always capture the exact task row, click coordinates, cell, and duration
                document.addEventListener('click', function(e) {
                    try {
                        var el = e.target;
                        if (!el) return;
                        var row = el.closest ? (el.closest('tr') || el.closest('.work-serf') || el.closest('.task-item') || el.closest('[id^="adv_"]') || el.closest('[id^="task_"]') || el.closest('[id^="bl_"]') || el.closest('div')) : null;
                        if (row) {
                            window._avisoLastTaskRow = row;
                            window._avisoLastTaskId = row.id || row.getAttribute('id') || row.getAttribute('data-task-id') || row.getAttribute('data-id') || '';
                            window._avisoLastTaskLink = el;
                            window._avisoLastTaskCell = el.closest ? (el.closest('td') || el.closest('th') || el.parentElement) : el.parentElement;
                            window._avisoLastTaskCellIndex = (window._avisoLastTaskCell && typeof window._avisoLastTaskCell.cellIndex === 'number') ? window._avisoLastTaskCell.cellIndex : -1;

                            try {
                                var rect = el.getBoundingClientRect();
                                window._avisoLastClickCoords = { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
                            } catch(err) {}

                            var parsedSec = parseExactDuration(el, row.innerText);
                            if (parsedSec && parsedSec > 0) {
                                window._avisoLastExtractedSec = parsedSec;
                            }
                        }
                    } catch(err) {}
                }, true);

                function handleOpenUrl(url, duration) {
                    if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) return;
                    var fullUrl = resolveFullUrl(url);
                    var dur = (duration && duration > 0) ? duration : (window._avisoLastExtractedSec || 15);
                    window._avisoLastExtractedSec = null;
                    if (window.AvisoBridge && window.AvisoBridge.openNewTab) {
                        window.AvisoBridge.openNewTab(fullUrl, dur);
                    } else {
                        window.location.href = fullUrl;
                    }
                }

                // Override window.open to delegate to Tab system with dynamic task duration
                window.open = function(url, target, features) {
                    if (url && url !== 'about:blank') {
                        var dynamicDur = window._avisoLastExtractedSec || 15;
                        handleOpenUrl(url, dynamicDur);
                    }
                    return {
                        closed: false,
                        close: function() {},
                        focus: function() {},
                        location: {
                            set href(val) { 
                                var dDur = window._avisoLastExtractedSec || 15;
                                handleOpenUrl(val, dDur); 
                            },
                            get href() { return window.location.href; },
                            replace: function(val) { 
                                var dDur = window._avisoLastExtractedSec || 15;
                                handleOpenUrl(val, dDur); 
                            },
                            assign: function(val) { 
                                var dDur = window._avisoLastExtractedSec || 15;
                                handleOpenUrl(val, dDur); 
                            }
                        },
                        document: {
                            write: function() {},
                            close: function() {}
                        }
                    };
                };
            } catch(e) {}
        })();
    """

    /**
     * Finds the next uncompleted YouTube task:
     * - Clicks the blue task title or row link.
     * - Detects and clicks "Приступить к просмотру" (Start watching) button.
     * - Reports duration and title to Kotlin bridge.
     */
    const val JS_AUTO_WORK_FIND_AND_CLICK = """
        (function() {
            try {
                window._avisoVideoPlayTriggered = false;
                window._avisoVideoPositionReported = false;

                function extractSecondsFromTask(rowOrEl, textOverride) {
                    try {
                        var str = (textOverride || (rowOrEl ? (rowOrEl.innerText || rowOrEl.textContent || '') : '')) || '';
                        
                        // 1. Min + Sec (e.g. "1 мин 30 сек", "1 min 20 sec", "1 min 20 Cek")
                        var mMS = str.match(/(\d+)\s*(?:мин|минут|min|m)[\s.,]+(\d+)\s*(?:[CcСс][EeеЕ][KkкК]|[Сс][еЕ][кК]|cek|сек|секунд|sec|secs|second|seconds|с|s|c)\b/i);
                        if (mMS) {
                            var mn = parseInt(mMS[1], 10) || 0;
                            var sc = parseInt(mMS[2], 10) || 0;
                            var tot = mn * 60 + sc;
                            if (tot >= 3 && tot <= 1800) return tot;
                        }

                        // 2. Exact "Cek" / "сек" / "sec" duration match
                        // Examples: "30 Cek", "60 Cek", "10 Cek", "20 cek", "30 сек", "60 сек", "20 sec"
                        var mS = str.match(/(\d+)\s*(?:[CcСс][EeеЕ][KkкК]|[Сс][еЕ][кК]|cek|сек|секунд|секунды|секунда|sec|secs|second|seconds|с|s|c)\b/i) ||
                                 str.match(/(\d+)\s*(?:cek|сек|секунд|sec|s|c)/i);
                        if (mS) {
                            var valS = parseInt(mS[1], 10) || 0;
                            if (valS >= 3 && valS <= 1800) return valS;
                        }

                        // 3. Minutes only: "1 мин", "2 мин", "1 min"
                        var mM = str.match(/(\d+)\s*(?:мин|минут|минуты|минута|min|mins)(?:[^\w\dа-яА-ЯёЁ]|$)/i);
                        if (mM) {
                            var valM = parseInt(mM[1], 10) || 0;
                            if (valM >= 1 && valM <= 60) return valM * 60;
                        }

                        // 4. Data attributes
                        if (rowOrEl && rowOrEl.getAttribute) {
                            var dSec = rowOrEl.getAttribute('data-sec') || rowOrEl.getAttribute('data-time') || rowOrEl.getAttribute('data-timer') || rowOrEl.getAttribute('data-duration');
                            if (dSec) {
                                var dVal = parseInt(dSec, 10);
                                if (dVal >= 3 && dVal <= 1800) return dVal;
                            }
                        }
                    } catch(e) {}
                    return 0; // Strictly NO DEFAULT TIME!
                }

                function extractDur(el) {
                    var d = extractSecondsFromTask(el);
                    return (d && d > 0) ? d : 0;
                }

                function extractDurationFromRow(rowEl, text) {
                    var d = extractSecondsFromTask(rowEl, text);
                    return (d && d > 0) ? d : 0;
                }

                // Clean, reliable click helper for starting video task
                function safeClick(el) {
                    if (!el) return false;
                    try { el.scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}
                    var aTag = (el.tagName === 'A') ? el : (el.closest ? el.closest('a') : null);
                    var href = (aTag ? aTag.getAttribute('href') : null) || el.getAttribute('href');
                    var dur = extractDur(el);
                    if (dur && dur > 0) {
                        window._avisoLastExtractedSec = dur;
                    }

                    // Track clicked task element and ID without modifying styles
                    var parentRow = el.closest ? (el.closest('tr') || el.closest('.work-serf') || el.closest('.task-item') || el.closest('[id^="adv_"]') || el.closest('[id^="task_"]')) : null;
                    if (parentRow) {
                        window._avisoLastTaskRow = parentRow;
                        window._avisoLastTaskId = parentRow.id || parentRow.getAttribute('id') || parentRow.getAttribute('data-task-id') || parentRow.getAttribute('data-id') || '';
                        window._avisoLastTaskLink = el;
                    }

                    try { el.focus(); } catch(e) {}
                    
                    try {
                        var rect = el.getBoundingClientRect();
                        var cx = rect.left + rect.width / 2;
                        var cy = rect.top + rect.height / 2;
                        var mdown = new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy });
                        var mup = new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy });
                        var mclick = new MouseEvent('click', { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy });
                        el.dispatchEvent(mdown);
                        el.dispatchEvent(mup);
                        el.dispatchEvent(mclick);
                    } catch(e) {}

                    try { el.click(); } catch(e) {}
                    if (aTag && aTag !== el) {
                        try { aTag.click(); } catch(e) {}
                    }

                    if (el.getAttribute && el.getAttribute('onclick')) {
                        try {
                            var fn = new Function(el.getAttribute('onclick'));
                            fn.call(el);
                        } catch(e) {}
                    }

                    if (href && href !== '#' && href.indexOf('javascript:') === -1 &&
                        (href.indexOf('/vl') !== -1 || href.indexOf('/go/') !== -1 || href.indexOf('create_session') !== -1 || href.indexOf('youtube') !== -1 || href.indexOf('youtu.be') !== -1)) {
                        if (window.AvisoBridge && window.AvisoBridge.openNewTab) {
                            window.AvisoBridge.openNewTab(href, dur);
                        } else {
                            window.location.href = (aTag ? aTag.href : null) || href;
                        }
                    }
                    return true;
                }

                function isStartWatchingBtn(el) {
                    if (!el || el.offsetParent === null) return false;
                    var t = (el.innerText || el.value || '').toLowerCase().trim();
                    var cls = (el.className || '').toString().toLowerCase();
                    var oc = (el.getAttribute('onclick') || '').toLowerCase();
                    var href = (el.getAttribute('href') || '').toLowerCase();

                    if (!t && !cls && !oc && !href) return false;
                    if (t === 'посмотреть видео' || t === 'инструкция' || t.indexOf('жалоб') !== -1 || href.indexOf('delete') !== -1) {
                        return false;
                    }

                    if (t.indexOf('приступить к просмотру') !== -1 ||
                        t.indexOf('приступить к выполнению') !== -1 ||
                        t.indexOf('начать просмотр') !== -1 ||
                        t.indexOf('start watching') !== -1 ||
                        t.indexOf('start view') !== -1 ||
                        t.indexOf('start execution') !== -1) {
                        return true;
                    }

                    if (cls.indexOf('btn_play') !== -1 || cls.indexOf('btn-play') !== -1 || cls.indexOf('btn_youtube') !== -1 ||
                        oc.indexOf('start_youtube') !== -1 || oc.indexOf('func_start') !== -1 ||
                        href.indexOf('create_session') !== -1 || href.indexOf('/vl/') !== -1) {
                        return true;
                    }

                    return false;
                }

                // 1. CHECK FOR PENDING CONFIRM VIEW BUTTONS FIRST (HIGHEST PRIORITY)
                // 1. Check if a genuine visible captcha block exists
                var realCaptcha = document.querySelector('iframe[src*="recaptcha/api2/bframe"], iframe[src*="challenges.cloudflare"], #captcha_block, .modal-captcha');
                if (realCaptcha && realCaptcha.offsetParent !== null && realCaptcha.offsetWidth > 30) {
                    if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                        window.AvisoBridge.onCaptchaFound('ক্যাপচা সমাধান প্রয়োজন');
                    }
                    return;
                }

                // 3. Check if current page is already the "Start Watching" Interstitial Screen
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var textLower = bodyText.toLowerCase();
                var isDirectInterstitial = (
                    textLower.indexOf('to count your view') !== -1 ||
                    textLower.indexOf('please watch the video for at least') !== -1 ||
                    textLower.indexOf('start watching') !== -1 ||
                    textLower.indexOf('чтобы засчитать просмотр') !== -1 ||
                    textLower.indexOf('начать просмотр') !== -1
                );
                if (isDirectInterstitial) {
                    var interSec = extractDurationFromRow(null, textLower);
                    var interBtns = document.querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                    for (var ib = 0; ib < interBtns.length; ib++) {
                        var ibTxt = (interBtns[ib].innerText || interBtns[ib].value || '').trim().toLowerCase();
                        if (ibTxt === 'start watching' || ibTxt.indexOf('start watching') !== -1 || ibTxt === 'начать просмотр' || ibTxt.indexOf('приступить к просмотру') !== -1) {
                            safeClick(interBtns[ib]);
                            if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                                window.AvisoBridge.onInterstitialHandled(interSec);
                            }
                            if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                                window.AvisoBridge.onAutoWorkTaskStarted(interSec);
                            }
                            return;
                        }
                    }
                }

                // 4. Check if ANY "Приступить к просмотру" button is ALREADY visible on the page right now
                var allClickables = document.querySelectorAll('button, a, input[type="button"], span, div');
                var globalStartBtn = null;
                var globalStartRow = null;
                for (var a = 0; a < allClickables.length; a++) {
                    var btnEl = allClickables[a];
                    if (isStartWatchingBtn(btnEl)) {
                        globalStartBtn = btnEl;
                        globalStartRow = btnEl.closest('tr, div.work-serf, table.work-serf, div[id^="adv_"], div[id^="bl_"], tr[id^="adv_"]') || btnEl.parentElement;
                        break;
                    }
                }
                if (globalStartBtn) {
                    var gDur = extractDurationFromRow(globalStartRow, globalStartRow ? globalStartRow.innerText : '');
                    safeClick(globalStartBtn);
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                        window.AvisoBridge.onAutoWorkTaskStarted(gDur);
                    }
                    return;
                }

                // 5. Locate next uncompleted task row
                var rows = document.querySelectorAll('tr[id^="adv_"], div[id^="adv_"], table[id^="adv_"], tr[id^="task_"], div[id^="task_"], tr[id^="bl_"], div[id^="bl_"], .work-serf, .work-youtube, [data-task-id], tr.table-task, tbody tr, tr');

                var foundRow = null;
                var durationSec = 10;
                var taskTitle = '';
                var targetLink = null;
                var startBtnAlreadyVisible = null;

                for (var i = 0; i < rows.length; i++) {
                    var row = rows[i];
                    var rText = (row.innerText || '');
                    
                    if (rText.indexOf('₽') === -1 && rText.indexOf('руб') === -1 && rText.indexOf('сек') === -1 && rText.indexOf('видео') === -1) {
                        continue;
                    }

                    if (row.style.display === 'none' || row.getAttribute('data-completed') === 'true' || row.classList.contains('task-done') || row.classList.contains('completed') || rText.indexOf('Задание выполнено') !== -1 || rText.indexOf('выполнено') !== -1) {
                        continue;
                    }

                    // Step 4 & 5: Look at the RIGHT side of the selected task row and read exact viewing time
                    var sec = 0;
                    if (row.cells && row.cells.length > 0) {
                        for (var c = row.cells.length - 1; c >= 0; c--) {
                            var cellText = (row.cells[c].innerText || row.cells[c].textContent || '').trim();
                            var parsedS = extractSecondsFromTask(row.cells[c], cellText);
                            if (parsedS && parsedS > 0) {
                                sec = parsedS;
                                break;
                            }
                        }
                    }
                    if (!sec || sec <= 0) {
                        sec = extractDurationFromRow(row, rText);
                    }

                    // Strict: If timer cannot be read, DO NOT GUESS -> skip this task
                    if (!sec || sec <= 0) {
                        continue;
                    }

                    // Step 6: From the SAME task row, locate the YouTube video link on the LEFT side
                    var rowEls = row.querySelectorAll('button, a, input[type="button"], span, div');
                    for (var re = 0; re < rowEls.length; re++) {
                        if (isStartWatchingBtn(rowEls[re])) {
                            startBtnAlreadyVisible = rowEls[re];
                            break;
                        }
                    }

                    // Look for the clickable task title/link on the LEFT side of row
                    // (Strictly avoid advertiser user profiles, instructions, complaints, delete)
                    var candidateLink = null;
                    if (row.cells && row.cells.length > 0) {
                        // Priority search in left-side cells (cells 0, 1)
                        for (var lc = 0; lc < Math.min(row.cells.length, 3); lc++) {
                            var leftLinks = row.cells[lc].querySelectorAll('a, span[onclick], div[onclick], .title, .task-title, .work-title, a.serf-url');
                            for (var ll = 0; ll < leftLinks.length; ll++) {
                                var lEl = leftLinks[ll];
                                var lt = (lEl.innerText || '').trim();
                                var href = (lEl.getAttribute('href') || '').toLowerCase();
                                var oc = (lEl.getAttribute('onclick') || '').toLowerCase();
                                if (lt === 'Инструкция' || lt.indexOf('жалоб') !== -1 || href.indexOf('delete') !== -1 ||
                                    href.indexOf('/user') !== -1 || href.indexOf('/profile') !== -1 || href.indexOf('/wm/') !== -1 ||
                                    lt === 'Посмотреть видео') {
                                    continue;
                                }
                                candidateLink = lEl;
                                break;
                            }
                            if (candidateLink) break;
                        }
                    }

                    if (!candidateLink) {
                        var links = row.querySelectorAll('a, span[onclick], div[onclick], .title, .task-title, .work-title, a.serf-url');
                        for (var l = 0; l < links.length; l++) {
                            var lt = (links[l].innerText || '').trim();
                            var href = (links[l].getAttribute('href') || '').toLowerCase();
                            var oc = (links[l].getAttribute('onclick') || '').toLowerCase();
                            var cls = (links[l].className || '').toString().toLowerCase();

                            if (lt === 'Инструкция' || lt.indexOf('жалоб') !== -1 || href.indexOf('delete') !== -1 ||
                                href.indexOf('/user') !== -1 || href.indexOf('/profile') !== -1 || href.indexOf('/wm/') !== -1 ||
                                lt === 'Посмотреть видео') {
                                continue;
                            }

                            if (cls.indexOf('serf-url') !== -1 || cls.indexOf('title') !== -1 ||
                                oc.indexOf('start') !== -1 || oc.indexOf('func') !== -1 ||
                                href.indexOf('youtube') !== -1 || href.indexOf('youtu.be') !== -1 ||
                                href.indexOf('/go/') !== -1 || href.indexOf('/vl/') !== -1 ||
                                lt.length > 2) {
                                candidateLink = links[l];
                                break;
                            }
                        }
                    }

                    // Strict: If video link is missing and no start button is visible, skip that task!
                    if (!startBtnAlreadyVisible && !candidateLink) {
                        continue;
                    }

                    if (startBtnAlreadyVisible || candidateLink) {
                        foundRow = row;
                        window._avisoLastTaskRow = row;
                        durationSec = sec;
                        taskTitle = (candidateLink ? candidateLink.innerText.trim() : rText.split('\n')[0].trim()).substring(0, 80);
                        targetLink = candidateLink;
                        break;
                    }
                }

                if (!foundRow || (!startBtnAlreadyVisible && !targetLink)) {
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkNoTasks) {
                        window.AvisoBridge.onAutoWorkNoTasks();
                    }
                    return;
                }

                if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskFound) {
                    window.AvisoBridge.onAutoWorkTaskFound(taskTitle || 'YouTube Task', durationSec);
                }

                // If "Приступить к просмотру" is ALREADY visible in this row, click it immediately!
                if (startBtnAlreadyVisible) {
                    safeClick(startBtnAlreadyVisible);
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                        window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                    }
                    return;
                }

                // Otherwise, click the task link to reveal "Приступить к просмотру"
                safeClick(targetLink);

                // Poll for "Приступить к просмотру" or direct navigation
                var pollAttempts = 0;
                var maxAttempts = 24;
                var pollTimer = setInterval(function() {
                    pollAttempts++;
                    var curText = (document.body ? document.body.innerText : '') || '';
                    var curLower = curText.toLowerCase();

                    // Check if Interstitial appeared
                    if (curLower.indexOf('to count your view') !== -1 || curLower.indexOf('please watch the video') !== -1 || curLower.indexOf('start watching') !== -1 || curLower.indexOf('начать просмотр') !== -1) {
                        clearInterval(pollTimer);
                        var dur = extractDuration(curText);
                        var sbEls = document.querySelectorAll('button, a, input[type="button"], span, div');
                        for (var sb = 0; sb < sbEls.length; sb++) {
                            var sTxt = (sbEls[sb].innerText || sbEls[sb].value || '').trim().toLowerCase();
                            if (sTxt === 'start watching' || sTxt.indexOf('start watching') !== -1 || sTxt === 'начать просмотр' || sTxt.indexOf('приступить к просмотру') !== -1) {
                                safeClick(sbEls[sb]);
                                if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                                    window.AvisoBridge.onInterstitialHandled(dur);
                                }
                                if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                                    window.AvisoBridge.onAutoWorkTaskStarted(dur);
                                }
                                return;
                            }
                        }
                        return;
                    }

                    // Check if "Приступить к просмотру" appeared in the row
                    var rowBtns = foundRow.querySelectorAll('button, a, input[type="button"], span, div');
                    var foundAction = false;
                    for (var rb = 0; rb < rowBtns.length; rb++) {
                        var rEl = rowBtns[rb];
                        if (isStartWatchingBtn(rEl)) {
                            clearInterval(pollTimer);
                            safeClick(rEl);
                            foundAction = true;
                            if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                                window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                            }
                            return;
                        }
                    }

                    // Check if any global button appeared
                    if (!foundAction) {
                        var gBtns = document.querySelectorAll('button, a, input[type="button"], span, div');
                        for (var gb = 0; gb < gBtns.length; gb++) {
                            var gEl = gBtns[gb];
                            if (isStartWatchingBtn(gEl)) {
                                clearInterval(pollTimer);
                                safeClick(gEl);
                                foundAction = true;
                                if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                                    window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                                }
                                return;
                            }
                        }
                    }

                    // Check if page navigated directly
                    if (window.location.href.indexOf('/vl') !== -1 || window.location.href.indexOf('/go/') !== -1 || window.location.href.indexOf('youtube') !== -1 || window.location.href.indexOf('create_session') !== -1) {
                        clearInterval(pollTimer);
                        if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                            window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                        }
                        return;
                    }

                    if (pollAttempts >= maxAttempts) {
                        clearInterval(pollTimer);
                        // Trigger task started as fallback if page is about to load
                        if (window.location.href.indexOf('tasks-youtube') === -1) {
                            if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                                window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                            }
                        } else {
                            if (window.AvisoBridge && window.AvisoBridge.onError) {
                                window.AvisoBridge.onError('ভিডিও লিঙ্ক বা বোতাম খুঁজে পাওয়া যায়নি');
                            }
                        }
                    }
                }, 150);

            } catch (err) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError('AutoWork start error: ' + err.toString());
                }
            }
        })();
    """

    /**
     * VIDEO START RULE:
     * When the selected video/page opens:
     * 1. Wait for the page to load.
     * 2. If a visible “Start” button appears, click that Start button.
     * 3. Do not click unrelated buttons.
     * 4. If Start does not appear, continue according to the existing workflow.
     * 5. Do not restart the task unnecessarily.
     */
    const val JS_CLICK_START_BUTTON_ON_VIDEO_PAGE = """
        (function() {
            try {
                function isVisible(el) {
                    if (!el) return false;
                    try {
                        var style = window.getComputedStyle ? window.getComputedStyle(el) : null;
                        if (style && (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0')) return false;
                    } catch(e) {}
                    return (el.offsetParent !== null || el.offsetWidth > 0 || el.offsetHeight > 0);
                }

                function isStartBtn(el) {
                    if (!isVisible(el)) return false;
                    var t = (el.innerText || el.textContent || el.value || el.getAttribute('aria-label') || '').trim().toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    var id = (el.id || '').toLowerCase();

                    // Do not click unrelated buttons
                    if (t.indexOf('cancel') !== -1 || t.indexOf('delete') !== -1 || t.indexOf('отмена') !== -1 || t.indexOf('жалоб') !== -1 || t.indexOf('инструкция') !== -1) {
                        return false;
                    }

                    // Direct "Start" button checks
                    if (t === 'start' ||
                        t === 'start watching' ||
                        t === 'start view' ||
                        t === 'start video' ||
                        t === 'начать' ||
                        t === 'начать просмотр' ||
                        t === 'приступить к просмотру' ||
                        t.indexOf('start watching') !== -1 ||
                        t.indexOf('начать просмотр') !== -1 ||
                        t.indexOf('приступить к просмотру') !== -1) {
                        return true;
                    }

                    // Video play / start overlays
                    if (id === 'start_video' || id === 'video-click' || cls.indexOf('btn_play') !== -1 || cls.indexOf('btn-play') !== -1 || cls.indexOf('ytp-large-play-button') !== -1) {
                        return true;
                    }

                    return false;
                }

                var docs = [document];
                try {
                    for (var f = 0; f < window.frames.length; f++) {
                        try {
                            if (window.frames[f] && window.frames[f].document) docs.push(window.frames[f].document);
                        } catch(e) {}
                    }
                } catch(e) {}

                var clicked = false;
                for (var d = 0; d < docs.length; d++) {
                    var candidates = docs[d].querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], .btn, [class*="play"], [class*="start"]');
                    for (var i = 0; i < candidates.length; i++) {
                        if (isStartBtn(candidates[i])) {
                            try { candidates[i].scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}
                            try { candidates[i].focus(); } catch(e) {}
                            try {
                                var rect = candidates[i].getBoundingClientRect();
                                var cx = rect.left + rect.width / 2;
                                var cy = rect.top + rect.height / 2;
                                var opts = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy };
                                candidates[i].dispatchEvent(new MouseEvent('mousedown', opts));
                                candidates[i].dispatchEvent(new MouseEvent('mouseup', opts));
                                candidates[i].dispatchEvent(new MouseEvent('click', opts));
                            } catch(e) {}
                            try { candidates[i].click(); } catch(e) {}
                            clicked = true;
                            if (window.AvisoBridge && window.AvisoBridge.onAutomationLog) {
                                window.AvisoBridge.onAutomationLog('[Video] Clicked visible Start button on opened page.');
                            }
                            break;
                        }
                    }
                    if (clicked) break;
                }
            } catch(e) {}
        })();
    """

    /**
     * Active watcher script that runs continuously during video viewing:
     * 1. Detects video player / iframe and triggers playback ONCE so it plays continuously without pause interruption.
     * 2. Scans all frames and documents for Aviso's real countdown timer.
     * 3. Scans all frames and documents for Aviso's "Подтвердить просмотр" (Confirm View) button.
     * 4. When countdown reaches 0 and button appears, scrolls it into view and clicks it.
     */
    const val JS_START_AND_WATCH_VIDEO = """
        (function() {
            try {
                function getAllDocs() {
                    var docs = [document];
                    try {
                        for (var f = 0; f < window.frames.length; f++) {
                            try {
                                if (window.frames[f] && window.frames[f].document) {
                                    docs.push(window.frames[f].document);
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                    try {
                        var ifrs = document.querySelectorAll('iframe, frame');
                        for (var i = 0; i < ifrs.length; i++) {
                            try {
                                var doc = ifrs[i].contentDocument || (ifrs[i].contentWindow && ifrs[i].contentWindow.document);
                                if (doc && docs.indexOf(doc) === -1) {
                                    docs.push(doc);
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                    return docs;
                }

                function safeClick(el) {
                    if (!el) return false;
                    try { el.scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}
                    try { el.focus(); } catch(e) {}
                    try {
                        var rect = el.getBoundingClientRect();
                        var cx = (rect.left + rect.width / 2) || 100;
                        var cy = (rect.top + rect.height / 2) || 100;
                        var opts = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy };
                        el.dispatchEvent(new PointerEvent('pointerdown', opts));
                        el.dispatchEvent(new MouseEvent('mousedown', opts));
                        el.dispatchEvent(new PointerEvent('pointerup', opts));
                        el.dispatchEvent(new MouseEvent('mouseup', opts));
                        el.dispatchEvent(new MouseEvent('click', opts));
                    } catch(e) {}
                    try { el.click(); } catch(e) {}
                    var aParent = el.closest ? el.closest('a, button') : null;
                    if (aParent && aParent !== el) {
                        try { aParent.click(); } catch(e) {}
                    }
                    if (el.getAttribute && el.getAttribute('onclick')) {
                        try {
                            var fn = new Function(el.getAttribute('onclick'));
                            fn.call(el);
                        } catch(e) {}
                    }
                    return true;
                }

                function isConfirmBtn(el) {
                    if (!el) return false;
                    var t = (el.innerText || el.textContent || el.value || '').trim().toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    var oc = (el.getAttribute('onclick') || '').toLowerCase();
                    var id = (el.id || '').toLowerCase();

                    if (!t && !cls && !oc && !id) return false;

                    if (t.indexOf('отмена') !== -1 || t.indexOf('cancel') !== -1 || t.indexOf('жалоб') !== -1 || t.indexOf('delete') !== -1 || t.indexOf('удалить') !== -1) {
                        return false;
                    }

                    if (t.indexOf('подтвердить просмотр') !== -1 ||
                        t.indexOf('подтвердить') !== -1 ||
                        t.indexOf('подтверждаю') !== -1 ||
                        t.indexOf('проверить просмотр') !== -1 ||
                        t.indexOf('проверить выполнение') !== -1 ||
                        t.indexOf('проверить задание') !== -1 ||
                        t.indexOf('проверить') !== -1 ||
                        t.indexOf('забрать награду') !== -1 ||
                        t.indexOf('забрать деньги') !== -1 ||
                        t.indexOf('забрать') !== -1 ||
                        t.indexOf('получить вознаграждение') !== -1 ||
                        t.indexOf('получить награду') !== -1 ||
                        t.indexOf('получить деньги') !== -1 ||
                        t.indexOf('получить оплату') !== -1 ||
                        t.indexOf('получить') !== -1 ||
                        t.indexOf('клик для подтверждения') !== -1 ||
                        t.indexOf('нажмите для подтверждения') !== -1 ||
                        t.indexOf('кликните для подтверждения') !== -1 ||
                        t.indexOf('засчитать просмотр') !== -1 ||
                        t.indexOf('засчитать') !== -1 ||
                        t.indexOf('просмотр засчитан') !== -1 ||
                        t.indexOf('я просмотрел видео') !== -1 ||
                        t.indexOf('завершить') !== -1 ||
                        t.indexOf('confirm view') !== -1 ||
                        t.indexOf('confirm') !== -1 ||
                        t.indexOf('verify view') !== -1 ||
                        t.indexOf('verify execution') !== -1 ||
                        t.indexOf('verify') !== -1 ||
                        t.indexOf('check view') !== -1 ||
                        t.indexOf('check task') !== -1 ||
                        t.indexOf('check') !== -1 ||
                        t.indexOf('claim reward') !== -1 ||
                        t.indexOf('claim') !== -1 ||
                        t.indexOf('get reward') !== -1 ||
                        t.indexOf('get money') !== -1 ||
                        t.indexOf('click to confirm') !== -1 ||
                        t.indexOf('tap to confirm') !== -1 ||
                        t.indexOf('finish') !== -1 ||
                        t.indexOf('complete') !== -1 ||
                        t.indexOf('নিশ্চিত করুন') !== -1 ||
                        t.indexOf('যাচাই করুন') !== -1 ||
                        t.indexOf('ভিউ নিশ্চিত') !== -1) {
                        return true;
                    }

                    if (cls.indexOf('btn_confirm') !== -1 ||
                        cls.indexOf('btn_check') !== -1 ||
                        cls.indexOf('confirm-btn') !== -1 ||
                        cls.indexOf('btn_success') !== -1 ||
                        cls.indexOf('btn-success') !== -1 ||
                        cls.indexOf('btn_youtube') !== -1 ||
                        cls.indexOf('btn-verify') !== -1 ||
                        cls.indexOf('btn_verify') !== -1 ||
                        oc.indexOf('confirm') !== -1 ||
                        oc.indexOf('check_task') !== -1 ||
                        oc.indexOf('check_adv') !== -1 ||
                        oc.indexOf('func_check') !== -1 ||
                        oc.indexOf('get_money') !== -1 ||
                        oc.indexOf('confirm_view') !== -1 ||
                        oc.indexOf('verify') !== -1 ||
                        id.indexOf('confirm') !== -1 ||
                        id.indexOf('btn_check') !== -1 ||
                        id.indexOf('btn-check') !== -1 ||
                        id.indexOf('check') !== -1) {
                        return true;
                    }
                    return false;
                }

                // 1. Check for genuine blocking captcha iframe / widget only
                var realCaptcha = document.querySelector('iframe[src*="recaptcha/api2/bframe"], iframe[src*="challenges.cloudflare"], #captcha_block, .modal-captcha');
                if (realCaptcha && realCaptcha.offsetParent !== null && realCaptcha.offsetWidth > 30) {
                    if (window.AvisoBridge && window.AvisoBridge.onCaptchaFound) {
                        window.AvisoBridge.onCaptchaFound('ক্যাপচা সমাধান প্রয়োজন');
                    }
                    return;
                }

                var docs = getAllDocs();
                var combinedBodyText = '';
                for (var d = 0; d < docs.length; d++) {
                    try {
                        combinedBodyText += ' ' + ((docs[d].body ? docs[d].body.innerText : '') || '');
                    } catch(e) {}
                }
                var textLower = combinedBodyText.toLowerCase();

                // 1b. Check and handle "Start Watching" Interstitial Screen if present
                if (textLower.indexOf('to count your view') !== -1 || textLower.indexOf('please watch the video for at least') !== -1 || textLower.indexOf('start watching') !== -1 || textLower.indexOf('начать просмотр') !== -1 || textLower.indexOf('приступить к просмотру') !== -1) {
                    var interSec = 0;
                    var mMS = textLower.match(/(\d+)\s*(?:minute|min|минут)[s]?\s*(?:and\s*)?(\d+)\s*(?:second|sec|секунд)[s]?/i);
                    if (mMS) {
                        interSec = (parseInt(mMS[1], 10) || 0) * 60 + (parseInt(mMS[2], 10) || 0);
                    }
                    if (interSec === 0) {
                        var mM = textLower.match(/(\d+)\s*(?:minute|min|минут)[s]?/i);
                        if (mM) interSec = (parseInt(mM[1], 10) || 0) * 60;
                    }
                    if (interSec === 0) {
                        var mS = textLower.match(/(\d+)\s*(?:second|sec|секунд)[s]?/i);
                        if (mS) interSec = parseInt(mS[1], 10) || 0;
                    }
                    if (interSec <= 0) interSec = 40;

                    for (var d0 = 0; d0 < docs.length; d0++) {
                        var startWatchBtns = docs[d0].querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                        for (var swb = 0; swb < startWatchBtns.length; swb++) {
                            var swTxt = (startWatchBtns[swb].innerText || startWatchBtns[swb].value || '').trim().toLowerCase();
                            if (swTxt === 'start watching' || swTxt.indexOf('start watching') !== -1 || swTxt === 'начать просмотр' || swTxt.indexOf('начать просмотр') !== -1 || swTxt.indexOf('приступить к просмотру') !== -1) {
                                safeClick(startWatchBtns[swb]);
                                if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                                    window.AvisoBridge.onInterstitialHandled(interSec);
                                }
                                break;
                            }
                        }
                    }
                }

                // 2. Play HTML5 <video> if present and currently paused
                for (var d1 = 0; d1 < docs.length; d1++) {
                    var vids = docs[d1].querySelectorAll('video');
                    for (var v = 0; v < vids.length; v++) {
                        try {
                            vids[v].muted = false;
                            if (vids[v].paused) {
                                vids[v].play().catch(function(e){});
                            }
                        } catch(e) {}
                    }
                }

                // 3. YouTube Iframes and Video Embeds: Continuously trigger playback & unmuting
                var iframes = document.querySelectorAll('iframe[src*="youtube"], iframe[src*="youtu.be"], iframe#video-click, iframe');
                for (var f = 0; f < iframes.length; f++) {
                    var ifr = iframes[f];
                    try {
                        ifr.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                        ifr.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                        ifr.contentWindow.postMessage('{"event":"command","func":"setVolume","args":[100]}', '*');
                        ifr.contentWindow.postMessage('{"event":"listening","id":1,"channel":"widget"}', '*');
                    } catch(e) {}
                }

                // Click play overlay / buttons with full simulated touch & mouse events
                for (var dPlay = 0; dPlay < docs.length; dPlay++) {
                    var startButtons = docs[dPlay].querySelectorAll('#video-click, .video-click, #start_video, .start-video, .ytp-large-play-button, button.ytp-large-play-button, .ytp-play-button, button.btn-play, [class*="play_btn"], a[onclick*="start"], button[onclick*="start"], .ytp-cuj-button, .video-stream, button[aria-label*="Play"], button[aria-label*="Воспроизвести"], [aria-label*="Play"], [aria-label*="Воспроизвести"]');
                    for (var s = 0; s < startButtons.length; s++) {
                        try {
                            var btn = startButtons[s];
                            if (btn && btn.offsetParent !== null) {
                                safeClick(btn);
                            }
                        } catch(e) {}
                    }
                }

                // 4. Read Real Aviso Countdown Timer across all frames & document.title
                var realSec = -1;

                // Check document.title (Aviso updates title e.g. "20 сек - Просмотр" or "15 sec")
                var docTitle = (document.title || '');
                var mTitle = docTitle.match(/^(\d+)\s*(?:сек|sec|s\b)/i) || docTitle.match(/(\d+)\s*(?:сек|sec|s\b)/i);
                if (mTitle) {
                    realSec = parseInt(mTitle[1], 10);
                }

                if (realSec === -1) {
                    for (var d2 = 0; d2 < docs.length; d2++) {
                        var curDoc = docs[d2];
                        var timerEls = curDoc.querySelectorAll('#tmr, #timer, .timer, #sec, #sec-time, span[id*="tmr"], span[id*="sec"], span[id*="time"], div[id*="timer"], div[id*="tmr"], div[id*="sec"], .time_block, .time-count, .timer-block, #block-timer, [data-timer], [data-time], .badge-timer');
                        for (var t = 0; t < timerEls.length; t++) {
                            var txt = (timerEls[t].innerText || timerEls[t].textContent || '').trim();
                            var m = txt.match(/^(\d+)$/) || txt.match(/(\d+)/);
                            if (m) {
                                realSec = parseInt(m[1], 10);
                                break;
                            }
                        }
                        if (realSec !== -1) break;
                        var bTxt = (curDoc.body ? curDoc.body.innerText : '') || '';
                        var m2 = bTxt.match(/Осталось:\s*(\d+)\s*сек/i) || bTxt.match(/Осталось\s*(\d+)\s*сек/i) || bTxt.match(/Таймер:\s*(\d+)/i) || bTxt.match(/(\d+)\s*сек(?:унд)?/i) || bTxt.match(/(\d+)\s*sec/i);
                        if (m2) {
                            realSec = parseInt(m2[1], 10);
                            break;
                        }
                    }
                }

                if (window.AvisoBridge && window.AvisoBridge.onRealTimerUpdate) {
                    window.AvisoBridge.onRealTimerUpdate(realSec);
                }
            } catch(e) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError('Watch video error: ' + e.toString());
                }
            }
        })();
    """

    /**
     * Clicks "Подтвердить просмотр" (Confirm view), "Проверить" (Check/Verify), or "Подтвердить" button:
     * - Checks all frames and documents with full event dispatch.
     * - Reports success to Kotlin bridge.
     */
    const val JS_AUTO_WORK_CLICK_CONFIRM = """
        (function() {
            try {
                function getAllDocs() {
                    var docs = [document];
                    try {
                        for (var f = 0; f < window.frames.length; f++) {
                            try {
                                if (window.frames[f] && window.frames[f].document) {
                                    docs.push(window.frames[f].document);
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                    try {
                        var ifrs = document.querySelectorAll('iframe, frame');
                        for (var i = 0; i < ifrs.length; i++) {
                            try {
                                var doc = ifrs[i].contentDocument || (ifrs[i].contentWindow && ifrs[i].contentWindow.document);
                                if (doc && docs.indexOf(doc) === -1) {
                                    docs.push(doc);
                                }
                            } catch(e) {}
                        }
                    } catch(e) {}
                    return docs;
                }

                function safeClick(el) {
                    if (!el) return false;
                    try { el.scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}
                    try { el.focus(); } catch(e) {}

                    // 1. Highlight visually
                    try {
                        el.style.outline = '4px solid #22c55e';
                        el.style.boxShadow = '0 0 25px rgba(34, 197, 94, 0.9)';
                        el.style.borderRadius = '8px';
                        el.style.backgroundColor = 'rgba(34, 197, 94, 0.3)';
                    } catch(e) {}

                    // 2. Direct onclick property execution
                    try {
                        if (typeof el.onclick === 'function') {
                            el.onclick.call(el, { target: el, currentTarget: el, preventDefault: function(){}, stopPropagation: function(){} });
                        }
                    } catch(e) {}

                    // 3. Evaluate onclick attribute string in window scope
                    try {
                        var oc = el.getAttribute ? el.getAttribute('onclick') : null;
                        if (oc) {
                            var cleanOc = oc.replace(/^javascript:/i, '').replace(/;\s*return\s+false\s*;?/i, '');
                            window.eval(cleanOc);
                        }
                    } catch(e) {}

                    // 4. Native click()
                    try { el.click(); } catch(e) {}

                    // 5. Full Mouse & Pointer & Touch Event dispatch
                    try {
                        var rect = el.getBoundingClientRect();
                        var cx = (rect.left + rect.width / 2) || 100;
                        var cy = (rect.top + rect.height / 2) || 100;
                        var opts = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy };

                        try {
                            var touchObj = new Touch({ identifier: Date.now(), target: el, clientX: cx, clientY: cy, pageX: cx, pageY: cy });
                            el.dispatchEvent(new TouchEvent('touchstart', { cancelable: true, bubbles: true, touches: [touchObj], targetTouches: [touchObj], changedTouches: [touchObj] }));
                            el.dispatchEvent(new TouchEvent('touchend', { cancelable: true, bubbles: true, touches: [], targetTouches: [], changedTouches: [touchObj] }));
                        } catch(te) {}

                        el.dispatchEvent(new PointerEvent('pointerdown', opts));
                        el.dispatchEvent(new MouseEvent('mousedown', opts));
                        el.dispatchEvent(new PointerEvent('pointerup', opts));
                        el.dispatchEvent(new MouseEvent('mouseup', opts));
                        el.dispatchEvent(new MouseEvent('click', opts));
                    } catch(e) {}

                    // 6. Click parents (e.g. <a>, <button>, <span>, <tr>)
                    var cur = el.parentElement;
                    var levels = 0;
                    while (cur && levels < 4) {
                        try { cur.click(); } catch(e) {}
                        if (typeof cur.onclick === 'function') {
                            try { cur.onclick.call(cur, { target: cur, currentTarget: cur, preventDefault: function(){}, stopPropagation: function(){} }); } catch(e) {}
                        }
                        var pOc = cur.getAttribute ? cur.getAttribute('onclick') : null;
                        if (pOc) {
                            try { window.eval(pOc.replace(/^javascript:/i, '').replace(/;\s*return\s+false\s*;?/i, '')); } catch(e) {}
                        }
                        cur = cur.parentElement;
                        levels++;
                    }

                    return true;
                }

                function isConfirmBtn(el) {
                    if (!el) return false;
                    var t = (el.innerText || el.textContent || el.value || '').trim().toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    var oc = (el.getAttribute('onclick') || '').toLowerCase();
                    var id = (el.id || '').toLowerCase();
                    var role = (el.getAttribute('role') || '').toLowerCase();

                    if (!t && !cls && !oc && !id) return false;

                    // Exclude negative / non-confirm elements
                    if (t.indexOf('отмена') !== -1 || t.indexOf('cancel') !== -1 || t.indexOf('жалоб') !== -1 || t.indexOf('delete') !== -1 || t.indexOf('удалить') !== -1 || t.indexOf('правила') !== -1) {
                        return false;
                    }

                    // 1. Russian keywords (entire page search)
                    if (t.indexOf('подтвердить просмотр') !== -1 ||
                        t.indexOf('подтвердить') !== -1 ||
                        t.indexOf('подтверждаю') !== -1 ||
                        t.indexOf('проверить просмотр') !== -1 ||
                        t.indexOf('проверить выполнение') !== -1 ||
                        t.indexOf('проверить задание') !== -1 ||
                        t.indexOf('проверить') !== -1 ||
                        t.indexOf('забрать награду') !== -1 ||
                        t.indexOf('забрать деньги') !== -1 ||
                        t.indexOf('забрать') !== -1 ||
                        t.indexOf('получить вознаграждение') !== -1 ||
                        t.indexOf('получить награду') !== -1 ||
                        t.indexOf('получить деньги') !== -1 ||
                        t.indexOf('получить оплату') !== -1 ||
                        t.indexOf('получить') !== -1 ||
                        t.indexOf('клик для подтверждения') !== -1 ||
                        t.indexOf('нажмите для подтверждения') !== -1 ||
                        t.indexOf('кликните для подтверждения') !== -1 ||
                        t.indexOf('засчитать просмотр') !== -1 ||
                        t.indexOf('засчитать') !== -1 ||
                        t.indexOf('просмотр засчитан') !== -1 ||
                        t.indexOf('я просмотрел видео') !== -1 ||
                        t.indexOf('завершить') !== -1) {
                        return true;
                    }

                    // 2. English keywords
                    if (t.indexOf('confirm view') !== -1 ||
                        t.indexOf('confirm') !== -1 ||
                        t.indexOf('verify view') !== -1 ||
                        t.indexOf('verify execution') !== -1 ||
                        t.indexOf('verify') !== -1 ||
                        t.indexOf('check view') !== -1 ||
                        t.indexOf('check task') !== -1 ||
                        t.indexOf('check') !== -1 ||
                        t.indexOf('claim reward') !== -1 ||
                        t.indexOf('claim') !== -1 ||
                        t.indexOf('get reward') !== -1 ||
                        t.indexOf('get money') !== -1 ||
                        t.indexOf('click to confirm') !== -1 ||
                        t.indexOf('tap to confirm') !== -1 ||
                        t.indexOf('finish') !== -1 ||
                        t.indexOf('complete') !== -1) {
                        return true;
                    }

                    // 3. Bengali keywords (if translated)
                    if (t.indexOf('নিশ্চিত করুন') !== -1 ||
                        t.indexOf('যাচাই করুন') !== -1 ||
                        t.indexOf('ভিউ নিশ্চিত') !== -1 ||
                        t.indexOf('পুরস্কার') !== -1 ||
                        t.indexOf('সম্পন্ন') !== -1) {
                        return true;
                    }

                    // 4. CSS Classes & IDs & OnClick functions
                    if (cls.indexOf('btn_confirm') !== -1 ||
                        cls.indexOf('btn_check') !== -1 ||
                        cls.indexOf('confirm-btn') !== -1 ||
                        cls.indexOf('btn_success') !== -1 ||
                        cls.indexOf('btn-success') !== -1 ||
                        cls.indexOf('btn_youtube') !== -1 ||
                        cls.indexOf('btn-verify') !== -1 ||
                        cls.indexOf('btn_verify') !== -1 ||
                        oc.indexOf('confirm') !== -1 ||
                        oc.indexOf('check_task') !== -1 ||
                        oc.indexOf('check_adv') !== -1 ||
                        oc.indexOf('func_check') !== -1 ||
                        oc.indexOf('get_money') !== -1 ||
                        oc.indexOf('confirm_view') !== -1 ||
                        oc.indexOf('verify') !== -1 ||
                        id.indexOf('confirm') !== -1 ||
                        id.indexOf('btn_check') !== -1 ||
                        id.indexOf('btn-check') !== -1 ||
                        id.indexOf('check') !== -1) {
                        return true;
                    }
                    return false;
                }

                var clicked = false;

                // 1. Strictly locate the SAME task row that was initiated
                var targetRow = (window._avisoLastTaskId ? document.getElementById(window._avisoLastTaskId) : null) || 
                                (window._avisoLastTaskRow && document.body.contains(window._avisoLastTaskRow) ? window._avisoLastTaskRow : null);
                
                // If the task row cannot be identified with certainty, DO NOT CLICK!
                if (!targetRow) {
                    if (window.AvisoBridge && window.AvisoBridge.onError) {
                        window.AvisoBridge.onError('Task row could not be identified with certainty. Skipping confirmation click.');
                    }
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkConfirmClicked) {
                        window.AvisoBridge.onAutoWorkConfirmClicked(false);
                    }
                    return;
                }

                try { targetRow.scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}

                // 2. Locate the "Confirm view" button belonging to THAT EXACT task row
                var rowCandidates = targetRow.querySelectorAll('button, a, input, [role="button"], span[onclick], div[onclick], span, div');
                var targetConfirmBtn = null;
                for (var r = 0; r < rowCandidates.length; r++) {
                    if (isConfirmBtn(rowCandidates[r])) {
                        targetConfirmBtn = rowCandidates[r];
                        break;
                    }
                }

                // If not found in primary search, check the specific cell where the link was located
                if (!targetConfirmBtn) {
                    var targetCell = (window._avisoLastTaskCell && document.body.contains(window._avisoLastTaskCell)) ? window._avisoLastTaskCell : null;
                    if (!targetCell && window._avisoLastTaskCellIndex !== undefined && targetRow.cells && targetRow.cells[window._avisoLastTaskCellIndex]) {
                        targetCell = targetRow.cells[window._avisoLastTaskCellIndex];
                    }
                    if (targetCell) {
                        var cellCandidates = targetCell.querySelectorAll('button, a, input, [role="button"], span[onclick], div[onclick], span, div');
                        for (var c = 0; c < cellCandidates.length; c++) {
                            if (isConfirmBtn(cellCandidates[c])) {
                                targetConfirmBtn = cellCandidates[c];
                                break;
                            }
                        }
                    }
                }

                // If still not found, check around the last clicked link container inside this SAME row
                if (!targetConfirmBtn && window._avisoLastTaskLink && targetRow.contains(window._avisoLastTaskLink)) {
                    var pEl = window._avisoLastTaskLink.parentElement;
                    if (pEl) {
                        var pCandidates = pEl.querySelectorAll('button, a, input, [role="button"]');
                        for (var pc = 0; pc < pCandidates.length; pc++) {
                            if (isConfirmBtn(pCandidates[pc])) {
                                targetConfirmBtn = pCandidates[pc];
                                break;
                            }
                        }
                    }
                }

                // 3. Strict Execution: Click ONLY if the button belongs to this exact task
                if (targetConfirmBtn) {
                    safeClick(targetConfirmBtn);
                    clicked = true;
                    if (window.AvisoBridge && window.AvisoBridge.onAutomationLog) {
                        window.AvisoBridge.onAutomationLog('[Confirm] Clicked Confirm View on the exact same task row.');
                    }
                } else {
                    // NEVER click a different task's “Confirm view”
                    if (window.AvisoBridge && window.AvisoBridge.onAutomationLog) {
                        window.AvisoBridge.onAutomationLog('[Safety] Confirm view button could not be matched to this exact task row. Re-checking page safely.');
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

    /**
     * Verifies that the task page is loaded and ready, leaving Confirm View and task rows untouched in their natural page state.
     */
    const val JS_HIGHLIGHT_CONFIRM_BUTTON = """
        (function() {
            try {
                if (window.AvisoBridge && window.AvisoBridge.onConfirmViewPending) {
                    window.AvisoBridge.onConfirmViewPending(window._avisoLastTaskId || '', window._avisoLastExtractedSec || 15);
                }
            } catch(e) {}
        })();
    """

    /**
     * Specifically checks for and handles the interstitial "To count your view, please watch the video for at least..." screen:
     * - Parses exact required duration (e.g. 1 minute 30 seconds -> 90s, or 40 seconds -> 40s).
     * - Clicks "Start Watching" button with dispatchEvent.
     * - Reports duration via AvisoBridge.onInterstitialHandled and onAutoWorkTaskStarted.
     */
    const val JS_CHECK_AND_HANDLE_INTERSTITIAL = """
        (function() {
            try {
                var bodyText = (document.body ? document.body.innerText : '') || '';
                var textLower = bodyText.toLowerCase();

                var isInterstitial = (
                    textLower.indexOf('to count your view') !== -1 ||
                    textLower.indexOf('please watch the video for at least') !== -1 ||
                    textLower.indexOf('start watching') !== -1 ||
                    textLower.indexOf('чтобы засчитать просмотр') !== -1 ||
                    textLower.indexOf('посмотрите видео не менее') !== -1 ||
                    textLower.indexOf('начать просмотр') !== -1 ||
                    textLower.indexOf('приступить к просмотру') !== -1
                );

                if (!isInterstitial) return 0;

                var durationSec = 0;
                var mMinSec = textLower.match(/(\d+)\s*(?:minute|min|минут)[s]?\s*(?:and\s*)?(\d+)\s*(?:second|sec|секунд)[s]?/i);
                if (mMinSec) {
                    durationSec = (parseInt(mMinSec[1], 10) || 0) * 60 + (parseInt(mMinSec[2], 10) || 0);
                }
                if (durationSec === 0) {
                    var mMin = textLower.match(/(\d+)\s*(?:minute|min|минут)[s]?/i);
                    if (mMin) durationSec = (parseInt(mMin[1], 10) || 0) * 60;
                }
                if (durationSec === 0) {
                    var mSec = textLower.match(/(\d+)\s*(?:second|sec|секунд)[s]?/i);
                    if (mSec) durationSec = parseInt(mSec[1], 10) || 0;
                }
                if (durationSec <= 0) durationSec = 40;

                var clickables = document.querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                for (var i = 0; i < clickables.length; i++) {
                    var el = clickables[i];
                    var txt = (el.innerText || el.value || '').trim().toLowerCase();
                    if (txt === 'start watching' || txt.indexOf('start watching') !== -1 || txt === 'начать просмотр' || txt.indexOf('начать просмотр') !== -1 || txt.indexOf('приступить к просмотру') !== -1) {
                        try { el.click(); } catch(e) {}
                        try {
                            var evt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                            el.dispatchEvent(evt);
                        } catch(e) {}

                        if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                            window.AvisoBridge.onInterstitialHandled(durationSec);
                        }
                        if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                            window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                        }
                        return durationSec;
                    }
                }
            } catch(e) {}
            return 0;
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
