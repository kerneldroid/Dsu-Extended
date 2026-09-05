import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jmailen.kotlinter")
}

apply(from = "$rootDir/signing.gradle")

extensions.configure<ApplicationExtension>("android") {
    val packageName = rootProject.extra["packageName"] as String
    val enableAbiSplits =
        (project.findProperty("ENABLE_ABI_SPLITS") as? String)
            ?.equals("true", ignoreCase = true)
            ?: false

    namespace = packageName
    compileSdk = 37

    defaultConfig {
        applicationId = packageName
        this.versionCode = 21
        this.versionName = "1.2.5"
        val updateCheckUrl =
            (project.findProperty("UPDATE_CHECK_URL") as? String)
                ?: "https://raw.githubusercontent.com/kerneldroid/Dsu-Extended/master/other/updater.json"
        val authorSignDigest =
            (project.findProperty("AUTHOR_SIGN_DIGEST") as? String)
                ?: "f20611c8371f47af456a81d3682e31955655fec43178bb5d5d6d40778e85878b"
        buildConfigField("String", "UPDATE_CHECK_URL", "\"$updateCheckUrl\"")
        buildConfigField("String", "AUTHOR_SIGN_DIGEST", "\"$authorSignDigest\"")
        minSdk = 29
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Configured in signing.gradle (local props file or CI environment variables).
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("miniDebug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    splits {
        abi {
            isEnable = enableAbiSplits
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(26)
    compilerOptions {
        moduleName.set("app")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0-alpha10")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3:1.5.0-alpha26")
    implementation("androidx.compose.material3:material3-window-size-class:1.5.0-alpha26")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha26")
    // Glance AppWidget (stable 1.1.1; 1.2.0 exists only as 1.2.0-rc01 per 2026-07-01 release notes)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.13.0-alpha01")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.0-alpha06")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("org.tukaani:xz:1.12")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.mikepenz:aboutlibraries-core:11.2.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.materialkolor:material-kolor:5.0.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("io.github.iamr0s:Dhizuku-API:2.6.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    compileOnly(project(":hidden-api-stub"))
}
