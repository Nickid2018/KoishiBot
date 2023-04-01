import java.security.MessageDigest

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
    api("net.mamoe:mirai-core:2.15.0-dev-52")

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.backend.Main"
        manifest.attributes["Class-Path"] = configurations.runtimeClasspath.get()
            .map { it.name }
            .filter { it.endsWith(".jar") }
            .sorted()
            .joinToString(" ") { "libraries/$it" }
    }

    register<Sync>("exportApi") {
        from(configurations.runtimeClasspath)
        into(layout.buildDirectory.dir("apis"))
    }

    register("computeChecksum") {
        doLast {
            val md = MessageDigest.getInstance("SHA-256")

            layout.buildDirectory.dir("apis").get().asFileTree.files
                .filter { it.isFile }
                .filter { !it.name.contains("koishibot") }
                .sortedBy { it.name }
                .forEach { md.update(it.readBytes()) }
            parent!!.layout.projectDirectory.dir("koishibot-message-api/src/main/java").asFileTree.files
                .filter { it.isFile }
                .sortedBy { it.name }
                .forEach { md.update(it.readBytes()) }
            parent!!.layout.projectDirectory.dir("koishibot-network/src/main/java").asFileTree.files
                .filter { it.isFile }
                .sortedBy { it.name }
                .forEach { md.update(it.readBytes()) }
            val checksumAPI = md.digest().joinToString("") { "%02x".format(it) }
            layout.projectDirectory.dir("src/main/java").asFileTree.files
                .filter { it.isFile }
                .sortedBy { it.name }
                .forEach { md.update(it.readBytes()) }
            val checksumJar = md.digest().joinToString("") { "%02x".format(it) }

            parent!!.layout.buildDirectory.file("libs/qq-backend-checksum.txt").get().asFile.writeText(
                "$checksumJar\n$checksumAPI"
            )
        }
    }
}

tasks["exportApi"].dependsOn("jar")
tasks["computeChecksum"].dependsOn("exportApi")
tasks["build"].dependsOn("computeChecksum")