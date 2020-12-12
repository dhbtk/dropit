object Deps {
    object Plugins {
        const val ANDROID_GRADLE = "4.0.2"
        const val KOTLIN = "1.4.21"

        const val androidGradle = "com.android.tools.build:gradle:$ANDROID_GRADLE"

        val kotlinJvm = Plugin("org.jetbrains.kotlin.jvm")
        val kotlinKapt = Plugin("org.jetbrains.kotlin.kapt")
        val shadow = Plugin("com.github.johnrengelman.shadow", "6.1.0")
        val launch4j = Plugin("edu.sc.seis.launch4j", "2.4.9")
        val macAppBundle = Plugin("edu.sc.seis.macAppBundle", "2.3.0")
        val flyway = Plugin("org.flywaydb.flyway", FLYWAY_VERSION)
        val jooq = Plugin("nu.studer.jooq", "5.2")
    }

    data class Plugin(val id: String, val version: String? = null)

    const val FLYWAY_VERSION = "7.2.0"
    const val DAGGER_VERSION = "2.30.1"
    const val JACKSON_VERSION = "2.12.0"
    const val RETROFIT_VERSION = "2.9.0"
    const val SPEK_VERSION = "2.0.13"
    const val SWT_VERSION = "3.115.0"
    const val JOOQ_VERSION = "3.14.4"

    const val androidRetrofuture = "net.sourceforge.streamsupport:android-retrofuture:1.7.0"
    const val androidTestJunit = "androidx.test.ext:junit:1.1.2"
    const val ankoSqlite = "org.jetbrains.anko:anko-sqlite:0.10.8"
    const val appCompat = "androidx.appcompat:appcompat:1.2.0"
    const val arrowCore = "io.arrow-kt:arrow-core:0.11.0"

    const val commonsFileupload = "commons-fileupload:commons-fileupload:1.4"
    const val commonsLang3 = "org.apache.commons:commons-lang3:3.11"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.4"

    const val dagger = "com.google.dagger:dagger:$DAGGER_VERSION"
    const val daggerAndroid = "com.google.dagger:dagger-android:$DAGGER_VERSION"
    const val daggerAndroidProcessor = "com.google.dagger:dagger-android-processor:$DAGGER_VERSION"
    const val daggerAndroidSupport = "com.google.dagger:dagger-android-support:$DAGGER_VERSION"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:$DAGGER_VERSION"

    const val espressoCore = "androidx.test.espresso:espresso-core:3.3.0"

    const val flywayCore = "org.flywaydb:flyway-core:$FLYWAY_VERSION"

    const val hikariCP = "com.zaxxer:HikariCP:3.4.5"

    const val jacksonDatabind = "com.fasterxml.jackson.core:jackson-databind:$JACKSON_VERSION"
    const val jacksonDataformatYaml = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$JACKSON_VERSION"
    const val jacksonModuleKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$JACKSON_VERSION"
    const val javalin = "io.javalin:javalin:3.11.0"
    const val javaxActivation = "com.sun.activation:javax.activation:1.2.0"
    const val javaxAnnotationApi = "javax.annotation:javax.annotation-api:1.3.2"
    const val jaxbRuntime = "org.glassfish.jaxb:jaxb-runtime:2.3.1"
    const val jooq = "org.jooq:jooq:$JOOQ_VERSION"
    const val jsr305 = "com.google.code.findbugs:jsr305:3.0.2"
    const val julToSlf4j = "org.slf4j:jul-to-slf4j:1.7.30"
    const val junit = "junit:junit:4.12"
    const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:5.3.2"

    const val legacySupportV4 = "androidx.legacy:legacy-support-v4:1.0.0"
    const val logbackClassic = "ch.qos.logback:logback-classic:1.2.3"
    const val material = "com.google.android.material:material:1.2.1"

    const val okHttp = "com.squareup.okhttp3:okhttp:4.9.0"
    const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:4.9.0"

    const val qrGenJavaSe = "com.github.kenglxn.QRGen:javase:2.6.0"

    const val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
    const val retrofit = "com.squareup.retrofit2:retrofit:$RETROFIT_VERSION"
    const val retrofitConverterJackson = "com.squareup.retrofit2:converter-jackson:$RETROFIT_VERSION"
    const val retrofitConverterScalars = "com.squareup.retrofit2:converter-scalars:$RETROFIT_VERSION"
    const val rxJava = "io.reactivex.rxjava2:rxjava:2.2.4"

    const val spekDslJvm = "org.spekframework.spek2:spek-dsl-jvm:$SPEK_VERSION"
    const val spekRunnerJunit5 = "org.spekframework.spek2:spek-runner-junit5:$SPEK_VERSION"
    const val sqliteJdbc = "org.xerial:sqlite-jdbc:3.21.0.1"
    const val swt = "org.eclipse.platform:org.eclipse.swt:$SWT_VERSION"
    val swtRuntime = "org.eclipse.platform:org.eclipse.swt.${BuildPlatform.current.swtRuntime}:$SWT_VERSION"
}

enum class BuildPlatform(private val targetPlatform: String, val swtRuntime: String) {
    WINDOWS("win32", "win32.win32.x86_64"),
    LINUX("linux", "gtk.linux.x86_64"),
    MACOS("osx", "cocoa.macosx.x86_64");

    val desktopClassifier: String
            get() = targetPlatform

    companion object {
        val current: BuildPlatform
            get() {
                for (platform in values()) {
                    if (platform.targetPlatform == System.getProperty("targetPlatform")) {
                        return platform
                    }
                }
                val osName = System.getProperty("os.name").toLowerCase()
                return when {
                    osName.contains("mac os") -> MACOS
                    osName.contains("windows") -> WINDOWS
                    else -> LINUX
                }
            }
    }
}

fun org.gradle.plugin.use.PluginDependenciesSpec.use(plugin: Deps.Plugin) {
    if (plugin.version == null) {
        this.id(plugin.id)
    } else {
        this.id(plugin.id).version(plugin.version)
    }
}
