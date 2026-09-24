package com.horae.app.ui.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS 液态玻璃修饰符：
 * 1. clip 到目标形状；
 * 2. 壁纸（玻璃后的真实背景）先记录进独立 GraphicsLayer，经 RenderEffect 实时高斯模糊
 *    后再绘制 —— 真·背景折射，且不会模糊组件自身的内容（文字/图标保持清晰）；
 * 3. 叠加半透明白磨砂层与柔和高光描边。
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float = 0.52f,
    blurRadius: Dp = 22.dp,
    highlight: Boolean = true,
): Modifier = composed {
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val blurEffect = remember(blurPx) {
        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    }
    val wallLayer = rememberGraphicsLayer()
    val wallSize = LocalWallSize.current
    var windowPos by remember { mutableStateOf(Offset.Zero) }

    this
        .onGloballyPositioned { windowPos = it.positionInWindow() }
        .clip(shape)
        .drawBehind {
            // 只把"壁纸 + 白磨砂 tint"记录进独立 layer 并应用模糊，
            // 组件内容不受 renderEffect 影响
            wallLayer.record {
                if (wallSize.width > 0f) {
                    translate(-windowPos.x, -windowPos.y) {
                        Wall.draw(this, wallSize)
                    }
                } else {
                    drawRect(Color(0xFFF2F3F8))
                }
                drawRect(Color.White.copy(alpha = tintAlpha))
            }
            wallLayer.renderEffect = blurEffect
            drawLayer(wallLayer)
        }
        .drawWithContent {
            drawContent()
            if (highlight) {
                drawStroke(
                    outline = shape.createOutline(size, layoutDirection, this),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.35f),
                        ),
                        startY = 0f,
                        endY = size.height,
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
        }
}

/** 沿 Outline 轮廓描边（drawOutline 的等效实现，兼容 Compose 1.7） */
private fun DrawScope.drawStroke(
    outline: Outline,
    brush: Brush,
    strokeWidth: Float,
) {
    val style = Stroke(width = strokeWidth)
    when (outline) {
        is Outline.Rectangle ->
            drawRect(brush = brush, topLeft = outline.bounds.topLeft, size = outline.bounds.size, style = style)
        is Outline.Rounded ->
            drawPath(path = Path().apply { addRoundRect(outline.roundRect) }, brush = brush, style = style)
        is Outline.Generic ->
            drawPath(path = outline.path, brush = brush, style = style)
    }
}
