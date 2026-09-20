package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AvisoUiState
import com.example.viewmodel.AvisoViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvisoSidebar(
    uiState: AvisoUiState,
    viewModel: AvisoViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("right_sidebar_panel"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            // Top Bar with Cross Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "সাইডবার নিয়ন্ত্রণ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সাইজ ও টাস্ক সেটিংস",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Cross Button (Explicit user requirement)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("close_sidebar_cross_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Scrollable Settings Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // SECTION 1: Page Size / Zoom Controls (Explicit user requirement)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পৃষ্ঠার সাইজ (Zoom)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.zoomPercent}%",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Zoom Slider
                        Slider(
                            value = uiState.zoomPercent.toFloat(),
                            onValueChange = { viewModel.setZoom(it.toInt()) },
                            valueRange = 50f..250f,
                            steps = 19,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("zoom_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("৫০% (ছোট)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text("১০০% (স্বাভাবিক)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text("২৫০% (বড়)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick buttons: Zoom In, Zoom Out, 100% Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.zoomOut() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("zoom_out_button"),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "ছোট করুন", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ছোট", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.resetZoom() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(38.dp)
                                    .testTag("zoom_reset_button")
                            ) {
                                Text("১০০% রিসেট", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.zoomIn() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("zoom_in_button"),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "বড় করুন", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বড়", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Desktop Mode Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleDesktopMode() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (uiState.isDesktopMode) "ডেস্কটপ ভিউ সক্রিয়" else "মোবাইল ভিউ সক্রিয়",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "কম্পিউটারের মত পুরো সাইট দেখুন",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isDesktopMode,
                                onCheckedChange = { viewModel.toggleDesktopMode() },
                                modifier = Modifier.testTag("desktop_mode_switch")
                            )
                        }
                    }
                }

                // SECTION 2: YouTube Tasks Detection & Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ইউটিউব কাজের স্ট্যাটাস",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { viewModel.reloadPage() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "রিফ্রেশ",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Task Count Display
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("মোট YouTube কাজ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${uiState.scanResult.totalTasks} টি",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (uiState.scanResult.isLoggedIn) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (uiState.scanResult.username.isNotBlank()) uiState.scanResult.username else "লগইন আছে",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFE65100),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("লগইন চেক করুন", fontSize = 11.sp, color = Color(0xFFE65100))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category breakdown chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ভিডিও ভিউ", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "${uiState.scanResult.watchCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("সাবস্ক্রাইব", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "${uiState.scanResult.subscribeCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("লাইক", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "${uiState.scanResult.likesCount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Background Notification & Permissions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isBgServiceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ব্যাকগ্রাউন্ডে সর্বদা কাজ চেক",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (uiState.isBgServiceRunning) "চলমান (নতুন কাজ পেলেই ফোনে নোটিফিকেশন আসবে)" else "বন্ধ আছে",
                                        fontSize = 10.sp,
                                        color = if (uiState.isBgServiceRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isBgServiceRunning,
                                onCheckedChange = { viewModel.toggleBgService(context) },
                                modifier = Modifier.testTag("bg_service_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("চেক করার ব্যবধান:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 5, 10).forEach { mins ->
                                FilterChip(
                                    selected = uiState.checkIntervalMinutes == mins,
                                    onClick = { viewModel.setCheckInterval(context, mins) },
                                    label = { Text("$mins মিনিট", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Notification Master Switch (Notification On / Off)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (uiState.isNotificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "নোটিফিকেশন",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (uiState.isNotificationsEnabled) "চালু আছে (নোটিফিকেশন পাবেন)" else "সম্পূর্ণ বন্ধ (কোনো নোটিফিকেশন আসবে না)",
                                        fontSize = 10.sp,
                                        color = if (uiState.isNotificationsEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isNotificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications(context) },
                                modifier = Modifier.testTag("notifications_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Notification Sound Switch (Sound On / Off)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (uiState.isNotificationsEnabled && uiState.isNotificationSoundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "নোটিফিকেশন সাউন্ড",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (!uiState.isNotificationsEnabled) {
                                            "নোটিফিকেশন বন্ধ"
                                        } else if (uiState.isNotificationSoundEnabled) {
                                            "সাউন্ড চালু (অ্যালার্ট রিংটোন বাজবে)"
                                        } else {
                                            "সাউন্ড বন্ধ (সাইলেন্ট নোটিফিকেশন)"
                                        },
                                        fontSize = 10.sp,
                                        color = if (uiState.isNotificationsEnabled && uiState.isNotificationSoundEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isNotificationSoundEnabled,
                                onCheckedChange = { viewModel.toggleNotificationSound(context) },
                                enabled = uiState.isNotificationsEnabled,
                                modifier = Modifier.testTag("notification_sound_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Notification on Task Finish / No Tasks Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (uiState.isNotificationsEnabled && uiState.isNotifyOnTaskEndEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "কাজ শেষ হলে নোটিফিকেশন",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (!uiState.isNotificationsEnabled) {
                                            "নোটিফিকেশন বন্ধ"
                                        } else if (uiState.isNotifyOnTaskEndEnabled) {
                                            "সব কাজ শেষ হলে বা কাজ না থাকলে নোটিফিকেশন দেবে"
                                        } else {
                                            "বন্ধ আছে"
                                        },
                                        fontSize = 10.sp,
                                        color = if (uiState.isNotificationsEnabled && uiState.isNotifyOnTaskEndEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isNotifyOnTaskEndEnabled,
                                onCheckedChange = { viewModel.toggleNotifyOnTaskEnd(context) },
                                enabled = uiState.isNotificationsEnabled,
                                modifier = Modifier.testTag("notify_on_task_end_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Notification Button
                        OutlinedButton(
                            onClick = { viewModel.sendTestNotification(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("test_notification_button")
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("টেস্ট নোটিফিকেশন পাঠান", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // App Notification Permission Button (Explicit user requirement)
                        Button(
                            onClick = { viewModel.openAppNotificationSettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("open_app_settings_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফোনের App Settings-এ পারমিশন দিন", fontSize = 12.sp)
                        }
                    }
                }

                // SECTION 4: Aviso Quick Links & Login Fix
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "দ্রুত নেভিগেশন ও লগইন সমাধান",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Go to Tasks
                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo("https://aviso.bz/tasks-youtube")
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("ইউটিউব টাস্ক পেজ খুলুন", fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Go to Login
                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo("https://aviso.bz/login")
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("লগইন পেজে যান (Login)", fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Force Cookie Sync & Reload
                        OutlinedButton(
                            onClick = {
                                android.webkit.CookieManager.getInstance().flush()
                                viewModel.reloadPage()
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কুকিজ সিঙ্ক ও রিফ্রেশ", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
