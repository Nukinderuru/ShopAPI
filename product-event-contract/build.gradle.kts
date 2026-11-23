plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val kotlinxSerializationVersion = project.property("kotlinxSerializationVersion") as String

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

tasks.test {
    useJUnitPlatform()
}
