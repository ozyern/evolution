package com.oxygenupdater.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.oxygenupdater.ui.theme.light

/*
 * iOS-style grouped ("inset grouped") list building blocks, following Apple's Human Interface
 * Guidelines for lists & tables: rounded cards on a neutral background, inset separators,
 * small secondary section headers/footers, trailing values and disclosure chevrons.
 */

/** Neutral page background behind grouped cards */
val groupedBackground: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
        if (light) surfaceContainer else background
    }

/** Card (cell) colour on top of [groupedBackground] */
val groupedCardColor: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.run {
        if (light) surfaceContainerLowest else surfaceContainerHigh
    }

/** Continuous (squircle) corners, like iOS */
val GroupedCardShape = RoundedRectangle(20.dp)

private val SeparatorInset = 16.dp

/**
 * A titled card of rows. Separators are drawn automatically between visible rows,
 * inset from the leading edge like iOS. Rows that end up empty (0 height) are skipped.
 */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    separatorStart: Dp = SeparatorInset,
    content: @Composable () -> Unit,
) = Column(modifier.padding(horizontal = 16.dp)) {
    if (header != null) GroupedHeader(header)

    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    // Y offsets of row boundaries, updated during layout and read during draw (same frame)
    val boundaries = remember { mutableListOf<Int>() }
    Layout(
        content = content,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GroupedCardShape)
            .background(groupedCardColor)
            .drawWithContent {
                drawContent()
                val start = separatorStart.toPx()
                val stroke = 0.5.dp.toPx().coerceAtLeast(1f)
                boundaries.forEach { y ->
                    drawLine(separatorColor, Offset(start, y.toFloat()), Offset(size.width, y.toFloat()), stroke)
                }
            },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val height = placeables.sumOf { it.height }
        layout(constraints.maxWidth, height) {
            boundaries.clear()
            var y = 0
            var previousVisible = false
            placeables.forEach {
                if (it.height > 0) {
                    if (previousVisible) boundaries.add(y)
                    previousVisible = true
                }
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }

    if (footer != null) GroupedFooter(footer)
}

@Composable
fun GroupedHeader(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
)

@Composable
fun GroupedFooter(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
)

/** Apple-style glyph: white symbol on a coloured rounded square */
@Composable
fun GroupedIcon(icon: ImageVector, tint: Color) = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .size(30.dp)
        .clip(RoundedRectangle(8.dp))
        .background(tint)
) {
    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
}

/**
 * A single row: optional leading icon, title (+ optional secondary text below),
 * optional trailing value, chevron, or custom [trailing] content.
 *
 * @param subtitle shown below the title in secondary colour
 * @param value shown at the trailing edge in secondary colour (like iOS "Detail" cells)
 */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    subtitleIsError: Boolean = false,
    value: String? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .alpha(if (enabled) 1f else 0.4f)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 10.dp)
) {
    val colorScheme = MaterialTheme.colorScheme
    if (leading != null) leading() else if (icon != null) GroupedIcon(icon, iconTint)

    Column(Modifier.weight(1f)) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) Text(
            text = subtitle,
            color = if (subtitleIsError) colorScheme.error else colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }

    if (value != null) Text(
        text = value,
        color = colorScheme.onSurfaceVariant,
        fontSize = 17.sp,
        textAlign = TextAlign.End,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 200.dp)
    )

    trailing?.invoke(this)

    if (chevron) Chevron()
}

/** iOS disclosure indicator */
@Composable
fun Chevron(color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) = Canvas(
    Modifier.size(8.dp, 14.dp)
) {
    val stroke = 2.dp.toPx()
    val w = size.width
    val h = size.height
    drawLine(color, Offset(stroke / 2, stroke / 2), Offset(w - stroke / 2, h / 2), stroke, StrokeCap.Round)
    drawLine(color, Offset(w - stroke / 2, h / 2), Offset(stroke / 2, h - stroke / 2), stroke, StrokeCap.Round)
}

/** Large bold page title, like iOS navigation bars' large titles */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    fontSize = 34.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 41.sp,
    modifier = modifier.padding(horizontal = 20.dp)
)

/** Scrollable column on the grouped background, for whole screens built from [GroupedSection]s */
@Composable
fun GroupedColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier
        .fillMaxWidth()
        .background(groupedBackground),
    content = content,
)
