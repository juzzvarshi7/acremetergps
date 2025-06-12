// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
buildscript {
    repositories {
        google()
        mavenCentral() // Or jcenter() - Google recommends mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath(libs.google.services) // Firebase plugin
        classpath(libs.gradle)
    }
}
