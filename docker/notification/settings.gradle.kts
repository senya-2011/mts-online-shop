// Минимальный settings для Docker-сборки notification-service (без backend/bank).
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
    }
}

rootProject.name = "mts-online-shop"

include(":messaging-contracts")
include(":notification-service")
