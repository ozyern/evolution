package com.oxygenupdater.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.oxygenupdater.ui.common.withPlaceholder

/**
 * Centred header for the Update screen, after iOS' Settings › General › Software Update:
 * a large rounded icon tile, the OS version as the title, and a short status line.
 */
@Composable
fun UpdateHero(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp)
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .size(88.dp)
            .clip(RoundedRectangle(22.dp))
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.85f), tint)))
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(48.dp))
    }

    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.withPlaceholder(refreshing, MaterialTheme.typography.titleLarge)
    )

    Text(
        text = subtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.withPlaceholder(refreshing, MaterialTheme.typography.bodyMedium)
    )
}
