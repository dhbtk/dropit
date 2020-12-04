import java.nio.file.Paths
import com.rohanprabhu.gradle.plugins.kdjooq.*

plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
    use(Deps.Plugins.flyway)
    use(Deps.Plugins.jooq)
}

description = ""
val buildDbPath = Paths.get(project.buildDir.toString(), "tmp", "build.db")

dependencies {
    api(project(":common"))
    
    api(Deps.sqliteJdbc)
    jooqGeneratorRuntime(Deps.sqliteJdbc)
    jooqGeneratorRuntime(Deps.jaxbRuntime)
    jooqGeneratorRuntime(Deps.javaxActivation)

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
}

jooqGenerator {
    jooqVersion = Deps.JOOQ_VERSION
    configuration("primary", project.sourceSets.getByName("main")) {
        configuration = jooqCodegenConfiguration {
            jdbc {
                driver = "org.sqlite.JDBC"
                url = "jdbc:sqlite:$buildDbPath"
            }
            generator {
                name = "org.jooq.codegen.KotlinGenerator"
                database {
                    includes = ".*"
                    excludes = "flyway_schema_history"
//                    schemaVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    catalogVersionProvider = "SELECT MAX(version) FROM flyway_schema_history"
//                    withSyntheticPrimaryKeys(".+\\.(id|ID)")
                    forcedTypes {
                        forcedType {
                            name = "uuid"
                            includeExpression = "(.*\\.(.+_){0,1}id|phone\\.token)"
                        }
                        forcedType {
                            name = "boolean"
                            includeExpression = "(settings\\.(separate_transfer_folders|open_transfer_on_completion|log_clipboard_transfers|keep_window_on_top)|transfer\\.send_to_clipboard)"
                        }
                        forcedType {
                            userType = "dropit.domain.entity.TransferSource"
                            isEnumConverter = true
                            includeExpression = "clipboard_log\\.source"
                        }
                        forcedType {
                            userType = "dropit.application.dto.TokenStatus"
                            isEnumConverter = true
                            includeExpression = "phone\\.status"
                        }
                        forcedType {
                            userType = "dropit.domain.entity.ShowFileAction"
                            isEnumConverter = true
                            includeExpression = "(settings\\.show_transfer_action|file_type_settings\\.show_action)"
                        }
                        forcedType {
                            userType = "dropit.domain.entity.ClipboardFileDestination"
                            isEnumConverter = true
                            includeExpression = "file_type_settings\\.clipboard_destination"
                        }
                        forcedType {
                            userType = "dropit.application.dto.TransferStatus"
                            isEnumConverter = true
                            includeExpression = "transfer\\.status"
                        }
                        forcedType {
                            userType = "dropit.application.dto.FileStatus"
                            isEnumConverter = true
                            includeExpression = "transfer_file\\.status"
                        }
                    }
                }
                target {
                    packageName = "dropit.jooq"
                }
            }
        }
    }
}

val `jooq-codegen-primary` by project.tasks
`jooq-codegen-primary`.dependsOn("flywayMigrate")

tasks.clean {
    delete(buildDbPath)
}

flyway {
    url = "jdbc:sqlite:$buildDbPath"
    mixed = true
}
