pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "online-shop"
include(":api")
project(":api").projectDir = file("../api")
include(":messaging-contracts")
project(":messaging-contracts").projectDir = file("../messaging-contracts")
include(":bank-jca-adapter")
project(":bank-jca-adapter").projectDir = file("../bank-jca-adapter")
