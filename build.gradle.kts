import java.lang.String.join

plugins {
    val kotlinVersion = "1.7.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

dependencies {
    // QQ Backend
    api("net.mamoe:mirai-core:2.9.2")

    // KOOK Backend
    api("com.github.KookyBot:KookyBot:0.1.2")

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.17.2")
    api("org.apache.logging.log4j:log4j-core:2.17.2")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")


    api("commons-io:commons-io:2.11.0")
    api("org.apache.commons:commons-text:1.9")
    api("org.apache.httpcomponents:httpclient:4.5.13")
    api("com.google.code.gson:gson:2.9.0")
    api("org.jsoup:jsoup:1.14.3")
    api("com.kotcrab.remark:remark:1.2.0")
    api("org.apache.xmlgraphics:batik-transcoder:1.14")
    api("org.apache.xmlgraphics:batik-codec:1.14")
    api("com.google.zxing:core:3.5.0")
    api("org.seleniumhq.selenium:selenium-java:4.2.1")
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