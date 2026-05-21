pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins { kotlin("multiplatform") version "2.3.21" }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "http-kotlin"

val bytesLocal = file("../bytes-kotlin")
if (bytesLocal.exists()) {
    includeBuild(bytesLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:bytes-kotlin")).using(project(":"))
            substitute(module("io.github.kotlinmania:bytes-kotlin-android")).using(project(":"))
        }
    }
}
