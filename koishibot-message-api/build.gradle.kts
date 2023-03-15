plugins {
    `java-library`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    api(project(":koishibot-network"))
    api("com.google.code.gson:gson:2.10")

    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}