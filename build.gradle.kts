plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "5.1.0.4882"
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

sonar {
    properties {
        property("sonar.projectKey", "imayordomo_LKS_Parking")
        property("sonar.organization", "imayordomo")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
