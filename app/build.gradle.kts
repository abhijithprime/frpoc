import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt")
    id("io.objectbox")
}

android {
    namespace = "com.abhijith.frpoc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.abhijith.frpoc"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        setProperty("archivesBaseName", "FRPOC-$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
repositories {
    google()
    mavenCentral()
    maven { url = URI("https://maven.google.com") }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.room.runtime)
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.activity.compose.v150)
    implementation(libs.androidx.camera.core.v120)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.vision.common)
    implementation("androidx.compose.runtime:runtime-livedata:1.7.2")


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.constraintlayout.compose)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.face.detection)

    // Mediapipe Face Detection
//    implementation(libs.mediapipe)

    implementation(libs.tensorflow.lite.v2140)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support.v043)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.task.vision)
    implementation(libs.androidx.material.icons.extended)


//    implementation (libs.tasks.vision)
//    implementation (libs.solutions.facemesh)

    testImplementation(libs.junit)

    // For TensorFlow Lite

// For MediaPipe
//    implementation("com.google.mediapipe:mediapipe-tasks-vision:0.10.1")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // ObjectBox - vector database
//    implementation("io.objectbox:objectbox-android:4.0.2")
//    releaseImplementation("io.objectbox:objectbox-android:4.0.2")
//    debugImplementation("io.objectbox:objectbox-android-objectbrowser:4.0.2")
//    implementation("io.objectbox:objectbox-android-objectbrowser:4.0.2")
    implementation("io.objectbox:objectbox-android:4.0.2") {
        exclude(group = "io.objectbox", module = "objectbox-android-objectbrowser")
    }
//    implementation("io.objectbox:objectbox-android-objectbrowser:4.0.2") {
//        exclude(group = "io.objectbox", module = "objectbox-android")
//    }


    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")



}

