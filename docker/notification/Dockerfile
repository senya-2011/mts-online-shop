# Сборка только notification-service + messaging-contracts (без backend/bank).
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /w
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.workers.max=2"

COPY gradle gradle
COPY gradlew gradlew.bat build.gradle.kts ./
COPY docker/notification/settings.gradle.kts settings.gradle.kts
COPY messaging-contracts messaging-contracts
COPY notification-service notification-service

RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    chmod +x gradlew && ./gradlew :notification-service:bootJar --no-daemon \
    -Dkotlin.compiler.execution.strategy=in-process \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:+UseSerialGC -Dfile.encoding=UTF-8" \
    -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /w/notification-service/build/libs/app.jar /app/app.jar
EXPOSE 10081
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]
