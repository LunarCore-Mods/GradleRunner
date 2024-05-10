plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.lunarcoremods"
version = "1.0-SNAPSHOT"

val shade: Configuration = configurations.register("shade") {
    configurations.implementation.get().extendsFrom(this)
}.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    shade(group = "net.fabricmc", name = "access-widener", version = "2.1.0")
    shade(group = "org.ow2.asm", name = "asm-commons", version = "9.4")
    shade(group = "com.google.code.gson", name = "gson", version = "2.10.1")

    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(shade)
}

kotlin {
    jvmToolchain(17)
}