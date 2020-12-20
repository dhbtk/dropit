allprojects {
    group = "io.edanni"

    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
        maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        maven("https://jitpack.io")
    }
}

buildscript {
    repositories {
        google()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }
}

subprojects {
    repositories {
        mavenLocal()

        maven("http://dl.bintray.com/kotlin/kotlinx")
        maven("http://jcenter.bintray.com")
        maven("https://dl.bintray.com/spekframework/spek-dev")
        maven("http://repo.maven.apache.org/maven2")
        maven("https://oss.sonatype.org/content/repositories/releases/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("http://maven-eclipse.github.io/maven")
    }
}
