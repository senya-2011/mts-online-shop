plugins {
    java
    id("org.openapi.generator") version "6.6.0"
}

group = "com.mts"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.5"))
    
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.openapitools:jackson-databind-nullable:0.2.6")
}

val generatedOpenApiDir = layout.projectDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(layout.projectDirectory.file("openapi.yml").asFile.absolutePath)
    outputDir.set(generatedOpenApiDir.asFile.absolutePath)
    apiPackage.set("com.mts.online_shop.api")
    modelPackage.set("com.mts.online_shop.model")
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

tasks.named("compileJava") {
    dependsOn(tasks.openApiGenerate)
}
