plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "5.1.0.4882"
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.detekt) apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
}

sonar {
    properties {
        property("sonar.projectKey", "imayordomo_LKS_Parking")
        property("sonar.organization", "imayordomo")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.projectDir}/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.android.lint.reportPaths", "${project.projectDir}/app/build/reports/lint-results-debug.xml")
        property("sonar.coverage.exclusions", "**/R.class,**/R\$*.class,**/BuildConfig.*,**/Manifest*.*,**/*Test*.*,android/**/*.*,**/ui/pages/**,**/ui/components/**,**/ui/theme/**,**/MainActivity.*,**/MyFirebaseMessagingService.*")
    }
}