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
    api("com.google.code.gson:gson:2.10")
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

tasks.jar.configure {
    manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.core.BotStart"
    manifest.attributes["Class-Path"] = configurations.runtimeClasspath.get()
        .filter { it.name.endsWith(".jar") }
        .joinToString("") { "libraries/" + it.name }
}

tasks.register<Sync>("exportApi") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("apis"))
}

tasks.register("computeSignature") {
    val md = MessageDigest.getInstance("SHA-256")
    layout.buildDirectory.dir("apis").get().files().files.sorted().forEach {
        md.update(it.readBytes())
    }
    val signatureAPIs = md.digest().joinToString("") { "%02x".format(it) }
    val signatureCoreJar = layout.buildDirectory.file("libs/koishibot-core.jar")
        .map { it.asFile }.map { it.readBytes() }
        .map { md.digest(it) }.get().joinToString("") { "%02x".format(it) }
    layout.buildDirectory.file("libs/signature.txt").get().asFile.writeText(
        "$signatureAPIs\n$signatureCoreJar"
    )
}

tasks["exportApi"].dependsOn("jar")
tasks["computeSignature"].dependsOn("exportApi")
tasks["build"].dependsOn("computeSignature")