plugins {
    java
    id("org.openapi.generator") version "7.19.0"
}

group = "com.mts"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web:3.5.9")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("org.openapitools:jackson-databind-nullable:0.2.6")
}

val generatedOpenApiDir = layout.projectDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(layout.projectDirectory.file("openapi.yml").asFile.absolutePath)
    outputDir.set(generatedOpenApiDir.asFile.absolutePath)
    apiPackage.set("com.mts.online_shop.api")
    modelPackage.set("com.mts.online_shop.model")
    cleanupOutput.set(true)
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useSpringBoot3" to "true",
            "sourceFolder" to ""
        )
    )
    globalProperties.set(
        mapOf(
            "modelDocs" to "false",
            "apiDocs" to "false"
        )
    )
}

tasks.register("generateApi") {
    dependsOn(tasks.openApiGenerate)
    group = "openapi"
    description = "Generate API classes from openapi.yml"
}

sourceSets {
    main {
        java {
            srcDir(generatedOpenApiDir)
        }
    }
}
