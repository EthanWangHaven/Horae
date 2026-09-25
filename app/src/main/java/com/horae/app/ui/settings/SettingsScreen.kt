package com.horae.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horae.app.data.AppSettings
import com.horae.app.ui.common.DialogActions
import com.horae.app.ui.common.GlassDialog
import com.horae.app.ui.common.GlassScreenRoot
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.common.strings
import com.horae.app.ui.glass.BoardThemes
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Hairline
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.SubText

private const val SENS_LEVELS = 5

/** 设置页：看板主题 / 纵轴显示范围 / 滑动灵敏度 / 语言 / 关于 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val s = strings()
    val lang = AppSettings.languageIndex
    var langPickerOpen by remember { mutableStateOf(false) }
    GlassScreenRoot {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ---------- 顶部：返回 + 标题（胶囊条宽度包裹内容） ----------
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .liquidGlass(shape = RoundedCornerShape(24.dp), tintAlpha = 0.6f, blurRadius = 20.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.back,
                    tint = AccentBlue,
                    modifier = Modifier.clickableNoRipple(onBack),
                )
                Spacer(Modifier.width(14.dp))
                Text(text = s.settings, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            }

            Spacer(Modifier.height(14.dp))

            // ---------- 看板主题 ----------
            SettingsCard(title = s.boardTheme) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoardThemes.forEachIndexed { i, theme ->
                        val selected = AppSettings.themeIndex == i
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickableNoRipple { AppSettings.updateThemeIndex(i) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.verticalGradient(listOf(theme.top, theme.bottom)),
                                        RoundedCornerShape(13.dp),
                                    )
                                    .then(
                                        if (selected) Modifier.border(2.dp, AccentBlue, RoundedCornerShape(13.dp))
                                        else Modifier.border(1.dp, Hairline, RoundedCornerShape(13.dp))
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(AccentBlue, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = theme.displayName(lang),
                                fontSize = 11.sp,
                                color = if (selected) Ink else SubText,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- 纵轴显示范围 ----------
            SettingsCard(title = s.axisRange) {
                AxisRangeRow(
                    label = s.start,
                    hour = AppSettings.axisStartHour,
                    onMinus = { AppSettings.updateAxisRange(AppSettings.axisStartHour - 1, AppSettings.axisEndHour) },
                    onPlus = { AppSettings.updateAxisRange(AppSettings.axisStartHour + 1, AppSettings.axisEndHour) },
                )
                Spacer(Modifier.height(6.dp))
                AxisRangeRow(
                    label = s.end,
                    hour = AppSettings.axisEndHour,
                    onMinus = { AppSettings.updateAxisRange(AppSettings.axisStartHour, AppSettings.axisEndHour - 1) },
                    onPlus = { AppSettings.updateAxisRange(AppSettings.axisStartHour, AppSettings.axisEndHour + 1) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---------- 滑动灵敏度 ----------
            SettingsCard(title = s.sensitivity) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = s.low, fontSize = 12.sp, color = SubText)
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.weight(1f).height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(SENS_LEVELS) { i ->
                            val level = i + 1
                            val selected = AppSettings.sensitivity == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(
                                        if (selected) AccentBlue.copy(alpha = 0.85f)
                                        else Color.White.copy(alpha = 0.5f),
                                        RoundedCornerShape(11.dp),
                                    )
                                    .clickableNoRipple { AppSettings.updateSensitivity(level) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = level.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else SubText,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(text = s.high, fontSize = 12.sp, color = SubText)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- 语言 ----------
            SettingsCard(title = s.language) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { langPickerOpen = true },
                ) {
                    Text(
                        text = if (lang == 1) "English" else "中文",
                        fontSize = 15.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(text = "›", fontSize = 18.sp, color = SubText)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- 关于 ----------
            SettingsCard(title = s.about) {
                Column {
                    Text(text = s.author, fontSize = 13.sp, color = SubText)
                    Text(
                        text = s.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(text = s.openSource, fontSize = 13.sp, color = SubText)
                    Text(
                        text = "https://github.com/EthanWangHaven/Horae",
                        fontSize = 13.sp,
                        color = AccentBlue,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ---------- 语言选择弹窗（与其他弹窗统一玻璃风格） ----------
    if (langPickerOpen) {
        GlassDialog(onDismiss = { langPickerOpen = false }) {
            Text(text = s.language, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(10.dp))
            listOf(0 to "中文", 1 to "English").forEach { (idx, label) ->
                val selected = AppSettings.languageIndex == idx
                Text(
                    text = if (selected) "✓ $label" else label,
                    color = if (selected) AccentBlue else Ink,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple {
                            AppSettings.updateLanguageIndex(idx)
                            langPickerOpen = false
                        }
                        .padding(vertical = 11.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            DialogActions(onCancel = { langPickerOpen = false })
        }
    }
}

/** 玻璃设置卡片：标题 + 内容 */
@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 纵轴范围步进行：标签 + 时间 + 步进按钮 */
@Composable
private fun AxisRangeRow(
    label: String,
    hour: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 14.sp, color = Ink)
        Spacer(Modifier.weight(1f))
        StepButton(text = "−", onClick = onMinus)
        Text(
            text = "%02d:00".format(hour),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentBlue,
            modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepButton(text = "+", onClick = onPlus)
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White.copy(alpha = 0.55f), CircleShape)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
    }
}
