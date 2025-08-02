import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

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
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"


val relocatedLib by configurations.creating
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
    compileOnly(project(":"))
    compileOnly("com.google.guava:guava:32.1.3-jre")
    compileOnly("io.netty:netty-all:4.1.10.Final")
    val adventureVersion = "4.23.0"
    relocatedLib("net.kyori:adventure-nbt:$adventureVersion")

    compileOnly("io.github.valerashimchuck:simpleitemgenerator-api:1.10.0")
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


val targetJavaVersion = 16
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

