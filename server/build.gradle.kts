import java.nio.file.Files
import com.rohanprabhu.gradle.plugins.kdjooq.*

plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
    use(Deps.Plugins.flyway)
    use(Deps.Plugins.jooq)
}

description = ""
val buildDbPath = Files.createTempFile("dropit-build", ".db").toFile()
buildDbPath.deleteOnExit()

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
                database {
                    includes = ".*"
                    excludes = "flyway_schema_history"
                    isIncludeSequences = false
                    isIncludePrimaryKeys = false
                    isIncludeUniqueKeys = false
                    isIncludeForeignKeys = false
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
}
