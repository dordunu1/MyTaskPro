// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.google.dagger.hilt.android") version "2.44" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false // Add this line
}

ext {
    set("kotlin_version", "1.8.10")
    set("java_version", JavaVersion.VERSION_17)
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:2.44")

    }
}