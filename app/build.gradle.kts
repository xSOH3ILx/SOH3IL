import java.util.Properties

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
    ?: "soh3il_release_pass"
val resolvedKeyAlias = envKeyAlias
    ?: keystoreProperties.getProperty("keyAlias")
    ?: "soh3il"
val resolvedKeyPassword = envKeyPassword
    ?: keystoreProperties.getProperty("keyPassword")
    ?: "soh3il_release_pass"

val versionPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties()
if (versionPropertiesFile.exists()) {
    versionProperties.load(versionPropertiesFile.inputStream())
}
val vMajor = versionProperties.getProperty("VERSION_MAJOR", "1").toInt()
val vMinor = versionProperties.getProperty("VERSION_MINOR", "0").toInt()
val vPatch = versionProperties.getProperty("VERSION_PATCH", "0").toInt()
val vBuild = versionProperties.getProperty("VERSION_BUILD", "1").toInt()
val computedVersionCode = vMajor * 10000 + vMinor * 100 + vPatch + vBuild
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
            create("release") {
                storeFile = keyStoreFile
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
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
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        ignoreTestSources = true
        checkTestSources = false
        abortOnError = true
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
