plugins {
    kotlin("jvm")
    application
}

val exposedVersion = project.property("exposedVersion") as String
val postgresVersion = project.property("postgresVersion") as String
val hikariVersion = project.property("hikariVersion") as String
val coroutinesVersion = project.property("coroutinesVersion") as String
val ktorVersion = project.property("ktorVersion") as String
val logbackVersion = project.property("logbackVersion") as String

application {
    mainClass.set("com.nukinderuru.generator.ProductEventGeneratorApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.ktor:ktor-client-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio-jvm:${ktorVersion}")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}
