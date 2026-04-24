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
RUN chmod +x gradlew && ./gradlew :bank:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /w/bank/build/libs/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
