plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

val ver = "1.0.0"

group = "io.github.lunarcoremods"
version = System.getenv("COMMIT_SHA") ?: ver

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

tasks.jar.configure {
    enabled = false
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(shade)
    archiveVersion.set("")
    archiveClassifier.set("")
}

kotlin {
    jvmToolchain(17)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY")}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            artifactId = project.name
            from(components["java"])
        }
    }
}