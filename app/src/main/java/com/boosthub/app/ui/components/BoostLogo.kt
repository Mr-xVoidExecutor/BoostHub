package com.boosthub.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boosthub.app.ui.theme.BrandBlue
import com.boosthub.app.ui.theme.BrandGold
import com.boosthub.app.ui.theme.BrandPurple

/** অ্যানিমেটেড লোগো: গ্রেডিয়েন্ট বক্স + উপরমুখী তীর + পালস রিং */
@Composable
fun BoostLogo(size: Dp = 72.dp, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "logo")
    val pulse by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)),
        label = "pulse"
    )
    val lift by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift"
    )

    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawRoundRect(
            color = BrandBlue.copy(alpha = (1f - pulse) * 0.6f),
            topLeft = Offset(-w * 0.15f * pulse, -w * 0.15f * pulse),
            size = Size(w * (1f + 0.3f * pulse), w * (1f + 0.3f * pulse)),
            cornerRadius = CornerRadius(w * 0.28f),
            style = Stroke(width = w * 0.04f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(BrandBlue, BrandPurple),
                start = Offset.Zero,
                end = Offset(w, w)
            ),
            cornerRadius = CornerRadius(w * 0.26f)
        )
        val dy = -w * 0.03f * lift
        val arrow = Path().apply {
            moveTo(w * 0.5f, w * 0.2f + dy)
            lineTo(w * 0.78f, w * 0.52f + dy)
            lineTo(w * 0.62f, w * 0.52f + dy)
            lineTo(w * 0.62f, w * 0.74f + dy)
            lineTo(w * 0.38f, w * 0.74f + dy)
            lineTo(w * 0.38f, w * 0.52f + dy)
            lineTo(w * 0.22f, w * 0.52f + dy)
            close()
        }
        drawPath(arrow, Color.White)
        drawRoundRect(
            color = BrandGold,
            topLeft = Offset(w * 0.36f, w * 0.8f),
            size = Size(w * 0.28f, w * 0.06f),
            cornerRadius = CornerRadius(w * 0.03f)
        )
    }
}
