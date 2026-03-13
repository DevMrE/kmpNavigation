import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)

    id("maven-publish")
}

group = "io.github.devmre"
version = "1.3.0-alpha07"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    androidTarget("android") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

//    jvm("desktop") {
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_17)
//        }
//    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.lifecycle)
            implementation(libs.composeNavigation)
            implementation(libs.bundles.koin)
            implementation(libs.logger)
            implementation(libs.navigation3)
        }

        androidMain.dependencies {
            // hier Android-spezifische deps
        }

        iosMain.dependencies {
            // hier iOS-spezifische deps
        }
    }
}

android {
    namespace = "com.kmp.navigation"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Maven Publishing
publishing {
    publications {
        // Nur existierende Targets, Desktop weg
        create<MavenPublication>("navigation") {
            from(components["kotlin"])
            groupId = "com.workstation.kmp"
            artifactId = "navigation"
            version = "1.0.0"
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
}