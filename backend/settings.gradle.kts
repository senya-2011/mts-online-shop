pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "online-shop"
include(":api")
project(":api").projectDir = file("../api")
