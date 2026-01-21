import org.gradle.jvm.tasks.Jar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
}

version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files(rootProject.layout.projectDirectory.file("HytaleServer.jar")))
    implementation(kotlin("stdlib"))
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
}

// Replace version placeholder in manifest.json with gradle version
tasks.named<ProcessResources>("processResources") {
    filesMatching("manifest.json") {
        expand("version" to project.version)
    }
}

val modsDir = rootProject.layout.projectDirectory.dir("mods")

// Disable publishing the standard jar to mods to avoid duplicates
tasks.named<Jar>("jar") {
    archiveBaseName.set("ExamplePlugin")
    // Keep default destination (build/libs) and avoid placing in mods
}

tasks.named<ShadowJar>("shadowJar") {
    destinationDirectory.set(modsDir)
    archiveBaseName.set("ExamplePlugin")
    archiveVersion.set("")  // Remove version from filename
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn("shadowJar")
}
