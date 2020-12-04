import java.nio.file.Paths
import org.jooq.meta.jaxb.ForcedType
import java.io.File

plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
    use(Deps.Plugins.flyway)
    use(Deps.Plugins.jooq)
}

description = ""
project.buildDir.mkdirs()
val buildTmpPath = Paths.get(project.buildDir.toString(), "tmp")
buildTmpPath.toFile().mkdirs()
val buildDbPath = Paths.get(buildTmpPath.toString(), "build.db")

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
    api(Deps.commonsLang3)
    api(Deps.commonsFileupload)
    api(Deps.javaxAnnotationApi)
    api(Deps.jsr305)
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
                    name = "org.jooq.codegen.JavaGenerator"
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    database.apply {
                        excludes = "flyway_schema_history"
//                    schemaVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    catalogVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    withSyntheticPrimaryKeys(".+\\.(id|ID)")
                        forcedTypes.addAll(arrayOf(
                            ForcedType()
                                .withName("uuid")
                                .withIncludeExpression("(.*\\.(.+_){0,1}id|phone\\.token)")
                            ,ForcedType()
                                .withName("boolean")
                                .withIncludeExpression("(settings\\.(separate_transfer_folders|open_transfer_on_completion|log_clipboard_transfers|keep_window_on_top)|transfer\\.send_to_clipboard)")
                            ,ForcedType()
                                .withUserType("dropit.domain.entity.TransferSource")
                                .withEnumConverter(true)
                                .withIncludeExpression("clipboard_log\\.source")
                            ,ForcedType()
                                .withUserType("dropit.application.dto.TokenStatus")
                                .withEnumConverter(true)
                                .withIncludeExpression("phone\\.status")
                            ,ForcedType()
                                .withUserType("dropit.domain.entity.ShowFileAction")
                                .withEnumConverter(true)
                                .withIncludeExpression("(settings\\.show_transfer_action|file_type_settings\\.show_action)")
                            ,ForcedType()
                                .withUserType("dropit.domain.entity.ClipboardFileDestination")
                                .withEnumConverter(true)
                                .withIncludeExpression("file_type_settings\\.clipboard_destination")
                            ,ForcedType()
                                .withUserType("dropit.application.dto.TransferStatus")
                                .withEnumConverter(true)
                                .withIncludeExpression("transfer\\.status")
                            ,ForcedType()
                                .withUserType("dropit.application.dto.FileStatus")
                                .withEnumConverter(true)
                                .withIncludeExpression("transfer_file\\.status")
                        ).toList())
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
