import java.nio.file.Files
import com.rohanprabhu.gradle.plugins.kdjooq.*

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.flywaydb.flyway") version "5.2.4"
    id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.6"
}

description = ""
val buildDbPath = Files.createTempFile("dropit-build", ".db").toFile()
buildDbPath.deleteOnExit()

dependencies {
    api(project(":common"))
    
    api("org.xerial:sqlite-jdbc:${project.extra["sqlite_version"]}")
    jooqGeneratorRuntime("org.xerial:sqlite-jdbc:${project.extra["sqlite_version"]}")
    jooqGeneratorRuntime("org.glassfish.jaxb:jaxb-runtime:2.3.1")
    jooqGeneratorRuntime("com.sun.activation:javax.activation:1.2.0")

    api("com.zaxxer:HikariCP:${project.extra["hikari_version"]}")
    api("org.flywaydb:flyway-core:${project.extra["flyway_version"]}")
    api(group = "org.jooq", name = "jooq", version = "3.14.3")
    api("org.slf4j:jul-to-slf4j:${project.extra["slf4j_version"]}")
    api("com.google.dagger:dagger:${project.extra["dagger_version"]}")
    api("io.javalin:javalin:${project.extra["javalin_version"]}")
    api("com.fasterxml.jackson.core:jackson-databind:${project.extra["jackson_version"]}")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jackson_version"]}")
    api("org.apache.commons:commons-lang3:${project.extra["lang3_version"]}")
    api("commons-fileupload:commons-fileupload:${project.extra["fileupload_version"]}")
    implementation(kotlin("stdlib", Deps.Plugins.KOTLIN))
    implementation(kotlin("reflect",Deps.Plugins.KOTLIN))
    api("javax.annotation:javax.annotation-api:1.3.2")
}

jooqGenerator {
    jooqVersion = "3.14.3"
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
//                    includeSequences = false
//                    includePrimaryKeys = false
//                    includeUniqueKeys = false
//                    includeForeignKeys = false
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
