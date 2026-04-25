FROM eclipse-temurin:21-jdk AS builder
WORKDIR /w
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY api api
COPY backend backend
COPY bank bank
COPY messaging-contracts messaging-contracts
COPY bank-jca-adapter bank-jca-adapter
COPY notification-service notification-service
RUN chmod +x gradlew && ./gradlew :notification-service:bootJar --no-daemon \
    -Dkotlin.compiler.execution.strategy=in-process \
    -Dorg.gradle.jvmargs="-Xmx1024m -Dfile.encoding=UTF-8" \
    -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /w/notification-service/build/libs/app.jar /app/app.jar
EXPOSE 10081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
