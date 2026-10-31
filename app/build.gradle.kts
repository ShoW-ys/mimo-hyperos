import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 签名参数只从 local.properties（已 gitignore）读取，仓库里不出现密钥
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
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

    signingConfigs {
        if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
            create("release") {
                storeFile = file(localProps.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    packaging {
        // 关键：安装时把 jniLibs 解压到 nativeLibraryDir（W^X 唯一可执行目录）
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += setOf("**/libproot.so", "**/libproot-loader.so")
        }
    }

    androidResources {
        // aapt 会把 .gz 资源改名（去掉后缀），所以资产统一用 .tgz
        noCompress += listOf("tgz")
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
