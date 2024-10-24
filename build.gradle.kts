// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Updated Kotlin version
    id("com.google.dagger.hilt.android") version "2.50" apply false // Updated Hilt version
    id("com.google.gms.google-services") version "4.4.0" apply false
}

ext {
    set("kotlin_version", "1.9.22") // Updated to match the plugin version
    set("java_version", JavaVersion.VERSION_17)
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.1") // Matches your Android Gradle Plugin version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22") // Updated Kotlin Gradle plugin
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50") // Updated Hilt version
    }
}
