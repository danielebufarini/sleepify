import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    base
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.skie) apply false
}

val defaultIosSimulator = providers.exec {
    commandLine("xcrun", "simctl", "list", "devices", "available", "-j")
}.standardOutput.asText.map { output ->
    @Suppress("UNCHECKED_CAST")
    val devices = (groovy.json.JsonSlurper().parseText(output) as Map<String, Any?>)["devices"]
        as Map<String, List<Map<String, Any?>>>
    val phones = devices.filterKeys {
        it.substringAfter("SimRuntime.iOS-", "").substringBefore('-').toIntOrNull()?.let { version -> version >= 27 } == true
    }.values.flatten().filter { it["isAvailable"] == true && (it["name"] as? String)?.startsWith("iPhone") == true }
    val selected = phones.firstOrNull { it["name"] == "iPhone 17" } ?: phones.firstOrNull()
        ?: throw GradleException("Install an iOS 27+ iPhone simulator in Xcode.")
    selected["udid"] as String
}

subprojects {
    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        // Override with -Psleepify.iosSimulator=<UDID> when necessary.
        device.set(providers.gradleProperty("sleepify.iosSimulator").orElse(defaultIosSimulator))
    }
}

tasks.register("verifyAll") {
    group = "verification"
    description = "Shared tests on Android host and iOS simulator, Android lint and debug APK."
    dependsOn(":shared:app:testAndroidHostTest", ":shared:app:iosSimulatorArm64Test",
        ":androidApp:lintDebug", ":androidApp:assembleDebug")
}
