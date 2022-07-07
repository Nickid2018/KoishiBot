import java.lang.String.join

plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

dependencies {
    api("net.mamoe:mirai-core:2.9.2")
    api("org.apache.logging.log4j:log4j-api:2.17.2")
    api("org.apache.logging.log4j:log4j-core:2.17.2")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    api("org.apache.httpcomponents:httpclient:4.5.13")
    api("com.google.code.gson:gson:2.9.0")
    api("org.jsoup:jsoup:1.14.3")
    api("org.apache.commons:commons-text:1.9")
    api("commons-io:commons-io:2.11.0")
    api("com.kotcrab.remark:remark:1.2.0")
    api("org.apache.xmlgraphics:batik-transcoder:1.14")
    api("org.apache.xmlgraphics:batik-codec:1.14")
    api("com.google.zxing:core:3.5.0")
    api("org.seleniumhq.selenium:selenium-java:4.2.1")
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
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