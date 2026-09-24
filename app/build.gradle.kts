import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// ===== 版本号自增 =====
// version.properties 保存当前版本；执行编译类任务（assemble/bundle/install/build）时末位 +1，
// 末位满 100 向中间位进 1（如 1.0.99 -> 1.1.0），中间位满 100 同理进到首位。
// APK 输出名：Horae-<版本号>-app-<debug|release>.apk，如 Horae-1.0.2-app-debug.apk
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}
var vMajor = (versionProps.getProperty("VERSION_MAJOR") ?: "1").toInt()
var vMinor = (versionProps.getProperty("VERSION_MINOR") ?: "0").toInt()
var vPatch = (versionProps.getProperty("VERSION_PATCH") ?: "1").toInt()
val isCompiling = gradle.startParameter.taskNames.any { taskName ->
    val t = taskName.lowercase()
    "assemble" in t || "bundle" in t || "install" in t || t == "build" || t.endsWith(":build")
}
if (isCompiling) {
    vPatch += 1
    if (vPatch >= 100) {
        vPatch = 0
        vMinor += 1
        if (vMinor >= 100) {
            vMinor = 0
            vMajor += 1
        }
    }
    versionProps.setProperty("VERSION_MAJOR", vMajor.toString())
    versionProps.setProperty("VERSION_MINOR", vMinor.toString())
    versionProps.setProperty("VERSION_PATCH", vPatch.toString())
    versionPropsFile.outputStream().use { versionProps.store(it, "Horae build version") }
}

android {
    namespace = "com.horae.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.horae.app"
        minSdk = 31
        targetSdk = 36
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        versionName = "$vMajor.$vMinor.$vPatch"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // APK 重命名：Horae-<版本>-app-<变体>.apk
    // 注：Gradle 9 + Kotlin 2.2 下 all{}/configureEach{} 的 lambda 会被误解析成
    //     标准库或 kts 扩展版本，改用匿名 Action 对象，确保走 Gradle 成员方法
    @Suppress("UNCHECKED_CAST")
    val variants = applicationVariants
        as org.gradle.api.DomainObjectCollection<com.android.build.gradle.api.BaseVariant>
    variants.all(object : org.gradle.api.Action<com.android.build.gradle.api.BaseVariant> {
        override fun execute(variant: com.android.build.gradle.api.BaseVariant) {
            @Suppress("UNCHECKED_CAST")
            val outputs = variant.outputs
                as org.gradle.api.DomainObjectCollection<com.android.build.gradle.api.BaseVariantOutput>
            outputs.all(object : org.gradle.api.Action<com.android.build.gradle.api.BaseVariantOutput> {
                override fun execute(output: com.android.build.gradle.api.BaseVariantOutput) {
                    (output as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                        .outputFileName =
                        "Horae-$vMajor.$vMinor.$vPatch-app-${variant.name}.apk"
                }
            })
        }
    })
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
}
