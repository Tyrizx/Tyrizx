plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.tyrizx"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.tyrizx"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // Existing AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Kotlin coroutines (required by kmp-process async API)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // nodejs-mobile – full Node.js runtime for Android
    implementation("com.janeasystems:nodejs-mobile:18.20.4")

    // kmp-process – child_process-style API for Android
    implementation("io.github.xxfast:kmp-process:0.3.0")
}
