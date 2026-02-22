import java.util.Properties
import org.gradle.api.GradleException

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
        versionCode = 15
        versionName = "Stable-1.2-CopyButtonUpdate"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 尝试从 keystore.properties 文件读取签名配置
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                println("Found keystore.properties, using it for release signing")
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())

                val storeFileProp = keystoreProperties.getProperty("storeFile")
                val storePasswordProp = keystoreProperties.getProperty("storePassword")
                val keyAliasProp = keystoreProperties.getProperty("keyAlias")
                val keyPasswordProp = keystoreProperties.getProperty("keyPassword")

                if (storeFileProp != null) {
                    storeFile = file(storeFileProp)
                } else {
                    throw GradleException("Keystore file path not found in keystore.properties")
                }

                if (storePasswordProp != null) {
                    storePassword = storePasswordProp
                } else {
                    throw GradleException("Store password not found in keystore.properties")
                }

                if (keyAliasProp != null) {
                    keyAlias = keyAliasProp
                } else {
                    throw GradleException("Key alias not found in keystore.properties")
                }

                if (keyPasswordProp != null) {
                    keyPassword = keyPasswordProp
                } else {
                    throw GradleException("Key password not found in keystore.properties")
                }
            } else {
                // 如果 keystore.properties 不存在，则从 gradle.properties 读取（用于调试）
                println("keystore.properties not found, falling back to gradle.properties")
                val storeFileProp = project.findProperty("STORE_FILE") as String?
                val storePasswordProp = project.findProperty("STORE_PASSWORD") as String?
                val keyAliasProp = project.findProperty("KEY_ALIAS") as String?
                val keyPasswordProp = project.findProperty("KEY_PASSWORD") as String?

                if (storeFileProp != null) {
                    storeFile = file(storeFileProp)
                } else {
                    throw GradleException("STORE_FILE not found in gradle.properties")
                }

                if (storePasswordProp != null) {
                    storePassword = storePasswordProp
                } else {
                    throw GradleException("STORE_PASSWORD not found in gradle.properties")
                }

                if (keyAliasProp != null) {
                    keyAlias = keyAliasProp
                } else {
                    throw GradleException("KEY_ALIAS not found in gradle.properties")
                }

                if (keyPasswordProp != null) {
                    keyPassword = keyPasswordProp
                } else {
                    throw GradleException("KEY_PASSWORD not found in gradle.properties")
                }
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

        debug {
            // debug 版本使用默认的调试签名
            isDebuggable = true
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
    implementation(libs.io.ktor.ktor.serialization.kotlinx.json6)

    // Markdown 渲染 (直接使用 Markwon 以支持深色模式下行内代码主题)
    implementation("io.noties.markwon:core:4.6.2")

    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}