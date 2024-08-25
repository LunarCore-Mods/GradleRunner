plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.plugin-publish") version "1.2.1"
}

val ver = "dev"

group = "io.github.lunarcoremods"
version = System.getenv("TAG_NAME")?.let {
    if (it.startsWith("v")) it.substring(1) else it
} ?: System.getenv("COMMIT_SHA") ?: ver

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

gradlePlugin {
    website = "https://github.com/LunarCoreMods/GradleRunner"
    vcsUrl = "https://github.com/LunarCoreMods/GradleRunner.git"
    plugins {
        create("gradlerunner") {
            id = "io.github.lunarcoremods.gradlerunner"
            displayName = "LunarCore Gradle Runner"
            description = "A gradle plugin which makes it easier to run/debug your LunarCore mods."
            tags = listOf("lunarcore", "mod")
            implementationClass = "io.github.lunarcoremods.gradlerunner.LunarCoreRunnerPlugin"
        }
    }
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
}