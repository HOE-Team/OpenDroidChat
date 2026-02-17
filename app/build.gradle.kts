plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.hoeteam.opendroidchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hoeteam.opendroidchat"
        minSdk = 23
        targetSdk = 36
        versionCode = 13
        versionName = "Beta-1.0Fix-VerCheckFix"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFileProp = project.findProperty("STORE_FILE") as String?
            val storePasswordProp = project.findProperty("STORE_PASSWORD") as String?
            val keyAliasProp = project.findProperty("KEY_ALIAS") as String?
            val keyPasswordProp = project.findProperty("KEY_PASSWORD") as String?
            
            if (storeFileProp != null) {
                storeFile = file(storeFileProp)
            }
            if (storePasswordProp != null) {
                storePassword = storePasswordProp
            }
            if (keyAliasProp != null) {
                keyAlias = keyAliasProp
            }
            if (keyPasswordProp != null) {
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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

    // Markdown 渲染 (直接使用 Markwon 以支持深色模式下行内代码主题)
    implementation("io.noties.markwon:core:4.6.2")

    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
