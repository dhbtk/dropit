import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
    use(Deps.Plugins.shadow)
    use(Deps.Plugins.launch4j)
    use(Deps.Plugins.macAppBundle)
    id("application")
}

description = ""

application {
    mainClass.set("dropit.ApplicationKt")
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
    copyConfigurable = shadowJar.outputs.files
    jar = "lib/${shadowJar.archiveBaseName}"
    bundledJrePath = "jre"
}

macAppBundle {
    javaExtras["-XstartOnFirstThread"] = null
    javaExtras["-d64"] = null
    mainClassName = "dropit.ApplicationKt"
    appName = "DropIt"
}

dependencies {
    api(project(":server"))
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
