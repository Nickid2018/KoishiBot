import java.lang.String.join

plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

dependencies {
    // QQ Backend
    api("net.mamoe:mirai-core:2.13.2")

    // KOOK Backend
    api("com.github.KookyBot:KookyBot:0.2.4")

    // Telegram Backend
    api("org.telegram:telegrambots:6.1.0")

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")

    api("commons-io:commons-io:2.11.0")
    api("org.apache.commons:commons-text:1.10.0")
    api("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    api("com.google.code.gson:gson:2.10")
    api("org.jsoup:jsoup:1.15.3")
    api("org.apache.xmlgraphics:batik-transcoder:1.15")
    api("org.apache.xmlgraphics:batik-codec:1.15")
    api("com.google.zxing:core:3.5.1")
    api("org.seleniumhq.selenium:selenium-java:4.7.1")
    api("nl.vv32.rcon:rcon:1.2.0")
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
}

tasks.jar.configure {
    manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.core.BotStart"
    manifest.attributes["Class-Path"] = join(" ", configurations.runtimeClasspath.get()
        .filter{ it.name.endsWith(".jar") }.map { "libraries/" + it.name })
}

tasks.register<Sync>("exportApi") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("apis"))
}

tasks.register<Delete>("cleanTransfer") {
    delete("lib/transfer-${version}.jar")
}

tasks.register<Jar>("jarTransfer") {
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

tasks["jarTransfer"].dependsOn(tasks["cleanTransfer"], tasks["build"])
tasks["build"].dependsOn(tasks["exportApi"])