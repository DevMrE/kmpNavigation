import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication

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

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

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

// Desktop Jar Task (for Maven-Publishing)
tasks.register<Jar>("desktopJar") {
    archiveClassifier.set("desktop")
    from(kotlin.targets["desktop"].compilations["main"].output)
}

// Maven Publishing
publishing {
    publications.withType<MavenPublication> {
        // Android AAR
        components.findByName("android")?.let { from(it) }

        // Desktop/JVM Jar
        artifact(tasks.named("desktopJar"))
    }
}
