plugins {
    id("io.ktor.plugin")
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

val ktorVersion = project.property("ktorVersion") as String
val koinVersion = project.property("koinVersion") as String
val exposedVersion = project.property("exposedVersion") as String
val liquibaseVersion = project.property("liquibaseVersion") as String
val postgresVersion = project.property("postgresVersion") as String
val hikariVersion = project.property("hikariVersion") as String
val jacksonVersion = project.property("jacksonVersion") as String
val coroutinesVersion = project.property("coroutinesVersion") as String
val kotlinxSerializationVersion = project.property("kotlinxSerializationVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val testcontainersVersion = project.property("testcontainersVersion") as String
val grpcVersion = project.property("grpcVersion") as String

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = true
        onlyCommented = false
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":auth-contract"))

    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-routing-openapi-jvm:$ktorVersion")

    // Koin
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    // Serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("com.h2database:h2:2.2.224")

    // Mocking
    testImplementation("io.mockk:mockk:1.14.6")

    // Assertions
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
}

tasks.test {
    useJUnitPlatform()
}
