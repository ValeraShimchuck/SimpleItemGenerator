import org.jreleaser.model.Active
import org.jreleaser.model.Http
import org.jreleaser.model.Signing

plugins {
    id("java")
    `java-library`
    id("maven-publish")
    id("org.jreleaser") version "1.15.0"
}

group = "ua.valeriishymchuk"

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnlyApi("org.jetbrains:annotations:26.0.2")
}


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
                    username = (findProperty("mavencentral.name") ?: return@create )as String
                    password = (findProperty("mavencentral.password") ?: return@create) as String
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

