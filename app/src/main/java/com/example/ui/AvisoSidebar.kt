package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogoBadge
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoGlow
import com.example.ui.theme.WarmAmber
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
            .width(340.dp)
            .testTag("right_sidebar_panel"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        shadowElevation = 24.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // Top Gradient Accent Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryIndigo, AccentViolet, AccentCoral, MintEmerald)
                        )
                    )
            )

            // Modern Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AppLogoBadge(size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aviso Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "কন্ট্রোল হাব ও কনফিগারেশন",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigoGlow,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Close Cross Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .testTag("close_sidebar_cross_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // HERO SECTION: Live Status & Task Metrics Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, PrimaryIndigoGlow.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header row: Account & Refresh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // User Auth Badge
                            if (uiState.scanResult.isLoggedIn) {
                                Surface(
                                    color = MintEmerald.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MintEmerald,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.scanResult.username.isNotBlank()) uiState.scanResult.username else "লগইন সক্রিয় ✓",
                                            fontSize = 11.sp,
                                            color = MintEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = WarmAmber.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = WarmAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "লগইন চেক প্রয়োজন",
                                            fontSize = 11.sp,
                                            color = WarmAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Reload Button
                            IconButton(
                                onClick = { viewModel.reloadPage() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "রিফ্রেশ",
                                    tint = PrimaryIndigoGlow,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Task Count Display
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "মোট YouTube কাজ:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${uiState.scanResult.totalTasks} টি",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryIndigoGlow
                                    )
                                }

                                // Category Mini Chips
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    MiniCategoryPill(label = "ভিডিও", count = uiState.scanResult.watchCount, color = AccentCoral)
                                    MiniCategoryPill(label = "সাবস্ক্রাইব", count = uiState.scanResult.subscribeCount, color = AccentViolet)
                                    MiniCategoryPill(label = "লাইক", count = uiState.scanResult.likesCount, color = MintEmerald)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Session Task Results Counter
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MintEmerald,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "সফল: ${uiState.successfulTasksCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintEmerald
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = AccentCoral,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "ব্যর্থ: ${uiState.failedTasksCount}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCoral
                                        )
                                    }
                                }

                                if (uiState.successfulTasksCount > 0 || uiState.failedTasksCount > 0) {
                                    Text(
                                        text = "রিসেট ↺",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoGlow,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.resetTaskCounts() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 1: Page Size / Zoom Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SidebarSectionHeader(
                            icon = Icons.Default.ZoomIn,
                            title = "পৃষ্ঠার সাইজ (Zoom)",
                            badgeText = "${uiState.zoomPercent}%"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                        // Direct Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(50, 75, 100, 125, 150, 200).forEach { preset ->
                                Surface(
                                    color = if (uiState.zoomPercent == preset) PrimaryIndigo else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (uiState.zoomPercent == preset) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setZoom(preset) }
                                ) {
                                    Text(
                                        text = "$preset%",
                                        fontSize = 10.sp,
                                        fontWeight = if (uiState.zoomPercent == preset) FontWeight.Bold else FontWeight.Medium,
                                        color = if (uiState.zoomPercent == preset) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Zoom Quick Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.zoomOut() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("zoom_out_button"),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "ছোট করুন", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("ছোট", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.resetZoom() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(36.dp)
                                    .testTag("zoom_reset_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("১০০% রিসেট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.zoomIn() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("zoom_in_button"),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "বড় করুন", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("বড়", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Desktop Mode Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleDesktopMode() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (uiState.isDesktopMode) PrimaryIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = if (uiState.isDesktopMode) PrimaryIndigoGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (uiState.isDesktopMode) "ডেস্কটপ ভিউ সক্রিয়" else "মোবাইল ভিউ সক্রিয়",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "কম্পিউটারের মতো পুরো সাইট প্রদর্শিত হবে",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // SECTION 2: Video Watching Mode Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SidebarSectionHeader(
                            icon = Icons.Default.PlayCircle,
                            title = "ভিডিও দেখার মোড ও ট্যাব সেটিংস"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 1: In-App Tab Mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!uiState.openInExternalYouTubeApp) PrimaryIndigo.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.2.dp,
                                if (!uiState.openInExternalYouTubeApp) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (uiState.openInExternalYouTubeApp) viewModel.toggleExternalYouTubeAppMode()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !uiState.openInExternalYouTubeApp,
                                    onClick = { if (uiState.openInExternalYouTubeApp) viewModel.toggleExternalYouTubeAppMode() }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "📑 নতুন ট্যাব সিস্টেম (প্রস্তাবিত)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!uiState.openInExternalYouTubeApp) PrimaryIndigoGlow else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ভিডিও নতুন ট্যাবে চলবে, টাইমার শেষ হলে ট্যাব স্বয়ংক্রিয়ভাবে বন্ধ হয়ে মূল পেজে কনফার্ম করবে।",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: External YouTube App Mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.openInExternalYouTubeApp) AccentCoral.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.2.dp,
                                if (uiState.openInExternalYouTubeApp) AccentCoral else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!uiState.openInExternalYouTubeApp) viewModel.toggleExternalYouTubeAppMode()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.openInExternalYouTubeApp,
                                    onClick = { if (!uiState.openInExternalYouTubeApp) viewModel.toggleExternalYouTubeAppMode() }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "📱 সরাসরি YouTube App",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.openInExternalYouTubeApp) AccentCoral else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "YouTube অ্যাপে ভিডিও চলবে, টাইমার শেষে নোটিফিকেশন ও ওভারলে সহকারে অ্যাপে ফেরত আসবে।",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Background Service & Notifications
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SidebarSectionHeader(
                            icon = if (uiState.isBgServiceRunning) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            title = "ব্যাকগ্রাউন্ড মনিটর ও নোটিফিকেশন",
                            badgeText = if (uiState.isBgServiceRunning) "চলমান ✓" else "বন্ধ"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Background Service Master Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isBgServiceRunning) MintEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (uiState.isBgServiceRunning) MintEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ব্যাকগ্রাউন্ডে সর্বদা চেক",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (uiState.isBgServiceRunning) "নতুন কাজ পেলেই ফোনে নোটিফিকেশন আসবে" else "সার্ভিস নিষ্ক্রিয়",
                                        fontSize = 10.sp,
                                        color = if (uiState.isBgServiceRunning) MintEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isBgServiceRunning,
                                onCheckedChange = { viewModel.toggleBgService(context) },
                                modifier = Modifier.testTag("bg_service_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("চেক করার ব্যবধান:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2, 5, 10).forEach { mins ->
                                FilterChip(
                                    selected = uiState.checkIntervalMinutes == mins,
                                    onClick = { viewModel.setCheckInterval(context, mins) },
                                    label = { Text("$mins মিনিট", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Notifications Master Switch
                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "নোটিফিকেশন বার্তা",
                            subtitle = if (uiState.isNotificationsEnabled) "চালু আছে (অ্যালার্ট পাবেন)" else "সম্পূর্ণ বন্ধ",
                            isChecked = uiState.isNotificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(context) },
                            testTag = "notifications_master_switch",
                            tint = if (uiState.isNotificationsEnabled) PrimaryIndigoGlow else MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sound Alerts Switch
                        SettingsToggleRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "নোটিফিকেশন সাউন্ড",
                            subtitle = if (!uiState.isNotificationsEnabled) "নোটিফিকেশন বন্ধ" else if (uiState.isNotificationSoundEnabled) "সাউন্ড চালু (রিংটোন বাজবে)" else "সাইলেন্ট নোটিফিকেশন",
                            isChecked = uiState.isNotificationSoundEnabled,
                            onCheckedChange = { viewModel.toggleNotificationSound(context) },
                            enabled = uiState.isNotificationsEnabled,
                            testTag = "notification_sound_switch",
                            tint = if (uiState.isNotificationSoundEnabled) MintEmerald else MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Task End Notification Switch
                        SettingsToggleRow(
                            icon = Icons.Default.CheckCircle,
                            title = "কাজ শেষ হলে নোটিফিকেশন",
                            subtitle = if (!uiState.isNotificationsEnabled) "নোটিফিকেশন বন্ধ" else if (uiState.isNotifyOnTaskEndEnabled) "সব কাজ শেষ হলে নোটিফিকেশন দেবে" else "বন্ধ আছে",
                            isChecked = uiState.isNotifyOnTaskEndEnabled,
                            onCheckedChange = { viewModel.toggleNotifyOnTaskEnd(context) },
                            enabled = uiState.isNotificationsEnabled,
                            testTag = "notify_on_task_end_switch",
                            tint = if (uiState.isNotifyOnTaskEndEnabled) MintEmerald else MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Send Test Notification
                        OutlinedButton(
                            onClick = { viewModel.sendTestNotification(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("test_notification_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("টেস্ট নোটিফিকেশন পাঠান", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // SECTION 4: App Permissions & Health
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SidebarSectionHeader(
                            icon = Icons.Default.Security,
                            title = "প্রয়োজনীয় পারমিশন ও স্ট্যাটাস"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. Overlay Permission
                        PermissionItemCard(
                            title = "ওভারলে প্রদর্শন (Display Over Apps)",
                            description = "ভিডিও শেষে স্বয়ংক্রিয়ভাবে Aviso অ্যাপে ফিরে এসে কনফার্ম করার জন্য আবশ্যক।",
                            isGranted = uiState.isOverlayPermissionGranted,
                            buttonLabel = "ওভারলে পারমিশন দিন",
                            buttonTestTag = "request_overlay_permission_button",
                            onAction = { viewModel.requestOverlayPermission(context) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Battery Exemption
                        PermissionItemCard(
                            title = "ব্যাটারি অপ্টিমাইজেশন ছাড় (Battery Saver)",
                            description = "স্ক্রিন অফ বা ব্যাকগ্রাউন্ডে কাজ চলাকালীন ফোন যাতে সার্ভিস বন্ধ না করে।",
                            isGranted = uiState.isBatteryOptimizationExempted,
                            buttonLabel = "ব্যাটারি ছাড় দিন",
                            buttonTestTag = "request_battery_exemption_button",
                            onAction = { viewModel.requestBatteryOptimizationExemption(context) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Notification Permission
                        PermissionItemCard(
                            title = "নোটিফিকেশন পারমিশন (Notifications)",
                            description = "কাজের কাউন্টডাউন প্রগ্রেস এবং নতুন কাজ আসার অ্যালার্ট সাউন্ড পাওয়ার জন্য প্রয়োজন।",
                            isGranted = uiState.isNotificationGranted,
                            buttonLabel = "নোটিফিকেশন সেটিংস",
                            buttonTestTag = "open_notification_settings_button",
                            onAction = { viewModel.openAppNotificationSettings(context) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full App Info Settings Button
                        Button(
                            onClick = { viewModel.openAppSettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("open_full_app_settings_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigo
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফোনের সম্পূর্ণ App Settings খুলুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION 5: Quick Navigation Shortcuts
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SidebarSectionHeader(
                            icon = Icons.Default.OpenInBrowser,
                            title = "দ্রুত নেভিগেশন ও টুলস"
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
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ইউটিউব টাস্ক পেজ খুলুন", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("লগইন পেজে যান (Login)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কুকিজ সিঙ্ক ও রিফ্রেশ", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SidebarSectionHeader(
    icon: ImageVector,
    title: String,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryIndigoGlow,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (badgeText != null) {
            Surface(
                color = PrimaryIndigo.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigoGlow,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniCategoryPill(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
            Text("$count", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    enabled: Boolean = true,
    testTag: String,
    tint: Color
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    buttonTestTag: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) MintEmerald.copy(alpha = 0.4f) else WarmAmber.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (isGranted) MintEmerald.copy(alpha = 0.15f) else WarmAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isGranted) "সক্রিয় ✓" else "অনুমতি দিন ⚠️",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) MintEmerald else WarmAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .testTag(buttonTestTag),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(buttonLabel, fontSize = 10.sp)
            }
        }
    }
}
