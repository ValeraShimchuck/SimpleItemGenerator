import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.jar.JarFile

buildscript {
    configurations.all {
        resolutionStrategy {
            force("commons-io:commons-io:2.18.0")
        }
    }
    repositories { mavenCentral() }
    dependencies {
        classpath("org.ow2.asm:asm:9.8")
        classpath("org.ow2.asm:asm-commons:9.8")
    }
}
plugins {
    id("java")
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("idea")
    id("checkstyle")
    kotlin("jvm")
}

checkstyle {
    toolVersion = "10.12.5"
    configFile = file("config/checkstyle/checkstyle.xml")
}

group = "ua.valeriishymchuk"
version = "1.11.0"


val relocatedLib by configurations.creating
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

        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven("https://repo.negative.games/repository/maven-releases/")
    }
}

dependencies {

    registerTransform(RelocationTransform::class) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "transformed-jar")

        parameters {
            prepend.set("${group.toString().replace('.', '/')}/libs")
        }
    }
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


    //compileOnlyApi("org.spigotmc:spigot:1.8-R0.1-SNAPSHOT") // can be obtained from buildtools, being used only for investigation purposes
    compileOnlyApi("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    val adventureVersion = "4.23.0"
    relocatedLib("net.kyori:adventure-nbt:$adventureVersion")
    relocatedLib("net.kyori:adventure-text-minimessage:$adventureVersion")
    relocatedLib("net.kyori:adventure-text-serializer-gson:$adventureVersion"){
        exclude("com.google.code.gson")
        exclude("org.jetbrains")
        exclude("org.jspecify")
    }
    relocatedLib("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    api("org.joml:joml:1.10.8")
    implementation(project(":api"))


    compileOnlyApi("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnlyApi("me.clip:placeholderapi:2.11.6")
    compileOnlyApi("com.arcaniax:HeadDatabase-API:1.3.2")
    compileOnlyApi("com.sk89q.worldguard:worldguard-bukkit:7.0.5") {
        exclude("org.bukkit")
    }
    compileOnlyApi("com.sk89q.worldedit:worldedit-bukkit:7.2.17")


    api("de.tr7zw:item-nbt-api:2.15.2-SNAPSHOT")


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
    api("com.github.retrooper:packetevents-spigot:2.9.3")
    testImplementation("com.tngtech.archunit:archunit:1.4.0")
    testImplementation("junit:junit:4.13.2")
    implementation(kotlin("stdlib-jdk8"))
}

val targetJavaVersion = 16
java {
    withSourcesJar()
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

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
    configurations = listOf(
        project.configurations.runtimeClasspath.get(),
        relocatedLib
    )
    val mainPackage = project.group.toString() + "." + project.name.lowercase()
    relocate("cloud.commandframework", "$mainPackage.commandframework")
    //relocate("net.kyori", "$mainPackage.kyori")
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

configurations.named("relocatedLib") {
    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "transformed-jar")
    }
}

tasks.compileJava {
    dependsOn(relocatedLib)
    classpath = sourceSets.main.get().compileClasspath + files(relocatedLib)
}

object ASMUtils {
    class PackageRemapper(private val toRelocate: String, private val prepend: String): Remapper() {
        override fun mapPackageName(name: String): String {
            if (!name.startsWith(toRelocate)) return name
            return "$prepend/$name"
        }

        override fun map(internalName: String): String {
            if (!internalName.startsWith(toRelocate)) return internalName
            return "$prepend/$internalName"
        }

        override fun mapModuleName(name: String): String {
            if (!name.startsWith(toRelocate)) return name
            return "$prepend/$name"
        }

    }
}

abstract class RelocationTransform : TransformAction<RelocationTransform.Parameters> {


    interface Parameters : TransformParameters {
        @get:Input
        val prepend: Property<String>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    private fun getArtifactGroup(file: File): String {
        //return getSharedGroup(getAllClasses(file))
        return getAllClasses(file).first()
            .split("/")
            .take(2)
            .joinToString("/")
    }

    private fun getAllClasses(input: File): List<String> {
        return JarFile(input).use {
            val list: MutableList<String> = arrayListOf()
            for (entry in it.entries()) {
                val name = entry.name
                if (name.startsWith("META-INF")) continue
                if (name.endsWith("module-info.class")) continue
                if (name.endsWith(".class")) list.add(name)
            }
            list
        }
    }

    private fun getSharedGroup(list: List<String>): String {
        if (list.isEmpty()) throw IllegalArgumentException("List is empty")
        var packages = list.map { it.substringBeforeLast('/') }.distinct()
        var output: String? = null
        while (output == null) {
            val copyPackages = listOf(*packages.toTypedArray())
            output = packages.firstOrNull { candidate ->
                copyPackages.all { it.startsWith(candidate) }
            }
            if (output == null) {
                packages = packages.map { it.substringBeforeLast('/') }.distinct()
            }
        }
        if (output == "") throw IllegalArgumentException(
            "Couldn't find any group that would satisfy all packages.\n" +
                    "Maybe there are more groups then expected? Candidates: $packages")
        return output
    }

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputFile = outputs.file(inputFile.name.replace(".jar", "-transformed.jar"))
        try {
            processJar(inputFile, outputFile)
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
        //inputFile.copyTo(outputFile, overwrite = true)
    }

    private fun processJar(input: File, outputFile: File) {
        val artifact = getArtifactGroup(input).replace('.', '/')
        JarOutputStream(outputFile.outputStream()).use { jos ->
            JarFile(input).use { jar ->
                val entries = jar.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue

                    val entryName = entry.name
                    if (entryName.endsWith("module-info.class")) continue
                    if (entryName.contains("META-INF")) continue
                    val shouldRelocate =  entryName.endsWith(".class") &&
                            !entryName.endsWith("module-info.class")
                    val newEntryName: String = if (shouldRelocate) "${parameters.prepend.get()}/$entryName"
                    else entryName
                    val newEntry = ZipEntry(newEntryName)
                    jos.putNextEntry(newEntry)

                    jar.getInputStream(entry).use { inputStream ->
                        if (entryName.endsWith(".class")) {
                            val bytes = inputStream.readBytes()
                            val transformed = transformClass(bytes, artifact)
                            jos.write(transformed)
                        } else {
                            inputStream.copyTo(jos)
                        }
                    }
                    jos.closeEntry()
                }
            }
        }
    }

    private fun transformClass(bytes: ByteArray, artifact: String): ByteArray {
        val cr = ClassReader(bytes)
        val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
        val remapper = ASMUtils.PackageRemapper(artifact, parameters.prepend.get())
        val cv = ClassRemapper(cw, remapper)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

}


