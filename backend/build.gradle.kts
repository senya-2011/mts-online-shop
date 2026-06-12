plugins {
	java
	war
	kotlin("jvm") version "2.0.21"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.7"
	id("io.kotest") version "6.1.3"
}

group = "com.mts"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

kotlin {
	jvmToolchain(17)
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

val camundaVersion = "7.22.0"

dependencies {
	implementation(project(":api"))
	implementation(project(":messaging-contracts"))
	implementation(project(":bank-jca-adapter"))
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
	implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter:$camundaVersion")
	implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:$camundaVersion")
	// REST: core + ручная регистрация сервлета (starter-rest ломает WAR на WildFly)
	implementation("org.camunda.bpm:camunda-engine-rest-core-jakarta:$camundaVersion")
	implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-jersey")
	implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    
    // Narayana JTA
    implementation("org.jboss.narayana.jta:narayana-jta:7.0.0.Final")
    implementation("org.jboss.narayana.jta:jta:7.0.0.Final")
    
    // JWT для REST API (/api/auth/login, JwtAuthenticationFilter)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

	compileOnly("org.projectlombok:lombok:1.18.30")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.liquibase:liquibase-core")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
	testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.0")

    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

tasks.jar {
	enabled = false
}

tasks.bootJar {
	enabled = false
}

tasks.named<ProcessResources>("processResources") {
	// Camunda formRefBinding=deployment: BPMN и .form в одном каталоге processes/
	from("src/main/resources/forms") {
		into("processes")
		include("**/*.form")
	}
}

tasks.bootWar {
	archiveFileName.set("online-shop.war")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.main.allow-bean-definition-overriding", "true")
}