import org.jreleaser.model.Active
import org.jreleaser.model.Http
import org.jreleaser.model.Signing

plugins {
    id("java")
    `java-library`
    //id("application")
    id("maven-publish")
    id("org.jreleaser") version "1.15.0"
    //id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") // you can compile with this
    compileOnlyApi("org.jetbrains:annotations:26.0.2")
    //compileOnly(project(":"))
    //compileOnly("io.netty:netty-all:4.1.116.Final")
}

//configurations.all {
//    resolutionStrategy {
//        force("commons-io:commons-io:2.15.0") // Force the correct version
//    }
//}

//tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
//    val mainPackage = rootProject.group.toString() + "." + rootProject.name.lowercase()
//    relocate("cloud.commandframework", "$mainPackage.commandframework")
//    relocate("net.kyori", "$mainPackage.kyori")
//    relocate("de.tr7zw.changeme.nbtapi", "$mainPackage.nbtapi")
//    relocate("org.spongepowered", "$mainPackage.spongepowered")
//    relocate("org.yaml.snakeyaml", "$mainPackage.snakeyaml")
//    relocate("org.bstats", "$mainPackage.bstats")
//    relocate("org.joml", "$mainPackage.joml")
//    relocate("com.github.retrooper.packetevents", "$mainPackage.packetevents")
//    //minimize()
//}

publishing {
    publications {
        create("maven", MavenPublication::class) {
            groupId = "io.github.valerashimchuck"
            artifactId = "simpleitemgenerator-api"
            from(components["java"])

            pom {
                name = "SimpleItemGenerator API"
                description = "API from SimpleItemGenerator minecraft bukkit plugin"
                url = "https://github.com/ValeraShimchuck/SimpleItemGenerator"
                inceptionYear = "2025"
                version = rootProject.version.toString()
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://github.com/ValeraShimchuck/SimpleItemGenerator/blob/master/LICENSE"
                    }
                }
                developers {
                    developer {
                        id = "vshymchuk"
                        name = "Valerii Shymchuk"
                    }
                }
                scm {
                    connection = "scm:git:github.com/ValeraShimchuck/SimpleItemGenerator.git"
                    developerConnection = "scm:git:ssh://github.com/ValeraShimchuck/SimpleItemGenerator.git"
                    url = "https://github.com/ValeraShimchuck/SimpleItemGenerator"
                }
            }

        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }

}

val targetJavaVersion = 8
java {
    withSourcesJar()
    withJavadocJar()
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.register("createOutputDirectory") {
    doLast {
        val outputDir = file("build/jreleaser")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}

tasks.jar {
    archiveBaseName.set("simpleitemgenerator-api")
    archiveClassifier.set("")
}

tasks.named<Jar>("sourcesJar") {
    archiveBaseName.set("simpleitemgenerator-api")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.named<Jar>("javadocJar") {
    archiveBaseName.set("simpleitemgenerator-api")
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
}



tasks.named("jreleaserFullRelease") {
    dependsOn("createOutputDirectory")
}

jreleaser {
    gitRootSearch = true
    project {
        description = "API from SimpleItemGenerator minecraft bukkit plugin"
        copyright = "Valerii Shymchuk"
        version.set(rootProject.version.toString())
        authors= listOf("Valerii Shymchuk")
        website = "https://github.com/ValeraShimchuck/SimpleItemGenerator"
        docsUrl = "https://github.com/ValeraShimchuck/SimpleItemGenerator/wiki"
        license = "MIT"
    }
    deploy {
        signing {
            active = Active.ALWAYS
            armored = true
            publicKey = findProperty("gpg.public")?.toString() ?: ""
            secretKey = findProperty("gpg.secret")?.toString() ?: ""
            passphrase = findProperty("gpg.pass")?.toString() ?: ""
            mode = Signing.Mode.FILE
        }

        maven {
            mavenCentral {
                create("simpleitemgenerator-api") {
                    active.set(Active.ALWAYS)
                    url = "https://central.sonatype.com/api/v1/publisher"
                    username = findProperty("mavencentral.name")!! as String
                    password = findProperty("mavencentral.password")!! as String
                    applyMavenCentralRules = true
                    artifactOverride {
                        groupId = "io.github.valerashimchuck"
                        artifactId = "simpleitemgenerator-api"
                        jar = true
                        sourceJar = true
                        javadocJar = true
                    }
                    stagingRepository("build/staging-deploy")

                }
            }
        }
    }
}

//tasks.withType(JavaCompile).configureEach {
//    options.encoding = "UTF-8"
//
//    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
//        options.release.set(targetJavaVersion)
//    }
//}


//tasks.named<ProcessResources>("processResources") {
//    val props = mapOf(
//        "version" to rootProject.version
//    )
//    props.forEach { (key, value) ->
//        inputs.property(key, value)
//    }
//    filteringCharset = "UTF-8"
//    filesMatching("plugin.yml") {
//        expand(props)
//    }
//}


