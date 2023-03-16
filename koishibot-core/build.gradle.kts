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