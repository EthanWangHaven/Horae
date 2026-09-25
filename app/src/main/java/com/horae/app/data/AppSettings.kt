package com.horae.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 全局设置（SharedPreferences 持久化）：
 * 看板主题 / 纵轴显示范围 / 竖滑灵敏度
 */
object AppSettings {
    private const val PREFS = "horae_settings"
    private const val KEY_THEME = "theme_index"
    private const val KEY_START = "axis_start_hour"
    private const val KEY_END = "axis_end_hour"
    private const val KEY_SENS = "scroll_sensitivity"
    private const val KEY_HEADER_COLLAPSED = "board_header_collapsed"
    private const val KEY_LANG = "language_index"

    /** 看板背景主题序号（0 起，上界由 Wall.BoardThemes 决定） */
    var themeIndex by mutableIntStateOf(0)
        private set

    /** 纵轴起始小时（0..23） */
    var axisStartHour by mutableIntStateOf(7)
        private set

    /** 纵轴结束小时（start+1..24） */
    var axisEndHour by mutableIntStateOf(23)
        private set

    /** 竖滑灵敏度挡位（1..5，5 最快） */
    var sensitivity by mutableIntStateOf(4)
        private set

    /** 看板顶部标题栏是否折叠（记住上次状态） */
    var boardHeaderCollapsed by mutableStateOf(false)
        private set

    /** 界面语言：0 中文，1 English */
    var languageIndex by mutableIntStateOf(0)
        private set

    /** 灵敏度挡位对应的滚动位移/惯性乘数 */
    val sensitivityFactor: Float
        get() = floatArrayOf(0.72f, 0.96f, 1.2f, 1.5f, 1.8f)[(sensitivity - 1).coerceIn(0, 4)]

    private var prefs: android.content.SharedPreferences? = null

    fun load(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also {
            themeIndex = it.getInt(KEY_THEME, 0)
            axisStartHour = it.getInt(KEY_START, 7)
            axisEndHour = it.getInt(KEY_END, 23)
            sensitivity = it.getInt(KEY_SENS, 4)
            boardHeaderCollapsed = it.getBoolean(KEY_HEADER_COLLAPSED, false)
            languageIndex = it.getInt(KEY_LANG, 0)
        }
    }

    fun updateThemeIndex(v: Int) {
        themeIndex = v.coerceAtLeast(0)
        prefs?.edit()?.putInt(KEY_THEME, themeIndex)?.apply()
    }

    fun updateAxisRange(start: Int, end: Int) {
        axisStartHour = start.coerceIn(0, 23)
        axisEndHour = end.coerceIn(axisStartHour + 1, 24)
        prefs?.edit()
            ?.putInt(KEY_START, axisStartHour)
            ?.putInt(KEY_END, axisEndHour)
            ?.apply()
    }

    fun updateSensitivity(v: Int) {
        sensitivity = v.coerceIn(1, 5)
        prefs?.edit()?.putInt(KEY_SENS, sensitivity)?.apply()
    }

    fun updateBoardHeaderCollapsed(v: Boolean) {
        boardHeaderCollapsed = v
        prefs?.edit()?.putBoolean(KEY_HEADER_COLLAPSED, v)?.apply()
    }

    fun updateLanguageIndex(v: Int) {
        languageIndex = v.coerceIn(0, 1)
        prefs?.edit()?.putInt(KEY_LANG, languageIndex)?.apply()
    }

    /** 非组合环境（如广播接收器）使用：未加载时先加载 */
    fun ensureLoaded(context: Context) {
        if (prefs == null) load(context)
    }
}
