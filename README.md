# Horae

Horae 是一款 Android 原生日程看板应用，采用 iOS 液态玻璃（Liquid Glass）视觉风格，提供周视图看板、日程管理、提醒等功能。

## 功能特性

- **周视图看板**：七列时间网格，日程块按时间定位，重叠自动分车道显示
- **日程管理**：添加 / 编辑 / 删除日程，支持标题、地点、备注、颜色标记
- **日程提醒**：本地通知提醒，到点不遗漏
- **搜索**：按标题、地点、备注模糊搜索历史日程
- **主题换肤**：6 种预设看板背景色调（晨雾 / 暖沙 / 晴空 / 薄荷 / 樱花 / 暮紫）
- **纵轴范围**：看板时间轴显示范围可调（默认 7:00 – 23:00）
- **滑动灵敏度**：5 挡竖滑灵敏度调节
- **液态玻璃 UI**：壁纸实时模糊 + 高光描边，全应用统一质感

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Room 本地数据库
- Navigation Compose 单 Activity 导航
- RenderEffect 壁纸模糊实现液态玻璃效果

## 构建运行

1. 使用 Android Studio（Ladybug 或更新版本）打开项目根目录
2. 等待 Gradle Sync 完成后，直接 Run 即可安装到设备 / 模拟器

命令行构建（需本机安装 Gradle 8+ 或使用 Android Studio 内置 Gradle）：

```bash
gradle :app:assembleDebug
```

产物位于 `app/build/outputs/apk/debug/`。

> 项目通过 `version.properties` 管理版本号，执行编译类任务时版本末位自动 +1。

## 项目结构

```
app/src/main/java/com/horae/app/
├── MainActivity.kt        # 单 Activity + NavHost 导航
├── data/                  # Room 实体、DAO、数据库、全局设置
├── alarm/                 # 提醒通知调度
├── logic/                 # 日程展开 / 周切片等业务逻辑
└── ui/
    ├── board/             # 周视图看板（首页）
    ├── list/              # 日程列表页
    ├── edit/              # 添加 / 编辑日程页
    ├── search/            # 搜索页
    ├── settings/          # 设置页（主题 / 纵轴 / 灵敏度 / 关于）
    ├── glass/             # 液态玻璃组件与壁纸绘制
    ├── common/            # 公共 UI 组件
    └── theme/             # 颜色与主题
```

## 作者

**wangce**

开源地址：[https://github.com/EthanWangHaven/Horae](https://github.com/EthanWangHaven/Horae)
