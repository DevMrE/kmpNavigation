import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)

    id("maven-publish")
}

// publishing version
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

        }

        iosMain.dependencies {

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

publishing {
    publications.withType<MavenPublication> {
        // Android AAR
        if (components.findByName("android") != null) {
            from(components["android"])
        }

        // Desktop/JVM Jar
        artifact(tasks.named("desktopJar"))
    }
}

// For publishing:
//git tag v1.3.0-alpha01
//git push origin v1.3.0-alpha01
