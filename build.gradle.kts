plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.library") version "9.2.1" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false

    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("org.jmailen.kotlinter") version "5.4.0" apply false
}

val versionCode = extra.set("versionCode", 15)
val versionName = extra.set("versionName", "1.1")
val packageName = extra.set("packageName", "com.dsu.extended")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
