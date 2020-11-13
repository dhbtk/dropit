repositories {
    google()
    gradlePluginPortal()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.14.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
    implementation("com.android.tools.build:gradle:4.0.2")
}
