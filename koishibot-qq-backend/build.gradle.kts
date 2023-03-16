import java.lang.String.join

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

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
}

tasks.jar.configure {
    manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.backend.Main"
    manifest.attributes["Class-Path"] = join(" ", configurations.runtimeClasspath.get()
        .filter{ it.name.endsWith(".jar") }.map { "libraries/" + it.name })
}

tasks.register<Sync>("exportApi") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("apis"))
}

tasks["build"].dependsOn(tasks["exportApi"])