repositories {
    google()
    gradlePluginPortal()
    jcenter()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    val kotlinVersion = "1.4.21"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
}

gradlePlugin {
    plugins.register("dependencies-plugin") {
        id = "dependencies-plugin"
        implementationClass = "dropitconf.DependenciesPlugin"
    }
}