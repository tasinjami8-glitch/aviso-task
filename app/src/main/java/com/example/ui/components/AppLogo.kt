package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoGlow

@Composable
fun AppLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = PrimaryIndigoGlow.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A),
                        Color(0xFF0B0F19)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryIndigoGlow,
                        AccentViolet,
                        AccentCoral
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.16f)
        ) {
            val w = this.size.width
            val h = this.size.height

            // Center YouTube / Media rounded squircle card
            val cardRect = RoundRect(
                left = w * 0.08f,
                top = h * 0.12f,
                right = w * 0.92f,
                bottom = h * 0.88f,
                cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
            )
            val cardPath = Path().apply {
                addRoundRect(cardRect)
            }

            drawPath(
                path = cardPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AccentCoral,
                        Color(0xFFE11D48),
                        AccentViolet
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Fill
            )

            // Inner sleek white play triangle
            val playTriangle = Path().apply {
                moveTo(w * 0.40f, h * 0.32f)
                lineTo(w * 0.72f, h * 0.50f)
                lineTo(w * 0.40f, h * 0.68f)
                close()
            }

            drawPath(
                path = playTriangle,
                color = Color.White,
                style = Fill
            )

            // Dynamic task spark node (top-right emerald indicator)
            drawCircle(
                color = MintEmerald,
                radius = w * 0.11f,
                center = Offset(w * 0.82f, h * 0.18f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.05f,
                center = Offset(w * 0.82f, h * 0.18f)
            )
        }
    }
}

@Composable
fun AppBrandHeader(
    modifier: Modifier = Modifier,
    title: String = "Aviso Pro",
    subtitle: String = "Smart Task Automation Hub"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogoBadge(size = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryIndigoGlow,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
    }
}

