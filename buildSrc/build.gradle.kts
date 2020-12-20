repositories {
    google()
    gradlePluginPortal()
    jcenter()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    val kotlinVersion = "1.4.21"
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.14.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.android.tools.build:gradle:4.1.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    implementation("edu.sc.seis:macAppBundle:2.3.0")
    implementation("gradle.plugin.org.flywaydb:gradle-plugin-publishing:7.3.2")
    implementation("nu.studer:gradle-jooq-plugin:5.2")
}
