plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.mirai.mamoe.net/snapshots")
}

dependencies {
    api(project(":koishibot-message-api"))
    api("net.mamoe:mirai-core:2.15.0-dev-22")
}