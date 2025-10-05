plugins {
    id("dev.kikugie.stonecutter") version "0.7.9"
    id("dev.architectury.loom") version "1.10-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
    id("dev.kikugie.j52j") version "2.0"
}
stonecutter active "1.20.1-fabric" /* [SC] DO NOT EDIT */

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.nucleoid.xyz/")
    maven("https://maven.parchmentmc.org")
    maven("https://github.com/Progames723/maven/raw/main/maven/")
}