import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("application")
    id("edu.sc.seis.launch4j") version "2.4.4"
    id("edu.sc.seis.macAppBundle") version "2.3.0"
//    id("idea")
}

description = ""

application {
    mainClassName = "dropit.ApplicationKt"
    applicationName = "DropIt"
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("dropit-desktop")
    archiveClassifier.set(project.extra["desktopClassifier"] as String)
}

launch4j {
    icon = "${projectDir}/desktop.ico"
    outfile = "DropIt.exe"
    mainClassName = "dropit.ApplicationKt"
    val shadowJar = project.tasks.named("shadowJar").get() as ShadowJar
    copyConfigurable = shadowJar.outputs.files
    jar = "lib/${shadowJar.archiveBaseName}"
    bundledJrePath = "jre"
}

macAppBundle {
    javaExtras.put("-XstartOnFirstThread", null)
    javaExtras.put("-d64", null)
    mainClassName = "dropit.ApplicationKt"
    appName = "DropIt"
}



//idea {
//    module {
//        sourceDirs.addAll(files("build/generated/source/kapt/main"))
//        generatedSourceDirs.addAll(files("build/generated/source/kapt/main"))
//    }
//}

dependencies {
    api(project(":server"))
    implementation("ch.qos.logback:logback-classic:${project.extra["logback_version"]}")
    implementation("com.google.dagger:dagger:${project.extra["dagger_version"]}")
    implementation("io.arrow-kt:arrow-core-data:${project.extra["arrow_version"]}")
//    implementation("io.arrow-kt:arrow-core-extensions:$arrow_version")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:${project.extra["junit_jupiter_version"]}")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${project.extra["spek_version"]}")  {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:${project.extra["spek_version"]}") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }

    // UI
    implementation("org.eclipse.platform:org.eclipse.swt.${project.extra["swt_artifact"]}:${project.extra["swt_version"]}") {
        exclude( group = "org.eclipse.platform")
    }
    implementation("org.eclipse.platform:org.eclipse.swt:${project.extra["swt_version"]}") {
        exclude( group = "org.eclipse.platform")
    }
    implementation("com.github.kenglxn.QRGen:javase:2.6.0")

    kapt("com.google.dagger:dagger-compiler:${project.extra["dagger_version"]}")
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
