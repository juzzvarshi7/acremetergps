plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.example.acremetergps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.acremetergps"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments.put("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("my-release-keystore.jks")
            storePassword = "my-release-store-password"
            keyAlias = "my-release-key-alias"
            keyPassword = "my-release-key-password"
        }
        getByName("debug") {
            storeFile = File(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
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
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.mpandroidchart)
    implementation(libs.room.runtime)
    implementation(libs.recyclerview)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.services)
    implementation(libs.recyclerview.v132)
    implementation(libs.room.runtime)
    implementation(libs.media3.common)
    implementation(libs.google.firebase.analytics)
    annotationProcessor(libs.room.compiler)
    annotationProcessor(libs.room.compiler)
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.wms)
    implementation(libs.retrofit2.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.play.services.maps.v1810)
    implementation(libs.volley)
    implementation(libs.gson)
    implementation(libs.anychart.android.v115)
    implementation(libs.credentials)
    implementation(libs.mpandroidchart)
    implementation(libs.itext7.core)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.gms.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.googleid)
    implementation(libs.google.googleid)
    implementation(libs.support.annotations)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.auth)
}