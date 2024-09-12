
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Define the ObjectBox Gradle plugin here if needed
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.2") // Example version
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

}