package com.horae.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horae.app.ui.glass.LocalWallSize
import com.horae.app.ui.glass.Wall
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.SubText

/** 每个屏幕的根布局：绘制全局壁纸并向玻璃面板提供尺寸 */
@Composable
fun GlassScreenRoot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it.toSize() }
            .drawBehind { if (!size.isEmpty()) Wall.draw(this, size) },
    ) {
        CompositionLocalProvider(LocalWallSize provides size) {
            content()
        }
    }
}

/** 圆形玻璃图标按钮 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    iconSize: Int = 20,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .liquidGlass(shape = CircleShape, tintAlpha = 0.6f, blurRadius = 16.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

/** 玻璃分组卡片（编辑页的分组容器） */
@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        content()
    }
}

/** 设置行：左侧图标 + 标签，右侧值与下拉箭头 */
@Composable
fun SettingsRow(
    icon: ImageVector?,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valuePlaceholder: String? = null,
    valueColor: Color = SubText,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .padding(vertical = 10.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
        }
        Text(
            text = label,
            style = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            color = Ink,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(text = value, color = Ink)
        }
        if (valuePlaceholder != null) {
            Text(text = valuePlaceholder, color = valueColor)
        }
        trailing?.invoke()
    }
}

/** 无涟漪点击（用于自绘玻璃/色块） */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/** iOS 风格开关：圆角轨道 + 大白圆点，开启时轨道为主题色（默认蓝色） */
@Composable
fun IOSSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    checkedTrackColor: Color = AccentBlue,
    uncheckedTrackColor: Color = Color(0xFFE9E9EA),
) {
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 24.dp
    val thumbPadding = 2.dp
    val travel = trackWidth - thumbSize - thumbPadding * 2
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "iosSwitch",
    )
    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) checkedTrackColor else uncheckedTrackColor)
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickableNoRipple { onCheckedChange(!checked) }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbPadding + travel * progress)
                .size(thumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                .background(Color.White, CircleShape),
        )
    }
}

/** 主色文字按钮（如「保存」） */
@Composable
fun AccentTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** 通用玻璃弹窗容器：与看板日程详情弹窗同款样式（90% 宽 + 80% 白半透明 + 22dp 圆角）；
 * compact = true 时上下内边距更小，用于选项/确认类小弹窗 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoRipple(onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(0.9f)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                    .clickableNoRipple { } // 吞掉面板内点击
                    .padding(horizontal = 20.dp, vertical = if (compact) 12.dp else 18.dp),
            ) {
                content()
            }
        }
    }
}

/** 弹窗底部操作行：取消 + 可选确认按钮（confirmColor 可定制，如删除红色） */
@Composable
fun DialogActions(
    onCancel: () -> Unit,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    confirmColor: Color = AccentBlue,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "取消",
            color = Ink,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickableNoRipple(onCancel)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
        if (confirmText != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(confirmColor)
                    .clickableNoRipple(onConfirm)
                    .padding(horizontal = 20.dp, vertical = 9.dp),
            ) {
                Text(
                    text = confirmText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
