plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.mytaskpro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mytaskpro"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += listOf(
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/arm64-v8a/libc++_shared.so"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("androidx.navigation:navigation-compose:2.5.3")
    implementation ("androidx.glance:glance-appwidget:1.0.0-alpha05")
    implementation ("androidx.camera:camera-camera2:1.2.3")
    implementation ("com.patrykandpatrick.vico:compose:1.6.5")
    implementation ("com.patrykandpatrick.vico:compose-m3:1.12.0")
    implementation ("nl.dionsegijn:konfetti-compose:2.0.3")




    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In (add these)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // ML Kit for text recognition
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.camera:camera-mlkit-vision:1.3.0-alpha07")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    implementation ("com.google.dagger:hilt-android:2.44")
    implementation(libs.androidx.hilt.common)
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // Room
    val roomVersion = "2.5.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.5.3")

    // Calendar
    implementation("com.kizitonwose.calendar:compose:2.3.0")

    // Confetti
    implementation("nl.dionsegijn:konfetti-compose:2.0.2")

    // CameraX
    implementation("androidx.camera:camera-core:1.2.0-beta02")
    implementation("androidx.camera:camera-camera2:1.2.0-beta02")
    implementation("androidx.camera:camera-lifecycle:1.2.0-beta02")
    implementation("androidx.camera:camera-view:1.2.0-beta02")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}