import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin { compilerOptions.jvmTarget = JvmTarget.JVM_11 }

android {
    namespace = "it.danielebufarini.sleepify"
    compileSdk = 37
    defaultConfig {
        applicationId = "it.danielebufarini.sleepify"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":shared:app"))
    implementation(libs.decompose)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.compose.test.manifest)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
}
