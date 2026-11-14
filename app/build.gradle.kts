plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.animwidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.animwidget"
        minSdk = 26
        targetSdk = 34
        versionCode = 43
        versionName = "1.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// 仅 Shizuku 可选集成需要；主功能不依赖任何第三方库
dependencies {
    implementation("dev.rikka.shizuku:api:12.2.0")
    implementation("dev.rikka.shizuku:provider:12.2.0")
    implementation("androidx.annotation:annotation:1.7.1")
}
