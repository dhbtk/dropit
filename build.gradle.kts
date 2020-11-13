allprojects {
    apply(plugin = "maven")
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

    // versions

    // core
    extra["dagger_version"] = "2.19"
    extra["junit_jupiter_version"] = "5.3.2"
    extra["spek_version"] = "2.0.13"

    // desktop - server
    extra["sqlite_version"] = rootProject.extra["sqlite_version"]
    extra["flyway_version"] = "7.2.0"
    extra["jooq_plugin_version"] = "3.0.2"
    extra["hikari_version"] = "3.4.5"
    extra["slf4j_version"] = "1.7.30"
    extra["logback_version"] = "1.2.3"
    extra["javalin_version"] = "3.11.0"
    extra["jackson_version"] = "2.11.3"
    extra["lang3_version"] = "3.11"
    extra["fileupload_version"] = "1.4"
    extra["arrow_version"] = "0.11.0"

    // desktop - UI
    extra["swt_artifact"] = getSwtArtifact()
    extra["swt_version"] = "3.108.0"

    // client
    extra["retrofit_version"] = "2.9.0"
    extra["rxjava_version"] = "2.2.4"
    extra["okhttp_version"] = "4.9.0"

    // android
    extra["support_version"] = "27.1.1"
    extra["anko_version"] = "0.10.8"
    extra["retrofuture_version"] = "1.7.0"

    extra["desktopClassifier"] = getDesktopClassifier()
    extra["isWindowsBuild"] = isWindowsBuild()
    extra["isMacBuild"] = isMacBuild()

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

fun getSwtArtifact(): String {
    return when (System.getProperty("targetPlatform")) {
        "win32" ->
            "win32.win32.x86"
        "osx" ->
            "cocoa.macosx.x86_64"
        "linux" ->
            "gtk.linux.x86_64"
        else ->
            getDefaultSwtRuntime()
    }
}


fun getDefaultSwtRuntime(): String {
    val osName = System.getProperty("os.name").toLowerCase()
    return when {
        osName.contains("mac os") -> {
            "cocoa.macosx.x86_64"
        }
        osName.contains("windows") -> {
            "win32.win32.x86_64"
        }
        else -> {
            "gtk.linux.x86_64"
        }
    }
}

fun getDesktopClassifier(): String {
    return when (System.getProperty("desktop.platform")) {
        "win32" -> "win32"
        "osx" -> "osx"
        else -> "linux"
    }
}

fun isWindowsBuild(): Boolean {
    return getSwtArtifact() == "win32.win32.x86_64"
}

fun isMacBuild(): Boolean {
    return getSwtArtifact() == "cocoa.macosx.x86_64"
}

buildscript {
    rootProject.extra["sqlite_version"] = "3.21.0.1"
    rootProject.extra["detekt_version"] = "1.14.2"

    repositories {
        google()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(Deps.Plugins.sqliteJdbc)
        classpath(Deps.Plugins.androidGradle)
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
