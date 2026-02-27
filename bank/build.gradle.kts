plugins {
	java
	kotlin("jvm") version "2.0.21"
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.19.0"
	id("io.kotest") version "6.1.3"
}

group = "com.bank-simulator"
version = "0.0.1-SNAPSHOT"
description = "bank-simulator for mts-online-shop lab"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
	implementation("org.mapstruct:mapstruct:1.6.3")
	implementation("org.openapitools:jackson-databind-nullable:0.2.6")
	implementation("jakarta.validation:jakarta.validation-api:3.1.0")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("io.kotest:kotest-runner-junit5:6.1.3")
	testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.3")
	testImplementation("io.mockk:mockk:1.14.9")
}

val generatedOpenApiDir = layout.projectDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(layout.projectDirectory.file("openapi.yml").asFile.absolutePath)
    outputDir.set(generatedOpenApiDir.asFile.absolutePath)
    apiPackage.set("com.bank_simulator.demo.api")
    modelPackage.set("com.bank_simulator.demo.model")
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
tasks.named("compileKotlin") {
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
