package com.phataklive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The soft light field the glass frosts over: a cool navy-blue wash from the top left, a warm amber glow on the
 * right and a faint signal green low down. Static, so it costs nothing while scrolling.
 */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val c = Phatak.c
    Box(modifier.fillMaxSize().drawBehind {
        val w = size.width; val h = size.height
        drawRect(c.bgBase)
        drawRect(Brush.radialGradient(listOf(c.glowCool, Color.Transparent), center = Offset(w * 0.05f, h * 0.22f), radius = w * 1.05f))
        drawRect(Brush.radialGradient(listOf(c.glowWarm, Color.Transparent), center = Offset(w * 1.0f, h * 0.48f), radius = w * 0.85f))
        drawRect(Brush.radialGradient(listOf(c.glowSignal, Color.Transparent), center = Offset(w * 0.2f, h * 0.95f), radius = w * 0.9f))
    })
}

/** Top-lit edge of a glass panel; a [tint] (gate state colour) lights the edge in that colour. */
@Composable
fun glassEdge(tint: Color? = null): Brush {
    val c = Phatak.c
    return Brush.verticalGradient(listOf(tint?.copy(alpha = if (c.dark) 0.55f else 0.45f) ?: c.edgeTop, c.edgeBottom))
}

/**
 * A frosted panel: translucent fill, a light-catching edge, and optionally a wash of a state colour from the top
 * (a gate's red / amber / green signal glowing through the glass).
 */
fun Modifier.glass(shape: Shape, tint: Color? = null, strong: Boolean = false): Modifier = composed {
    val c = Phatak.c
    val wash = tint?.let {
        Modifier.background(Brush.verticalGradient(listOf(it.copy(alpha = if (c.dark) 0.22f else 0.15f), it.copy(alpha = 0.03f))))
    } ?: Modifier
    this.clip(shape)
        .background(if (strong) c.glassStrong else c.card)
        .then(wash)
        .border(1.dp, glassEdge(tint), shape)
}

/** Card made of glass; [onClick] gives the whole panel a ripple clipped to its shape. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier.glass(shape, tint).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content,
        )
    }
}
