plugins {
    `java-library`
}

group = "com.mts"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("jakarta.resource:jakarta.resource-api:2.1.0")
    implementation("javax.transaction:javax.transaction-api:1.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}
