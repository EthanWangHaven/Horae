package com.horae.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.horae.app.alarm.ReminderScheduler
import com.horae.app.data.AppSettings
import com.horae.app.ui.board.BoardScreen
import com.horae.app.ui.edit.AddEditScreen
import com.horae.app.ui.list.ScheduleListScreen
import com.horae.app.ui.search.SearchScreen
import com.horae.app.ui.settings.SettingsScreen
import com.horae.app.ui.theme.HoraeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        ReminderScheduler.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            HoraeTheme {
                HoraeApp()
            }
        }
    }
}

@Composable
fun HoraeApp() {
    val nav = rememberNavController()

    // Android 13+ 必须动态请求通知权限，否则提醒通知无法展示
    val context = androidx.compose.ui.platform.LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(navController = nav, startDestination = "board") {
        // 周视图看板（首页）
        composable("board") {
            BoardScreen(
                onOpenList = { nav.navigate("list") },
                onAdd = { millis -> nav.navigate("edit?dayMillis=$millis") },
                onEdit = { id, dayMillis -> nav.navigate("edit?scheduleId=$id&dayMillis=$dayMillis") },
                onSearch = { nav.navigate("search") },
                onSettings = { nav.navigate("settings") },
            )
        }
        // 日程列表页
        composable("list") {
            ScheduleListScreen(
                initialDayMillis = System.currentTimeMillis(),
                onBack = { nav.popBackStack() },
                onAdd = { millis -> nav.navigate("edit?dayMillis=$millis") },
                onEdit = { id, dayMillis -> nav.navigate("edit?scheduleId=$id&dayMillis=$dayMillis") },
            )
        }
        // 搜索页
        composable("search") {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id, millis -> nav.navigate("edit?scheduleId=$id&dayMillis=$millis") },
            )
        }
        // 设置页
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        // 添加 / 编辑日程页
        composable(
            route = "edit?scheduleId={scheduleId}&dayMillis={dayMillis}",
            arguments = listOf(
                navArgument("scheduleId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("dayMillis") {
                    type = NavType.LongType
                    defaultValue = System.currentTimeMillis()
                },
            ),
        ) { entry ->
            val scheduleId = entry.arguments?.getLong("scheduleId") ?: -1L
            val dayMillis = entry.arguments?.getLong("dayMillis") ?: System.currentTimeMillis()
            AddEditScreen(
                scheduleId = if (scheduleId > 0) scheduleId else null,
                initialMillis = dayMillis,
                onCancel = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }
    }
}
