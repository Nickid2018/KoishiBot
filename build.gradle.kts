plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.2"
}

dependencies {
    api("net.mamoe:mirai-console-terminal:2.9.2")
    api("net.mamoe:mirai-console:2.9.2")
    api("net.mamoe:mirai-core:2.9.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.kotcrab.remark:remark:1.2.0")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    implementation("org.apache.xmlgraphics:batik-codec:1.14")
    implementation("com.google.zxing:core:3.5.0")
}

group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}
