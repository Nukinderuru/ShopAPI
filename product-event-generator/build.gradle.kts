plugins {
    kotlin("jvm")
    application
}

val exposedVersion = project.property("exposedVersion") as String
val postgresVersion = project.property("postgresVersion") as String
val hikariVersion = project.property("hikariVersion") as String
val coroutinesVersion = project.property("coroutinesVersion") as String
val kotlinxSerializationVersion = project.property("kotlinxSerializationVersion") as String
val logbackVersion = project.property("logbackVersion") as String
val amqpClientVersion = project.property("amqpClientVersion") as String

application {
    mainClass.set("com.nukinderuru.generator.ProductEventGeneratorApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":product-event-contract"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlinxSerializationVersion}")
    implementation("com.rabbitmq:amqp-client:${amqpClientVersion}")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}
