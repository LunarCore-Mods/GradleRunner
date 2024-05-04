plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.yuko1101"
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
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("org.ow2.asm:asm-commons:9.4")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}

kotlin {
    jvmToolchain(17)
}