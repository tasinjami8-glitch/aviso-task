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
     * Overrides window.open and target="_blank" so all popups and video tasks stay in the same WebView.
     */
    const val JS_SETUP_OVERRIDE = """
        (function() {
            try {
                window.open = function(url) {
                    if (url) {
                        window.location.href = url;
                    }
                    return window;
                };
                var blankLinks = document.querySelectorAll('a[target="_blank"]');
                for (var i = 0; i < blankLinks.length; i++) {
                    blankLinks[i].target = '_self';
                }
            } catch(e) {}
        })();
    """

    /**
     * Finds the next uncompleted YouTube task:
     * - Reads duration (e.g. 5 сек, 10 сек, 90 сек).
     * - Clicks the blue task link.
     * - If "Приступить к выполнению" appears, clicks it.
     * - Reports duration and title to Kotlin bridge.
     */
    const val JS_AUTO_WORK_FIND_AND_CLICK = """
        (function() {
            try {
                // 1. Override window.open & link targets
                window.open = function(url) {
                    if (url) {
                        window.location.href = url;
                    }
                    return window;
                };
                var blankLinks = document.querySelectorAll('a[target="_blank"]');
                for (var b = 0; b < blankLinks.length; b++) {
                    blankLinks[b].target = '_self';
                }

                // 2. Captcha Check First
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

                // 2b. Check if current page is already the "Start Watching" Interstitial Screen
                var isDirectInterstitial = (
                    textLower.indexOf('to count your view') !== -1 ||
                    textLower.indexOf('please watch the video for at least') !== -1 ||
                    textLower.indexOf('start watching') !== -1 ||
                    textLower.indexOf('чтобы засчитать просмотр') !== -1 ||
                    textLower.indexOf('начать просмотр') !== -1
                );
                if (isDirectInterstitial) {
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

                    var interBtns = document.querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                    for (var ib = 0; ib < interBtns.length; ib++) {
                        var ibTxt = (interBtns[ib].innerText || interBtns[ib].value || '').trim().toLowerCase();
                        if (ibTxt === 'start watching' || ibTxt.indexOf('start watching') !== -1 || ibTxt === 'начать просмотр') {
                            try { interBtns[ib].click(); } catch(e) {}
                            try { interBtns[ib].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window })); } catch(e) {}
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

                // 3. Locate task rows
                var rows = document.querySelectorAll('tr[id^="task_"], div[id^="task_"], .work-serf, [data-task-id], tr.table-task');
                if (rows.length === 0) {
                    rows = document.querySelectorAll('.table tr, .tasks-list > div, .content-task, tbody tr');
                }

                var foundRow = null;
                var durationSec = 10;
                var taskTitle = '';
                var blueLink = null;
                var startBtn = null;

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

                    // Check if row already has "Приступить к выполнению" or "Подтвердить просмотр"
                    var allEls = row.querySelectorAll('*');
                    var existingBtn = null;
                    for (var k = 0; k < allEls.length; k++) {
                        var elT = (allEls[k].innerText || allEls[k].value || '').trim();
                        if (elT.indexOf('Приступить к выполнению') !== -1 || elT.indexOf('Подтвердить просмотр') !== -1 || elT.indexOf('Начать') !== -1) {
                            existingBtn = allEls[k];
                            break;
                        }
                    }

                    // Look for the blue task link in row
                    var links = row.querySelectorAll('a, span[onclick*="start"], div[onclick*="start"]');
                    var candidateLink = null;
                    for (var l = 0; l < links.length; l++) {
                        var lt = (links[l].innerText || '').trim();
                        var href = links[l].getAttribute('href') || '';
                        if (lt === 'Инструкция' || lt.indexOf('жалоб') !== -1 || href.indexOf('delete') !== -1) {
                            continue;
                        }
                        if (lt.length > 0 || links[l].className.indexOf('serf') !== -1 || href.indexOf('javascript') !== -1 || links[l].getAttribute('onclick')) {
                            candidateLink = links[l];
                            break;
                        }
                    }

                    if (existingBtn || candidateLink) {
                        foundRow = row;
                        durationSec = sec;
                        taskTitle = (candidateLink ? candidateLink.innerText.trim() : rText.split('\n')[0].trim()).substring(0, 80);
                        blueLink = candidateLink;
                        startBtn = existingBtn;
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

                // If "Приступить к выполнению" button is already there, click it directly
                if (startBtn) {
                    startBtn.click();
                    if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                        window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                    }
                } else if (blueLink) {
                    // Click blue link
                    blueLink.click();

                    // Check if clicking blue link revealed "Приступить к выполнению" or "Start Watching" interstitial
                    setTimeout(function() {
                        var bText = (document.body ? document.body.innerText : '') || '';
                        var bLower = bText.toLowerCase();

                        // Check if interstitial screen appeared
                        if (bLower.indexOf('to count your view') !== -1 || bLower.indexOf('please watch the video for at least') !== -1 || bLower.indexOf('start watching') !== -1 || bLower.indexOf('начать просмотр') !== -1) {
                            var interSec = 0;
                            var mMS = bLower.match(/(\d+)\s*(?:minute|min|минут)[s]?\s*(?:and\s*)?(\d+)\s*(?:second|sec|секунд)[s]?/i);
                            if (mMS) {
                                interSec = (parseInt(mMS[1], 10) || 0) * 60 + (parseInt(mMS[2], 10) || 0);
                            }
                            if (interSec === 0) {
                                var mM = bLower.match(/(\d+)\s*(?:minute|min|минут)[s]?/i);
                                if (mM) interSec = (parseInt(mM[1], 10) || 0) * 60;
                            }
                            if (interSec === 0) {
                                var mS = bLower.match(/(\d+)\s*(?:second|sec|секунд)[s]?/i);
                                if (mS) interSec = parseInt(mS[1], 10) || 0;
                            }
                            if (interSec <= 0) interSec = 40;

                            durationSec = interSec;

                            var btns = document.querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                            for (var b = 0; b < btns.length; b++) {
                                var bt = (btns[b].innerText || btns[b].value || '').trim().toLowerCase();
                                if (bt === 'start watching' || bt.indexOf('start watching') !== -1 || bt === 'начать просмотр') {
                                    try { btns[b].click(); } catch(e) {}
                                    try { btns[b].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window })); } catch(e) {}
                                    if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                                        window.AvisoBridge.onInterstitialHandled(durationSec);
                                    }
                                    break;
                                }
                            }
                        } else {
                            var innerEls = foundRow.querySelectorAll('*');
                            var clickedAction = false;
                            for (var m = 0; m < innerEls.length; m++) {
                                var t = (innerEls[m].innerText || innerEls[m].value || '').trim();
                                if (t.indexOf('Приступить к выполнению') !== -1 || t.indexOf('Подтвердить просмотр') !== -1 || t.indexOf('Начать') !== -1) {
                                    innerEls[m].click();
                                    clickedAction = true;
                                    break;
                                }
                            }
                        }

                        if (window.AvisoBridge && window.AvisoBridge.onAutoWorkTaskStarted) {
                            window.AvisoBridge.onAutoWorkTaskStarted(durationSec);
                        }
                    }, 600);
                }
            } catch (err) {
                if (window.AvisoBridge && window.AvisoBridge.onError) {
                    window.AvisoBridge.onError('AutoWork start error: ' + err.toString());
                }
            }
        })();
    """

    /**
     * Active watcher script that runs continuously during video viewing:
     * 1. Detects video player / iframe and triggers playback (playVideo, unmute, video.play()).
     * 2. Calculates video center position and notifies Android to simulate native touch on YouTube play button.
     * 3. Reads the real remaining countdown seconds from the Aviso website.
     * 4. Detects and clicks "Подтвердить просмотр" when timer finishes.
     */
    const val JS_START_AND_WATCH_VIDEO = """
        (function() {
            try {
                // 1. Captcha Check
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

                // 1b. Check and handle "Start Watching" Interstitial Screen if present
                if (textLower.indexOf('to count your view') !== -1 || textLower.indexOf('please watch the video for at least') !== -1 || textLower.indexOf('start watching') !== -1 || textLower.indexOf('начать просмотр') !== -1) {
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

                    var startWatchBtns = document.querySelectorAll('button, a, input[type="button"], div[role="button"], span[role="button"], div, span');
                    for (var swb = 0; swb < startWatchBtns.length; swb++) {
                        var swTxt = (startWatchBtns[swb].innerText || startWatchBtns[swb].value || '').trim().toLowerCase();
                        if (swTxt === 'start watching' || swTxt.indexOf('start watching') !== -1 || swTxt === 'начать просмотр') {
                            try { startWatchBtns[swb].click(); } catch(e) {}
                            try { startWatchBtns[swb].dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window })); } catch(e) {}
                            if (window.AvisoBridge && window.AvisoBridge.onInterstitialHandled) {
                                window.AvisoBridge.onInterstitialHandled(interSec);
                            }
                            break;
                        }
                    }
                }

                // 2. Play HTML5 <video> if present
                var vids = document.querySelectorAll('video');
                for (var v = 0; v < vids.length; v++) {
                    try {
                        vids[v].muted = false;
                        if (vids[v].paused) {
                            vids[v].play().catch(function(e){});
                        }
                    } catch(e) {}
                }

                // 3. YouTube Iframes & Overlays
                var iframes = document.querySelectorAll('iframe[src*="youtube"], iframe[src*="youtu.be"], iframe#video-click, iframe');
                for (var f = 0; f < iframes.length; f++) {
                    var ifr = iframes[f];
                    try {
                        ifr.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                        ifr.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                    } catch(e) {}

                    // Report video bounding box for native Android touch simulation
                    try {
                        var rect = ifr.getBoundingClientRect();
                        if (rect.width > 60 && rect.height > 60 && rect.top >= 0) {
                            var d = window.devicePixelRatio || 1.0;
                            var cx = (rect.left + rect.width / 2.0) * d;
                            var cy = (rect.top + rect.height / 2.0) * d;
                            if (window.AvisoBridge && window.AvisoBridge.onVideoPositionFound) {
                                window.AvisoBridge.onVideoPositionFound(cx, cy);
                            }
                        }
                    } catch(e) {}
                }

                // Click Aviso overlay buttons for starting video if present
                var startButtons = document.querySelectorAll('#video-click, .video-click, #start_video, .start-video, .ytp-large-play-button, .ytp-play-button, button.btn-play, [class*="play_btn"], a[onclick*="start"], button[onclick*="start"]');
                for (var s = 0; s < startButtons.length; s++) {
                    try {
                        startButtons[s].click();
                    } catch(e) {}
                }

                // 4. Read Real Aviso Countdown Timer
                var realSec = -1;
                var timerEls = document.querySelectorAll('#tmr, #timer, .timer, #sec, span[id*="tmr"], span[id*="time"], div[id*="timer"], .time_block, .time-count');
                for (var t = 0; t < timerEls.length; t++) {
                    var txt = (timerEls[t].innerText || '').trim();
                    var m = txt.match(/(\d+)/);
                    if (m) {
                        realSec = parseInt(m[1], 10);
                        break;
                    }
                }
                if (realSec === -1) {
                    var m2 = bodyText.match(/Осталось:\s*(\d+)\s*сек/i) || bodyText.match(/(\d+)\s*сек(?:унд)?/i);
                    if (m2) {
                        realSec = parseInt(m2[1], 10);
                    }
                }
                if (window.AvisoBridge && window.AvisoBridge.onRealTimerUpdate) {
                    window.AvisoBridge.onRealTimerUpdate(realSec);
                }

                // 5. Check for "Подтвердить просмотр" (Confirm View) or "Забрать награду"
                var confirmBtns = document.querySelectorAll('button, a, input[type="button"], span, div');
                for (var cb = 0; cb < confirmBtns.length; cb++) {
                    var cEl = confirmBtns[cb];
                    var cTxt = (cEl.innerText || cEl.value || '').trim();
                    if (cTxt.indexOf('Подтвердить просмотр') !== -1 || cTxt.indexOf('Забрать') !== -1 || cTxt.indexOf('Получить') !== -1 || cTxt.indexOf('Подтвердить') !== -1) {
                        if (cEl.offsetParent !== null) {
                            cEl.click();
                            if (window.AvisoBridge && window.AvisoBridge.onTaskCompleted) {
                                window.AvisoBridge.onTaskCompleted();
                            }
                            return;
                        }
                    }
                }

                // 6. Check if task completed text is shown
                if (bodyText.indexOf('засчитан') !== -1 || bodyText.indexOf('начислено') !== -1 || bodyText.indexOf('получили') !== -1) {
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
                    textLower.indexOf('начать просмотр') !== -1
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
                    if (txt === 'start watching' || txt.indexOf('start watching') !== -1 || txt === 'начать просмотр') {
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
