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
                function handleOpenUrl(url) {
                    if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) return;
                    var isYT = (url.indexOf('youtube.com') !== -1 || url.indexOf('youtu.be') !== -1);
                    var isSession = (url.indexOf('/vl/') !== -1 || url.indexOf('/go/') !== -1 || url.indexOf('create_session') !== -1 || url.indexOf('youtube.php') !== -1);
                    
                    if (isYT || isSession) {
                        if (window.AvisoBridge && window.AvisoBridge.openNewTab) {
                            window.AvisoBridge.openNewTab(url, 20);
                            return;
                        }
                    } else {
                        if (window.AvisoBridge && window.AvisoBridge.openNewTab) {
                            window.AvisoBridge.openNewTab(url, 0);
                            return;
                        }
                    }
                    window.location.href = url;
                }

                // Override window.open to delegate to separate Tab system
                window.open = function(url, target, features) {
                    if (url) {
                        handleOpenUrl(url);
                    }
                    return {
                        closed: false,
                        close: function() {},
                        focus: function() {},
                        location: {
                            set href(val) { handleOpenUrl(val); },
                            get href() { return window.location.href; },
                            replace: function(val) { handleOpenUrl(val); },
                            assign: function(val) { handleOpenUrl(val); }
                        },
                        document: {
                            write: function() {},
                            close: function() {}
                        }
                    };
                };

                // Capture clicks on document: if a link opens in new tab/window or YouTube/session, route to Tab system
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    var a = (el && el.tagName === 'A') ? el : (el && el.closest ? el.closest('a') : null);
                    if (a) {
                        var href = (a.getAttribute('href') || a.href || '').toString();
                        var target = (a.getAttribute('target') || '').toLowerCase();
                        var isYT = (href.indexOf('youtube.com') !== -1 || href.indexOf('youtu.be') !== -1);
                        var isSession = (href.indexOf('/vl/') !== -1 || href.indexOf('/go/') !== -1 || href.indexOf('create_session') !== -1 || href.indexOf('youtube.php') !== -1);
                        
                        if (isYT || isSession || target === '_blank') {
                            e.preventDefault();
                            handleOpenUrl(a.getAttribute('href') || a.href);
                            return;
                        }
                    }
                }, true);
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

                // 1. Enforce same-window navigation
                window.open = function(url) {
                    if (url && url !== 'about:blank' && url.indexOf('javascript:') === -1) {
                        window.location.href = url;
                    }
                    return window;
                };
                var blankLinks = document.querySelectorAll('a[target="_blank"], a[target="_new"], a[target]');
                for (var b = 0; b < blankLinks.length; b++) {
                    blankLinks[b].target = '_self';
                    blankLinks[b].removeAttribute('target');
                }

                // Clean, reliable click helper
                function safeClick(el) {
                    if (!el) return false;
                    try { el.scrollIntoView({ behavior: 'instant', block: 'center' }); } catch(e) {}
                    var aTag = (el.tagName === 'A') ? el : (el.closest ? el.closest('a') : null);

                    var href = (aTag ? aTag.getAttribute('href') : null) || el.getAttribute('href');
                    if (aTag) {
                        aTag.removeAttribute('target');
                        aTag.setAttribute('target', '_self');
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
                        setTimeout(function() {
                            window.location.href = (aTag ? aTag.href : null) || href;
                        }, 120);
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

                function extractDuration(text) {
                    var t = text || '';
                    var mMS = t.match(/(\d+)\s*(?:minute|min|минут)[s]?\s*(?:and\s*)?(\d+)\s*(?:second|sec|секунд)[s]?/i);
                    if (mMS) return (parseInt(mMS[1], 10) || 0) * 60 + (parseInt(mMS[2], 10) || 0);
                    var mM = t.match(/(\d+)\s*(?:minute|min|минут)[s]?/i);
                    if (mM) return (parseInt(mM[1], 10) || 0) * 60;
                    var mS = t.match(/(\d+)\s*(?:сек|sec|секунд)/i);
                    if (mS) return parseInt(mS[1], 10) || 10;
                    return 10;
                }

                // 2. Check if a genuine visible captcha block exists
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
                    var interSec = extractDuration(textLower);
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
                    var gDur = extractDuration(globalStartRow ? globalStartRow.innerText : '');
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

                    var sec = extractDuration(rText);

                    // Check if row ALREADY has "Приступить к просмотру" visible
                    var rowEls = row.querySelectorAll('button, a, input[type="button"], span, div');
                    for (var re = 0; re < rowEls.length; re++) {
                        if (isStartWatchingBtn(rowEls[re])) {
                            startBtnAlreadyVisible = rowEls[re];
                            break;
                        }
                    }

                    // Look for the clickable task title in row
                    // (Strictly avoid advertiser user profiles, instructions, complaints, delete)
                    var links = row.querySelectorAll('a, span[onclick], div[onclick], .title, .task-title, .work-title, a.serf-url');
                    var candidateLink = null;
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
                    try { el.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch(e) {}
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
                    var t = (el.innerText || el.value || '').trim().toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    var oc = (el.getAttribute('onclick') || '').toLowerCase();
                    var id = (el.id || '').toLowerCase();

                    if (!t && !cls && !oc && !id) return false;

                    if (t.indexOf('отмена') !== -1 || t.indexOf('cancel') !== -1 || t.indexOf('жалоб') !== -1 || t.indexOf('delete') !== -1) {
                        return false;
                    }

                    if (t.indexOf('подтвердить просмотр') !== -1 ||
                        t.indexOf('подтвердить') !== -1 ||
                        t.indexOf('проверить выполнение') !== -1 ||
                        t.indexOf('проверить задание') !== -1 ||
                        t.indexOf('проверить просмотр') !== -1 ||
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
                        t.indexOf('confirm view') !== -1 ||
                        t.indexOf('confirm') !== -1 ||
                        t.indexOf('verify') !== -1 ||
                        t.indexOf('claim') !== -1 ||
                        t.indexOf('get reward') !== -1 ||
                        t.indexOf('get money') !== -1) {
                        return true;
                    }

                    if (cls.indexOf('btn_confirm') !== -1 ||
                        cls.indexOf('btn_check') !== -1 ||
                        cls.indexOf('confirm-btn') !== -1 ||
                        cls.indexOf('btn_success') !== -1 ||
                        cls.indexOf('btn-success') !== -1 ||
                        cls.indexOf('btn_youtube') !== -1 ||
                        oc.indexOf('confirm') !== -1 ||
                        oc.indexOf('check_task') !== -1 ||
                        oc.indexOf('check_adv') !== -1 ||
                        oc.indexOf('func_check') !== -1 ||
                        oc.indexOf('get_money') !== -1 ||
                        oc.indexOf('confirm_view') !== -1 ||
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

                // 3. YouTube Iframes: Trigger playback ONCE and launch in YouTube App
                if (!window._avisoVideoPlayTriggered) {
                    window._avisoVideoPlayTriggered = true;
                    var iframes = document.querySelectorAll('iframe[src*="youtube"], iframe[src*="youtu.be"], iframe#video-click, iframe');
                    for (var f = 0; f < iframes.length; f++) {
                        var ifr = iframes[f];
                        var src = (ifr.getAttribute('src') || ifr.src || '').toString();
                        if ((src.indexOf('youtube.com') !== -1 || src.indexOf('youtu.be') !== -1) && !window._avisoYtAppOpened) {
                            window._avisoYtAppOpened = true;
                            if (window.AvisoBridge && window.AvisoBridge.openInYouTubeApp) {
                                window.AvisoBridge.openInYouTubeApp(src);
                            }
                        }
                        try {
                            ifr.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                            ifr.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                        } catch(e) {}
                    }

                    // Click initial play overlay once
                    var startButtons = document.querySelectorAll('#video-click, .video-click, #start_video, .start-video, .ytp-large-play-button, button.btn-play, [class*="play_btn"], a[onclick*="start"], button[onclick*="start"]');
                    for (var s = 0; s < startButtons.length; s++) {
                        try {
                            startButtons[s].click();
                        } catch(e) {}
                    }
                }

                // 4. Read Real Aviso Countdown Timer across all frames
                var realSec = -1;
                for (var d2 = 0; d2 < docs.length; d2++) {
                    var curDoc = docs[d2];
                    var timerEls = curDoc.querySelectorAll('#tmr, #timer, .timer, #sec, span[id*="tmr"], span[id*="time"], div[id*="timer"], .time_block, .time-count');
                    for (var t = 0; t < timerEls.length; t++) {
                        var txt = (timerEls[t].innerText || '').trim();
                        var m = txt.match(/(\d+)/);
                        if (m) {
                            realSec = parseInt(m[1], 10);
                            break;
                        }
                    }
                    if (realSec !== -1) break;
                    var bTxt = (curDoc.body ? curDoc.body.innerText : '') || '';
                    var m2 = bTxt.match(/Осталось:\s*(\d+)\s*сек/i) || bTxt.match(/(\d+)\s*сек(?:унд)?/i) || bTxt.match(/(\d+)\s*sec/i);
                    if (m2) {
                        realSec = parseInt(m2[1], 10);
                        break;
                    }
                }

                if (window.AvisoBridge && window.AvisoBridge.onRealTimerUpdate) {
                    window.AvisoBridge.onRealTimerUpdate(realSec);
                }

                // 5. Check for "Подтвердить просмотр" (Confirm View) or "Забрать награду" across all frames
                for (var d3 = 0; d3 < docs.length; d3++) {
                    var cDoc = docs[d3];
                    var confirmBtns = cDoc.querySelectorAll('#btn_check, .btn_confirm, [id*="confirm"], [id*="check"], button, a, input[type="button"], span, div');
                    for (var cb = 0; cb < confirmBtns.length; cb++) {
                        var cEl = confirmBtns[cb];
                        if (isConfirmBtn(cEl)) {
                            // Scroll button into clear view for user
                            try { cEl.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch(e) {}
                            // If countdown is finished, trigger click
                            if (realSec <= 0 && cEl.offsetParent !== null) {
                                safeClick(cEl);
                                if (window.AvisoBridge && window.AvisoBridge.onTaskCompleted) {
                                    window.AvisoBridge.onTaskCompleted();
                                }
                                return;
                            }
                        }
                    }
                }

                // 6. Check if task completed text is shown across all frames
                if (textLower.indexOf('засчитан') !== -1 || textLower.indexOf('начислено') !== -1 || textLower.indexOf('получили') !== -1) {
                    if (window.AvisoBridge && window.AvisoBridge.onTaskCompleted) {
                        window.AvisoBridge.onTaskCompleted();
                    }
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
                    try { el.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch(e) {}
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
                    var t = (el.innerText || el.value || '').trim().toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    var oc = (el.getAttribute('onclick') || '').toLowerCase();
                    var id = (el.id || '').toLowerCase();

                    if (!t && !cls && !oc && !id) return false;

                    if (t.indexOf('отмена') !== -1 || t.indexOf('cancel') !== -1 || t.indexOf('жалоб') !== -1 || t.indexOf('delete') !== -1) {
                        return false;
                    }

                    if (t.indexOf('подтвердить просмотр') !== -1 ||
                        t.indexOf('подтвердить') !== -1 ||
                        t.indexOf('проверить выполнение') !== -1 ||
                        t.indexOf('проверить задание') !== -1 ||
                        t.indexOf('проверить просмотр') !== -1 ||
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
                        t.indexOf('confirm view') !== -1 ||
                        t.indexOf('confirm') !== -1 ||
                        t.indexOf('verify') !== -1 ||
                        t.indexOf('claim') !== -1 ||
                        t.indexOf('get reward') !== -1 ||
                        t.indexOf('get money') !== -1) {
                        return true;
                    }

                    if (cls.indexOf('btn_confirm') !== -1 ||
                        cls.indexOf('btn_check') !== -1 ||
                        cls.indexOf('confirm-btn') !== -1 ||
                        cls.indexOf('btn_success') !== -1 ||
                        cls.indexOf('btn-success') !== -1 ||
                        cls.indexOf('btn_youtube') !== -1 ||
                        oc.indexOf('confirm') !== -1 ||
                        oc.indexOf('check_task') !== -1 ||
                        oc.indexOf('check_adv') !== -1 ||
                        oc.indexOf('func_check') !== -1 ||
                        oc.indexOf('get_money') !== -1 ||
                        oc.indexOf('confirm_view') !== -1 ||
                        id.indexOf('confirm') !== -1 ||
                        id.indexOf('btn_check') !== -1 ||
                        id.indexOf('btn-check') !== -1 ||
                        id.indexOf('check') !== -1) {
                        return true;
                    }
                    return false;
                }

                var clicked = false;
                var docs = getAllDocs();

                // 1. First check inside the saved active task row if present on tasks-youtube!
                if (window._avisoLastTaskRow && document.body.contains(window._avisoLastTaskRow)) {
                    var row = window._avisoLastTaskRow;
                    try { row.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch(e) {}
                    var rowCandidates = row.querySelectorAll('button, a, input[type="button"], span[role="button"], div[role="button"], span, div');
                    for (var r = 0; r < rowCandidates.length; r++) {
                        if (isConfirmBtn(rowCandidates[r])) {
                            safeClick(rowCandidates[r]);
                            clicked = true;
                            break;
                        }
                    }
                }

                // 2. Global search across all documents and frames
                if (!clicked) {
                    for (var d = 0; d < docs.length; d++) {
                        var allCandidates = docs[d].querySelectorAll('#btn_check, .btn_confirm, [id*="confirm"], [id*="check"], button, a, input[type="button"], span[role="button"], div[role="button"], span, div');
                        for (var i = 0; i < allCandidates.length; i++) {
                            var el = allCandidates[i];
                            if (isConfirmBtn(el) && el.offsetParent !== null) {
                                safeClick(el);
                                clicked = true;
                                break;
                            }
                        }
                        if (clicked) break;
                    }
                }

                if (clicked && window._avisoLastTaskRow) {
                    try {
                        window._avisoLastTaskRow.classList.add('task-done');
                        window._avisoLastTaskRow.setAttribute('data-completed', 'true');
                        window._avisoLastTaskRow.style.opacity = '0.4';
                    } catch(e) {}
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
