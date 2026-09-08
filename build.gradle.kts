plugins {
    id("io.ktor.plugin") version "3.4.3" apply false
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

group = "com.nukinderuru"
version = "1.0.0"

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}
