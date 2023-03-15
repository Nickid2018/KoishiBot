plugins {
    `java-library`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("io.netty:netty-all:4.1.89.Final")
    api("commons-io:commons-io:2.11.0")
    api("it.unimi.dsi:fastutil:8.5.12")
    api("org.slf4j:slf4j-api:1.7.36")

    api("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}