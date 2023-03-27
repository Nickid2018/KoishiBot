plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

//tasks.register<Delete>("cleanTransfer") {
//    delete("lib/transfer-${version}.jar")
//}
//
//tasks.register<Jar>("jarTransfer") {
//    archiveBaseName.set("transfer")
//    from("build/classes/java/main/") {
//        include("io/github/nickid2018/koishibot/module/mc/trans/*.class")
//        include("io/github/nickid2018/koishibot/util/tcp/*.class")
//    }
//    from(configurations.runtimeClasspath.get()
//        .filter { it.name.contains("rcon", true) }
//        .map { zipTree(it) })
//    manifest.attributes["Main-Class"] = "io.github.nickid2018.koishibot.module.mc.trans.TransMain"
//}
//
//tasks["jarTransfer"].dependsOn(tasks["cleanTransfer"], tasks["build"])
//tasks["build"].dependsOn(tasks["exportApi"])