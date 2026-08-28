plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false

    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    id("org.jmailen.kotlinter") version "5.7.0" apply false
}

val packageName = extra.set("packageName", "com.dsu.extended")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
