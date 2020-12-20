package dropitconf

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType

class DependenciesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.version = gitVersion(target.rootProject.projectDir)
        target.extra["commitCount"] = gitCommitCount(target.rootProject.projectDir)
        target.configurations.all {
            resolutionStrategy {
                force("org.jetbrains.kotlin:kotlin-stdlib-common:${Deps.Plugins.KOTLIN}")
                force("org.jetbrains.kotlin:kotlin-stdlib:${Deps.Plugins.KOTLIN}")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Deps.Plugins.KOTLIN}")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.Plugins.KOTLIN}")
                force("org.jetbrains.kotlin:kotlin-reflect:${Deps.Plugins.KOTLIN}")

                dependencySubstitution {
                    substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                        .with(module("org.eclipse.platform:org.eclipse.swt.${BuildPlatform.current.swtRuntime}:${Deps.SWT_VERSION}"))
                }
            }
        }

        target.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.apiVersion = "1.4"
            kotlinOptions.jvmTarget = "1.8"
        }

        target.tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
        }
    }
}