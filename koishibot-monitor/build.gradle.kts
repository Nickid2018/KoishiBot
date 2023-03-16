plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":koishibot-network"))

    api("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    // Logging
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.monitor.MonitorStart"
    }

    shadowJar {
        archiveBaseName.set("koishibot-monitor")
        manifest.inheritFrom(jar.get().manifest)
    }
}