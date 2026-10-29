import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.skie)
}

kotlin {
    android {
        namespace = "it.danielebufarini.sleepify.shared"
        compileSdk = 37
        minSdk = 31
        compilerOptions.jvmTarget = JvmTarget.JVM_11
        withHostTest {}
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.configureEach {
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=27.0;sdkVersion.ios=27.0;osVersionMin.ios_arm64=27.0;osVersionMin.ios_simulator_arm64=27.0"
        }
        target.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
            binaryOption("bundleId", "it.danielebufarini.sleepify.shared")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.decompose)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

// Same interop boundary as Trenify: only SKIE generated bridges use Swift 5.
// First-party SwiftUI code is compiled with Swift 6 by Xcode.
skie {
    build { freeSwiftCompilerArgs.set(listOf("-swift-version", "5")) }
    swiftBundling { enabled = false }
}
