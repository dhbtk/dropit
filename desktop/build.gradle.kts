import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.undercouch.gradle.tasks.download.Download
import org.jooq.meta.jaxb.ForcedType
import java.nio.file.Paths

plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
    use(Deps.Plugins.shadow)
    use(Deps.Plugins.launch4j)
    use(Deps.Plugins.macAppBundle)
    use(Deps.Plugins.flyway)
    use(Deps.Plugins.jooq)
    id("application")
    id("de.undercouch.download")
}

description = ""
project.buildDir.mkdirs()
val buildTmpPath = Paths.get(project.buildDir.toString(), "tmp")
buildTmpPath.toFile().mkdirs()
val buildDbPath = Paths.get(buildTmpPath.toString(), "build.db")

application {
    mainClass.set("dropit.ApplicationKt")
    mainClassName = "dropit.ApplicationKt"
    applicationName = "DropIt"
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("dropit-desktop")
    archiveClassifier.set(BuildPlatform.current.desktopClassifier)
}

launch4j {
    icon = "${projectDir}/desktop.ico"
    outfile = "DropIt.exe"
    mainClassName = "dropit.ApplicationKt"
    val shadowJar = project.tasks.named("shadowJar").get() as ShadowJar
    copyConfigurable = emptyArray<String>()
    jar = "${project.buildDir}/libs/${shadowJar.archiveBaseName.get()}-${project.version}-${shadowJar.archiveClassifier.get()}.jar"
    print("Jar is $jar")
    bundledJrePath = "jdk-15.0.1+9-jre"
}

tasks.named("createExe").configure {
    dependsOn(tasks.assemble)
}

task<Download>("downloadWindowsJre") {
    src("https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.1%2B9/OpenJDK15U-jre_x64_windows_hotspot_15.0.1_9.zip")
    dest(File(File(project.buildDir, "jre"), "jre.zip"))
}

task<Copy>("unzipWindowsJre") {
    dependsOn("downloadWindowsJre")
    from(zipTree(tasks.named<Download>("downloadWindowsJre").get().dest))
    into("${project.buildDir}/launch4j")
}

task<Zip>("windowsDistroZip") {
    from("${project.buildDir}/launch4j")
    include("**/*")
    archiveFileName.set("dropit-desktop-${project.version}-win32.zip")
    destinationDirectory.set(project.buildDir)

    dependsOn(tasks.named("createExe"), tasks.named("unzipWindowsJre"))
}

macAppBundle {
    javaExtras["-XstartOnFirstThread"] = null
    javaExtras["-d64"] = null
    mainClassName = "dropit.ApplicationKt"
    appName = "DropIt"
}

dependencies {
    api(project(":common"))
    api(Deps.sqliteJdbc)
    jooqGenerator(Deps.sqliteJdbc)
    jooqGenerator(Deps.jaxbRuntime)
    jooqGenerator(Deps.javaxActivation)

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
    implementation(Deps.logbackClassic)
    implementation(Deps.dagger)
//    implementation("io.arrow-kt:arrow-core-extensions:$arrow_version")
    implementation(Deps.javaxAnnotationApi)

    testImplementation(Deps.junitJupiterEngine)
    testImplementation(kotlin("test-junit5"))
    testImplementation(Deps.spekDslJvm)  {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly (Deps.spekRunnerJunit5) {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }

    // UI
    implementation(Deps.swtRuntime) {
        exclude( group = "org.eclipse.platform")
    }
    implementation(Deps.swt) {
        exclude( group = "org.eclipse.platform")
    }
    implementation(Deps.qrGenJavaSe)
    implementation(Deps.arrowCore)

    kapt(Deps.daggerCompiler)
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
//                    schemaVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    catalogVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    withSyntheticPrimaryKeys(".+\\.(id|ID)")
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
generateJooq.dependsOn("flywayMigrate")

tasks.clean {
    delete(buildDbPath)
}

flyway {
    url = "jdbc:sqlite:$buildDbPath"
    mixed = true
}

tasks {
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
}
