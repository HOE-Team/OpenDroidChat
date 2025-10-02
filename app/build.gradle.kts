plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.hoeteam.opendroidchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hoeteam.opendroidchat"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "ALPHA-0.3-MarkdownSupport"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)

    // ViewModel Compose 扩展
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore (配置存储)
    implementation(libs.androidx.datastore.datastore.preferences6)

    // JSON 序列化/反序列化 (用于 DataStore 存储复杂对象)
    implementation(libs.com.google.code.gson.gson6)

    // LLM API 网络请求 (Ktor 客户端)
    implementation(libs.io.ktor.ktor.client.core6)
    implementation(libs.io.ktor.ktor.client.android6)
    implementation(libs.io.ktor.ktor.client.content.negotiation6)
    implementation(libs.io.ktor.ktor.client.content.negotiation6)
    implementation(libs.io.ktor.ktor.serialization.kotlinx.json6)
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")

    // Markdown 库：直接使用字符串依赖，它依赖于 settings.gradle.kts 中的 JitPack 仓库
    implementation("com.github.jeziellago:compose-markdown:0.5.7")
}
