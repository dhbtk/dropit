rootProject.name = "dropit"

pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
    }
}

includeBuild("build-config")

include(":common")
include(":desktop")
include(":mobile")
if (System.getProperty("os.name").startsWith("Win")) {
    include(":windows-wrapper")
}