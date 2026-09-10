plugins {
    kotlin("jvm")
    application
}

val exposedVersion = project.property("exposedVersion") as String
val liquibaseVersion = project.property("liquibaseVersion") as String
val postgresVersion = project.property("postgresVersion") as String
val hikariVersion = project.property("hikariVersion") as String
val grpcVersion = project.property("grpcVersion") as String
val jwtVersion = project.property("jwtVersion") as String
val koinVersion = project.property("koinVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val testcontainersVersion = project.property("testcontainersVersion") as String

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.nukinderuru.auth.AuthApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":auth-contract"))
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks.test {
    useJUnitPlatform()
}
