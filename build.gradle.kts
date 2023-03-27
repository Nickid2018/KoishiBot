plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
}