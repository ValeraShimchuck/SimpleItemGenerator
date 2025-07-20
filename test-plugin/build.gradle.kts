plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"


dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    compileOnly(project(":"))
    compileOnly("com.google.guava:guava:32.1.3-jre")
    compileOnly("io.netty:netty-all:4.1.10.Final")
    compileOnly("io.github.valerashimchuck:simpleitemgenerator-api:1.5.0")
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    val mainPackage = rootProject.group.toString() + "." + rootProject.name.lowercase()
    relocate("cloud.commandframework", "$mainPackage.commandframework")
    relocate("net.kyori", "$mainPackage.kyori")
    relocate("de.tr7zw.changeme.nbtapi", "$mainPackage.nbtapi")
    relocate("org.spongepowered", "$mainPackage.spongepowered")
    relocate("org.yaml.snakeyaml", "$mainPackage.snakeyaml")
    relocate("org.bstats", "$mainPackage.bstats")
    relocate("org.joml", "$mainPackage.joml")
    relocate("com.github.retrooper.packetevents", "$mainPackage.packetevents")
}


val targetJavaVersion = 17
java {
    withSourcesJar()
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}


tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to rootProject.version
    )
    props.forEach { (key, value) ->
        inputs.property(key, value)
    }
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}


