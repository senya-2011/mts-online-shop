plugins {
	java
	kotlin("jvm") version "2.0.21"
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("io.kotest") version "6.1.3"
}

group = "com.mts"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

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
	implementation(project(":api"))
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.mapstruct:mapstruct:1.6.3")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("io.kotest:kotest-runner-junit5:6.1.3")
	testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.3")

    testImplementation("io.mockk:mockk:1.14.9")
}

tasks.withType<Test> {
	useJUnitPlatform()
}