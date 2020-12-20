import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.undercouch.gradle.tasks.download.Download
import dropitconf.BuildPlatform
import dropitconf.Deps
import org.jooq.meta.jaxb.ForcedType
import java.nio.file.Paths

plugins {
    id("dependencies-plugin")
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
    id("edu.sc.seis.macAppBundle")
    id("org.flywaydb.flyway")
    id("nu.studer.jooq")
    id("application")
    id("de.undercouch.download")
}

val zipFile by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "zipfile"))
    }
}

dependencies {
    if (BuildPlatform.current == BuildPlatform.WINDOWS) {
        zipFile(project(":windows-wrapper"))
    }
    api(project(":common"))
    api(Deps.sqliteJdbc)
    jooqGenerator(Deps.sqliteJdbc)
//    jooqGenerator(Deps.jaxbRuntime)
//    jooqGenerator(Deps.javaxActivation)

    api(Deps.hikariCP)
    api(Deps.flywayCore)
    api(Deps.jooq)
    api(Deps.julToSlf4j)
    api(Deps.dagger)
    api(Deps.javalin)
    api(Deps.jacksonDatabind)
    api(Deps.jacksonModuleKotlin)
    api(Deps.jacksonDataformatYaml)
    api(Deps.commonsLang3)
    api(Deps.commonsFileupload)
    api(Deps.javaxAnnotationApi)
    api(Deps.jsr305)
    api(Deps.logbackClassic)
    api(Deps.dagger)
    api(Deps.javaxAnnotationApi)

    testImplementation(Deps.junitJupiterEngine)
    testImplementation(kotlin("test-junit5"))
    testImplementation(Deps.spekDslJvm)
    testRuntimeOnly(Deps.spekRunnerJunit5)

    // UI
    api(Deps.swt)
    api(Deps.qrGenJavaSe)
    api(Deps.arrowCore)

    kapt(Deps.daggerCompiler)
}

description = ""
val buildDbPath = Paths.get("$buildDir/build-db/build.db")
val windowsBuildDir by extra { "$buildDir/windows" }

application {
    mainClass.set("dropit.ApplicationKt")
    mainClassName = "dropit.ApplicationKt"
    applicationName = "DropIt"
}

tasks {
    val shadowJar by named<ShadowJar>("shadowJar") {
        archiveBaseName.set("dropit-desktop")
        archiveClassifier.set(BuildPlatform.current.desktopClassifier)
    }

    val downloadWindowsJre by registering(Download::class) {
        src("https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1%2B9/OpenJDK15U-jre_x64_windows_hotspot_15.0.1_9.zip")
        dest("$buildDir/jre-zip/jre.zip")
    }

    val windowsDistroZip by registering(Zip::class) {
        from(shadowJar.archiveFile) {
            rename(".+", "dropit.jar")
        }
        from(zipTree(downloadWindowsJre.get().dest))
        from(zipFile.resolve().map { zipTree(it) })
        include("**/*")
        eachFile {
            if (path.startsWith("jdk-")) {
                val splitPath = ArrayList(path.split("/"))
                splitPath[0] = "jre"
                path = splitPath.joinToString("/")
            }
        }
        includeEmptyDirs = false
        archiveFileName.set("dropit-desktop-${project.version}-win32.zip")
        destinationDirectory.set(file("$buildDir/windows-distro-zip"))
        dependsOn(downloadWindowsJre, zipFile)
    }

    val generateKotlin by registering(Sync::class) {
        val props = HashMap<String, Any>()
        props["version"] = project.version
        inputs.properties(props)
        from("src/template/kotlin")
        into("$buildDir/generated-template")
        expand(props)
    }

    val compileKotlin by existing {
        dependsOn.add(generateKotlin.get())
    }

    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            events("PASSED", "STARTED", "FAILED", "SKIPPED")
        }
    }

    build {
        dependsOn(shadowJar)
    }

    val prepareBuildDbPath by registering {
        mustRunAfter("clean")
        doLast {
            mkdir(buildDbPath.parent)
        }
    }

    val flywayMigrate by existing {
        dependsOn(prepareBuildDbPath)
    }
}

sourceSets["main"].java.srcDir("$buildDir/generated-template")

macAppBundle {
    javaExtras["-XstartOnFirstThread"] = null
    javaExtras["-d64"] = null
    mainClassName = "dropit.ApplicationKt"
    appName = "DropIt"
}

jooq {
    version.set(Deps.JOOQ_VERSION)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.sqlite.JDBC"
                    url = "jdbc:sqlite:$buildDbPath"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    database.apply {
                        excludes = "flyway_schema_history"
                        schemaVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
                        catalogVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
                        forcedTypes.addAll(
                            arrayOf(
                                ForcedType()
                                    .withName("uuid")
                                    .withIncludeExpression("(.*\\.(.+_){0,1}id|phone\\.token)"),
                                ForcedType()
                                    .withName("boolean")
                                    .withIncludeExpression("(settings\\.(separate_transfer_folders|open_transfer_on_completion|log_clipboard_transfers|keep_window_on_top)|transfer\\.send_to_clipboard)"),
                                ForcedType()
                                    .withUserType("dropit.application.model.TransferSource")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("clipboard_log\\.source"),
                                ForcedType()
                                    .withUserType("dropit.application.dto.TokenStatus")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("phone\\.status"),
                                ForcedType()
                                    .withUserType("dropit.application.model.ShowFileAction")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("(settings\\.show_transfer_action|file_type_settings\\.show_action)"),
                                ForcedType()
                                    .withUserType("dropit.application.model.ClipboardFileDestination")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("file_type_settings\\.clipboard_destination"),
                                ForcedType()
                                    .withUserType("dropit.application.dto.TransferStatus")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("transfer\\.status"),
                                ForcedType()
                                    .withUserType("dropit.application.dto.FileStatus")
                                    .withEnumConverter(true)
                                    .withIncludeExpression("transfer_file\\.status")
                            ).toList()
                        )
                    }
                    generate.apply {
                        isPojos = true
                        isNonnullAnnotation = true
                        isNullableAnnotation = true
                    }
                    target.apply {
                        packageName = "dropit.jooq"
                    }
                }
            }
        }
    }
}

val generateJooq by project.tasks
generateJooq.dependsOn(tasks.flywayMigrate)

flyway {
    url = "jdbc:sqlite:$buildDbPath"
    mixed = true
}
