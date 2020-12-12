repositories {
    google()
    gradlePluginPortal()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    val kotlinVersion = "1.4.21"
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.14.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.android.tools.build:gradle:4.1.1")
}
