FROM docker.io/library/gradle:9.4.0-jdk21 AS builder

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY auth-contract auth-contract
COPY auth-service auth-service
COPY shop-api shop-api

RUN gradle :shop-api:installDist -x test --no-daemon

FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/shop-api/build/install/shop-api /app/

EXPOSE 8080

ENTRYPOINT ["/app/bin/shop-api"]
