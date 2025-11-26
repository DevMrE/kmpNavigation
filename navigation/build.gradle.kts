plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)

    id("maven-publish")
}

group = "com.devmre.navigation"
version = "0.1.0"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    androidTarget()
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.composeNavigation)
            implementation(libs.koinCore)
            implementation(libs.logger)
        }
    }
}

android {
    namespace = "com.devmre.navigation"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"

            url = uri("https://maven.pkg.github.com/DevMrE/kmpNavigation")

            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
