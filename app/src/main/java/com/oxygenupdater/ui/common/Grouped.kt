package com.oxygenupdater.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.oxygenupdater.ui.theme.light
import kotlin.math.roundToInt

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

/**
 * For rows of a lazy list that should look like one [GroupedSection] card: rounds the outer corners of the
 * [first] & [last] rows, and draws an inset separator above every row but the first.
 */
@Composable
fun Modifier.groupedCell(
    first: Boolean,
    last: Boolean,
    separatorStart: Dp = SeparatorInset,
): Modifier {
    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    val radius = 20.dp
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    return this
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .clip(shape)
        .background(groupedCardColor)
        .drawWithContent {
            drawContent()
            if (!first) drawLine(
                color = separatorColor,
                start = Offset(separatorStart.toPx(), 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 0.5.dp.toPx().coerceAtLeast(1f),
            )
        }
}

/**
 * iOS segmented control: a capsule track with a raised "thumb" that slides to the selected segment.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val light = colorScheme.light
    val track = if (light) Color(0x1F767680) else Color(0x3D767680)
    val thumb = if (light) Color.White else Color(0xFF636366)
    val count = options.size.coerceAtLeast(1)
    val position by animateFloatAsState(selectedIndex.toFloat(), spring(0.8f, 500f), label = "SegmentPosition")

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedRectangle(10.dp))
            .background(track)
            .padding(2.dp)
            .selectableGroup()
    ) {
        // Thumb
        Layout(
            content = {
                Box(
                    Modifier
                        .shadow(2.dp, RoundedRectangle(8.dp))
                        .clip(RoundedRectangle(8.dp))
                        .background(thumb)
                )
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth / count
            val placeable = measurables.first().measure(Constraints.fixed(width, constraints.maxHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative((position * width).roundToInt(), 0)
            }
        }

        Row(Modifier.matchParentSize()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * iOS-style capsule button.
 *
 * @param filled `true` for a prominent (accent-filled) button, `false` for a tinted ("gray") one
 */
@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .heightIn(min = 44.dp)
        .alpha(if (enabled) 1f else 0.4f)
        .clip(Capsule())
        .background(if (filled) color else color.copy(alpha = 0.12f))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 10.dp)
) {
    val contentColor = if (filled) Color.White else color
    if (icon != null) Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
    Text(
        text = text,
        color = contentColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Small uppercase-free "pill" label, e.g. "Latest" or "Installed" */
@Composable
fun PillLabel(text: String, color: Color = MaterialTheme.colorScheme.primary) = Text(
    text = text,
    color = color,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    modifier = Modifier
        .clip(Capsule())
        .background(color.copy(alpha = 0.14f))
        .padding(horizontal = 8.dp, vertical = 2.dp)
)
