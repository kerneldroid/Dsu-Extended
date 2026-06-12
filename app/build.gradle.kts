import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun getReleaseSigningConfig(): File {
    return File(".sign/dsu_extended.prop")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jmailen.kotlinter")
}

extensions.configure<ApplicationExtension>("android") {
    val versionCode: Int by rootProject.extra
    val versionName: String by rootProject.extra
    val packageName: String by rootProject.extra
    val enableAbiSplits =
        (project.findProperty("ENABLE_ABI_SPLITS") as? String)
            ?.equals("true", ignoreCase = true)
            ?: false

    namespace = packageName
    compileSdk = 37

    defaultConfig {
        applicationId = packageName
        this.versionCode = 16
        this.versionName = "1.2.0"
        val updateCheckUrl =
            (project.findProperty("UPDATE_CHECK_URL") as? String)
                ?: "https://raw.githubusercontent.com/kerneldroid/Dsu-Extended/master/other/updater.json"
        val authorSignDigest =
            (project.findProperty("AUTHOR_SIGN_DIGEST") as? String)
                ?: "48d04cac11d09be0b180d399d0c747e86e700a8c"
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
        val releaseSigningConfig = getReleaseSigningConfig()
        if (releaseSigningConfig.exists()) {
            create("release") {
                val props = Properties()
                props.load(releaseSigningConfig.inputStream())
                storeFile = File(props.getProperty("keystore"))
                storePassword = props.getProperty("keystore_pw")
                keyAlias = props.getProperty("alias")
                keyPassword = props.getProperty("alias_pw")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (getReleaseSigningConfig().exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.01"))

    implementation("androidx.appcompat:appcompat:1.8.0-alpha01")
    implementation("androidx.datastore:datastore-preferences:1.3.0-alpha09")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3:1.5.0-alpha21")
    implementation("androidx.compose.material3:material3-window-size-class:1.5.0-alpha21")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha21")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.12.0-alpha03")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0-rc01")
    implementation("androidx.navigation:navigation-compose:2.10.0-alpha05")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0-rc01")
    implementation("com.google.dagger:hilt-android:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0-rc01")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("org.tukaani:xz:1.10")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.mikepenz:aboutlibraries-core:11.2.3")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("io.github.iamr0s:Dhizuku-API:2.5.3")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    compileOnly(project(":hidden-api-stub"))
}
