buildscript {
    configurations.all {
        resolutionStrategy {
            force("commons-io:commons-io:2.18.0")
        }
    }
}
plugins {
    id("java")
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"
version = "1.7.3"



allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "sonatype"
            url = uri( "https://oss.sonatype.org/content/groups/public/")
        }

        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/central") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.extendedclip.com/releases/") }
        maven {
            name = "CodeMC"
            url = uri("https://repo.codemc.io/repository/maven-public/")
        }
        maven("https://repo.negative.games/repository/maven-releases/")
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    //compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    //compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") // you can compile with this

    val version = 19
    var finalVersion: String = "$version"
    if (version == 16) {
        finalVersion += ".5"
    }
    //compileOnly("org.spigotmc:spigot-api:1.$finalVersion-R0.1-SNAPSHOT")


    compileOnlyApi("org.spigotmc:spigot:1.8-R0.1-SNAPSHOT") // can be obtained from buildtools, being used only for investigation purposes
    val adventureVersion = "4.17.0"
    api("net.kyori:adventure-text-minimessage:$adventureVersion")
    api("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    api("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    api("org.joml:joml:1.10.8")
    implementation(project(":api"))


    compileOnlyApi("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnlyApi("me.clip:placeholderapi:2.11.6")
    compileOnlyApi("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnlyApi("com.sk89q:worldguard:6.1")


    api("de.tr7zw:item-nbt-api:2.14.1")


    val configVersion = "4.1.2"
    api("org.spongepowered:configurate-core:$configVersion")
    api("org.spongepowered:configurate-yaml:$configVersion")
    api("io.vavr:vavr:0.10.4")
    api("com.github.florianingerl.util:regex:1.1.11")

    val cloudVersion = "1.8.4"
    //implementation("cloud.commandframework:cloud-paper:$cloudVersion")
    api("cloud.commandframework:cloud-bukkit:$cloudVersion")
    api("cloud.commandframework:cloud-core:$cloudVersion")
    api("cloud.commandframework:cloud-minecraft-extras:$cloudVersion")
    api("org.bstats:bstats-bukkit:3.0.2")
    api("com.github.retrooper:packetevents-spigot:2.7.0")
}

val targetJavaVersion = 8
java {
    withSourcesJar()
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

//tasks.withType(JavaCompile).configureEach {
//    options.encoding = "UTF-8"
//
//    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
//        options.release.set(targetJavaVersion)
//    }
//}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version
    )
    props.forEach { (key, value) ->
        inputs.property(key, value)
    }
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val debugMode: Boolean = (System.getenv("DEBUG_MODE") as String?)?.toBoolean() ?: false


tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    val mainPackage = project.group.toString() + "." + project.name.lowercase()
    relocate("cloud.commandframework", "$mainPackage.commandframework")
    relocate("net.kyori", "$mainPackage.kyori")
    relocate("de.tr7zw.changeme.nbtapi", "$mainPackage.nbtapi")
    relocate("org.spongepowered", "$mainPackage.spongepowered")
    relocate("org.yaml.snakeyaml", "$mainPackage.snakeyaml")
    relocate("org.bstats", "$mainPackage.bstats")
    relocate("org.joml", "$mainPackage.joml")
    relocate("com.github.retrooper.packetevents", "$mainPackage.packetevents")
    exclude("kotlin/**")
    if (!debugMode) {
        minimize()
    }
}


