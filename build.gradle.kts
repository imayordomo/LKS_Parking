// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Forcing a modern version of Bouncy Castle at the buildscript level
        classpath("org.bouncycastle:bcprov-jdk18on:1.80")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "7.3.1.8318"
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            // Force the same version across all subprojects
            force("org.bouncycastle:bcprov-jdk18on:1.80")
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "imayordomo_LKS_Parking")
        property("sonar.organization", "imayordomo")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
