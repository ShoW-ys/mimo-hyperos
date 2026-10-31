plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.showys.mimoandroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.showys.mimoandroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    packaging {
        // 关键：安装时把 jniLibs 解压到 nativeLibraryDir（W^X 唯一可执行目录）
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += setOf("**/libproot.so", "**/libproot-loader.so")
        }
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            // Phase 7 再配置签名；先保证 assembleRelease 可产出未签名包
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
