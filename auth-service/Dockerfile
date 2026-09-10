FROM docker.io/library/gradle:9.4.0-jdk21 AS builder

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY auth-contract auth-contract
COPY auth-service auth-service
COPY shop-api shop-api

RUN gradle :auth-service:installDist -x test --no-daemon

FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/auth-service/build/install/auth-service /app/

EXPOSE 9090

ENTRYPOINT ["/app/bin/auth-service"]
