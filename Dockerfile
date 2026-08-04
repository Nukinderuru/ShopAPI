FROM docker.io/library/eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src src

RUN chmod +x gradlew && ./gradlew installDist -x test

FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/install/shop /app/

EXPOSE 8080

ENTRYPOINT ["/app/bin/shop"]
