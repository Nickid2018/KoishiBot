import java.security.MessageDigest

plugins {
    `java-library`
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public")
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
}

dependencies {
    api(project(":koishibot-message-api"))

    api("org.apache.commons:commons-text:1.10.0")
    api("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    api("org.jsoup:jsoup:1.15.3")
    api("org.apache.xmlgraphics:batik-transcoder:1.15")
    api("org.apache.xmlgraphics:batik-codec:1.15")
    api("com.google.zxing:core:3.5.1")
    api("org.seleniumhq.selenium:selenium-java:4.7.1")
    api("nl.vv32.rcon:rcon:1.2.0")
    api("io.github.nickid2018:smcl:1.0.4")

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.core.BotStart"
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

            parent!!.layout.buildDirectory.file("libs/core-checksum.txt").get().asFile.writeText(
                "$checksumJar\n$checksumAPI"
            )
        }
    }

    register<Jar>("jarTransfer") {
        archiveBaseName.set("transfer")
        from("build/classes/java/main/") {
            include("io/github/nickid2018/koishibot/module/mc/trans/*.class")
            include("io/github/nickid2018/koishibot/util/tcp/*.class")
        }
        from(configurations.runtimeClasspath.get()
            .filter { it.name.contains("rcon", true) }
            .map { zipTree(it) })
        manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.module.mc.trans.TransMain"
    }
}

tasks["exportApi"].dependsOn("jar")
tasks["computeChecksum"].dependsOn("exportApi")
tasks["jarTransfer"].dependsOn("computeChecksum")
tasks["build"].dependsOn("jarTransfer")