import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val envKeystoreFile = System.getenv("KEYSTORE_FILE")
val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
val envKeyAlias = System.getenv("KEY_ALIAS")
val envKeyPassword = System.getenv("KEY_PASSWORD")

val keystorePropertiesFile = rootProject.file("signing/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val resolvedStoreFilePath = envKeystoreFile
    ?: keystoreProperties.getProperty("storeFile")
    ?: "signing/release.jks"
val resolvedStorePassword = envKeystorePassword
    ?: keystoreProperties.getProperty("storePassword")
val resolvedKeyAlias = envKeyAlias
    ?: keystoreProperties.getProperty("keyAlias")
val resolvedKeyPassword = envKeyPassword
    ?: keystoreProperties.getProperty("keyPassword")

val versionPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties()
if (versionPropertiesFile.exists()) {
    versionProperties.load(versionPropertiesFile.inputStream())
}
val vMajor = versionProperties.getProperty("VERSION_MAJOR", "1").toInt()
val vMinor = versionProperties.getProperty("VERSION_MINOR", "0").toInt()
val vPatch = versionProperties.getProperty("VERSION_PATCH", "0").toInt()
val vBuild = versionProperties.getProperty("VERSION_BUILD", "1").toInt()
// major*10_000_000 + minor*100_000 + patch*1_000 + build
// 1.3.1 build 1 stays above 1.3.0 build 21. Do not add patch and build together.
require(vMinor in 0..99 && vPatch in 0..99 && vBuild in 0..999) {
    "version.properties exceeds versionCode limits (minor/patch 0..99, build 0..999)"
}
val computedVersionCode = vMajor * 10_000_000 + vMinor * 100_000 + vPatch * 1_000 + vBuild
val computedVersionName = "$vMajor.$vMinor.$vPatch"

android {
    namespace = "com.soh3il.calendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soh3il.calendar"
        minSdk = 26
        targetSdk = 35
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    val keyStoreFile = File(resolvedStoreFilePath).let {
        if (it.isAbsolute) it else rootProject.file(resolvedStoreFilePath)
    }
    val hasReleaseKeystore = keyStoreFile.exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            val storePassword = resolvedStorePassword
                ?: error("Release signing requires KEYSTORE_PASSWORD or signing/keystore.properties storePassword")
            val keyAlias = resolvedKeyAlias
                ?: error("Release signing requires KEY_ALIAS or signing/keystore.properties keyAlias")
            val keyPassword = resolvedKeyPassword
                ?: error("Release signing requires KEY_PASSWORD or signing/keystore.properties keyPassword")
            create("release") {
                storeFile = keyStoreFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (!hasReleaseKeystore) {
                throw GradleException(
                    "Refusing to sign the release APK with the debug certificate. " +
                        "Set KEYSTORE_FILE or add an untracked signing/release.jks."
                )
            }
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // This prebuilt library has no strip-compatible symbol table.
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }

    lint {
        ignoreTestSources = true
        checkTestSources = false
        abortOnError = true
        disable += "RememberInComposition"
        disable += "FrequentlyChangingValue"
        disable += "NullSafeMutableLiveData"
        disable += "AutoboxingStateCreation"
        disable += "ObsoleteLintCustomCheck"
        disable += "GradleDependency"
        disable += "AndroidGradlePluginVersion"
        disable += "ObsoleteSdkInt"
        disable += "IconLauncherShape"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:jalali"))
    implementation(project(":core:database"))
    implementation(project(":core:scheduler"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:updater"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:reminder"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}
