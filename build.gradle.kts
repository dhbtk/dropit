allprojects {
    group = "io.edanni"
    version = "0.1"

    repositories {
        google()
        jcenter()
        mavenLocal()
        maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
        maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        maven("https://jitpack.io")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

buildscript {
    val kotlin_version by extra("1.4.21")
    repositories {
        google()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(Deps.sqliteJdbc)
        classpath(Deps.Plugins.androidGradle)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
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

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib-common:${Deps.Plugins.KOTLIN}")
            force("org.jetbrains.kotlin:kotlin-stdlib:${Deps.Plugins.KOTLIN}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Deps.Plugins.KOTLIN}")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.Plugins.KOTLIN}")
            force("org.jetbrains.kotlin:kotlin-reflect:${Deps.Plugins.KOTLIN}")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.apiVersion = "1.4"
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}
