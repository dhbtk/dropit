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

//    resolutionStrategy {
//        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.4.10")
//        force("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")
//        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.10")
//        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.10")
//        force("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
//    }
}
