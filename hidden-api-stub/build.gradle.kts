import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension>("android") {
    namespace = "com.dsu.extended"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
    }

    buildTypes {
        create("miniDebug")
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
