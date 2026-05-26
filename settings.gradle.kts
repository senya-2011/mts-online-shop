pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.0.21"
        kotlin("plugin.spring") version "2.0.21"
        id("org.springframework.boot") version "3.3.5"
        id("io.spring.dependency-management") version "1.1.7"
        id("org.openapi.generator") version "6.6.0"
        id("io.kotest") version "6.1.3"
    }
}

rootProject.name = "mts-online-shop"

include(":api")
include(":backend")
include(":bank")
include(":messaging-contracts")
include(":bank-jca-adapter")
include(":notification-service")
