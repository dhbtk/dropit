plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
}

description = ""

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.3.2")

    api(group = "com.google.dagger", name = "dagger", version = project.extra["dagger_version"] as String)
    api(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = project.extra["jackson_version"] as String)
    api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = project.extra["jackson_version"] as String)
    api("com.squareup.retrofit2:retrofit:${project.extra["retrofit_version"]}")
    api("com.squareup.retrofit2:converter-jackson:${project.extra["retrofit_version"]}")
    api("com.squareup.retrofit2:converter-scalars:${project.extra["retrofit_version"]}")
    api("com.squareup.okhttp3:okhttp:${project.extra["okhttp_version"]}")
    api("io.reactivex.rxjava2:rxjava:${project.extra["rxjava_version"]}")
    api("com.squareup.okhttp3:logging-interceptor:3.12.0")

    kapt(group = "com.google.dagger", name = "dagger-compiler", version = project.extra["dagger_version"] as String)
}
