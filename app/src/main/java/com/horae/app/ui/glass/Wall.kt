package com.horae.app.ui.glass

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.horae.app.data.AppSettings

/** 壁纸尺寸由每个屏幕根布局提供，玻璃面板据此折射模糊背景 */
val LocalWallSize = staticCompositionLocalOf { Size.Zero }

/**
 * 看板背景主题：浅色系渐变 + 柔和色斑（保证深色文字可读）。
 */
data class BoardTheme(
    val name: String,
    val nameEn: String,
    val top: Color,
    val bottom: Color,
    val blobs: List<Color>,
) {
    /** 按语言序号显示主题名 */
    fun displayName(lang: Int): String = if (lang == 1) nameEn else name
}

/** 预设主题（顺序与设置页色卡一致） */
val BoardThemes = listOf(
    BoardTheme(
        "晨雾", "Mist", Color(0xFFF5F6FB), Color(0xFFE9ECF5),
        listOf(Color(0x6BBFD4FF), Color(0x5CFFD9E4), Color(0x4FE3D9FF), Color(0x47CDEBFF), Color(0x33FFFFFF)),
    ),
    BoardTheme(
        "暖沙", "Sand", Color(0xFFFBF4EA), Color(0xFFF1E5D3),
        listOf(Color(0x66FFD9AE), Color(0x5CFFE7C8), Color(0x4FFFCF9E), Color(0x47FFDFB8), Color(0x33FFFFFF)),
    ),
    BoardTheme(
        "晴空", "Sky", Color(0xFFEFF6FD), Color(0xFFDCEAF8),
        listOf(Color(0x6BA8D4FF), Color(0x5CC7E3FF), Color(0x4F9ECFFF), Color(0x47BFE0FF), Color(0x33FFFFFF)),
    ),
    BoardTheme(
        "薄荷", "Mint", Color(0xFFEFFAF3), Color(0xFFD8EEE2),
        listOf(Color(0x66A8E6C8), Color(0x5CC2F0DC), Color(0x4F9EDCC0), Color(0x47B8E8D0), Color(0x33FFFFFF)),
    ),
    BoardTheme(
        "樱花", "Blossom", Color(0xFFFDF2F6), Color(0xFFF5E0E8),
        listOf(Color(0x66FFC9DC), Color(0x5CFFDCE8), Color(0x4FFFB6D2), Color(0x47FFD4E4), Color(0x33FFFFFF)),
    ),
    BoardTheme(
        "暮紫", "Dusk", Color(0xFFF6F2FB), Color(0xFFE4DEEF),
        listOf(Color(0x66C9B8F0), Color(0x5CD9CCF8), Color(0x4FB8A0E8), Color(0x47D0C2F4), Color(0x33FFFFFF)),
    ),
)

/**
 * 全局静态壁纸：浅色渐变 + 柔和色斑（iOS 白玻璃风）。
 * 玻璃面板以自身在窗口中的位置平移坐标后绘制对应区域，
 * 再经 RenderEffect 实时模糊，形成真实背景折射。
 */
object Wall {

    fun draw(scope: DrawScope, size: Size) {
        val theme = BoardThemes[AppSettings.themeIndex.coerceIn(0, BoardThemes.size - 1)]
        with(scope) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(theme.top, theme.bottom),
                    endY = size.height,
                ),
                size = size,
            )
            blob(size, 0.12f, 0.08f, 0.50f, theme.blobs[0])
            blob(size, 0.95f, 0.22f, 0.42f, theme.blobs[1])
            blob(size, 0.15f, 0.60f, 0.52f, theme.blobs[2])
            blob(size, 0.90f, 0.90f, 0.46f, theme.blobs[3])
            blob(size, 0.50f, 0.40f, 0.55f, theme.blobs[4])
        }
    }

    private fun DrawScope.blob(
        size: Size,
        fx: Float, fy: Float, fr: Float,
        color: Color,
    ) {
        val r = size.minDimension * fr
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(size.width * fx, size.height * fy),
                radius = r,
            ),
            radius = r,
            center = Offset(size.width * fx, size.height * fy),
        )
    }
}
