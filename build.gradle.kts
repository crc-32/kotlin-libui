// SPDX-License-Identifier: MIT OR Apache-2.0
@file:Suppress("SpellCheckingInspection")

plugins {
    kotlin("multiplatform") version Kotlin.version apply false
    id("de.undercouch.download") version Download.version apply false
    id("org.jetbrains.dokka") version "1.4.20-dev-10"
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    }
}
