plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"


dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    compileOnly(project(":"))
    //compileOnly("io.netty:netty-all:4.1.116.Final")


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


