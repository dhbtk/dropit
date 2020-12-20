import com.ullink.Msbuild

plugins {
    id("dependencies-plugin")
    id("com.ullink.msbuild") version "3.7"
}

val msbuild: Msbuild by project.tasks
val assemblyVersion by project.extra {
    project.version.let {
        val version = it as String
        if (version.split(".").size == 3) {
            "$version.0"
        } else {
            version
        }
    }
}

tasks {
    val setVersion by registering {
        doFirst {
            listOf("DropIt", "ShellExtension").forEach { name ->
                listOf("AssemblyVersion", "AssemblyFileVersion").forEach { field ->
                    val file = file("$projectDir/$name/Properties/AssemblyInfo.cs")
                    ant.withGroovyBuilder {
                        "replaceregexp"(
                            "file" to file,
                            "match" to Regex("^\\[assembly: $field\\s*\\(\".*\"\\)\\s*\\]$"),
                            "replace" to "[assembly: $field(\"$assemblyVersion\")]",
                            "byline" to true,
                            "encoding" to "UTF-8"
                        )
                    }
                }
            }
        }
    }
}

msbuild.apply {
    solutionFile = "DropItWrapper.sln"
    projectName = "DropIt"
    targets = listOf("Rebuild")
    configuration = "Debug"
    destinationDir = File(buildDir, "msbuild")
    dependsOn("setVersion")
}

task<Zip>("msbuildZip") {
    from(msbuild.destinationDir)
    include("**/*")
    archiveFileName.set("windows-wrapper-${project.version}.zip")
    destinationDirectory.set(file("$buildDir/libs"))
    dependsOn("msbuild")
}

val msbuildZip: Zip by project.tasks

val zipFile by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "zipfile"))
    }
    outgoing.artifact(msbuildZip)
}
