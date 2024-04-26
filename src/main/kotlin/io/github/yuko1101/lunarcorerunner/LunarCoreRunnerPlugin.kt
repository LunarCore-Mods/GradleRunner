package io.github.yuko1101.lunarcorerunner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File
import java.util.jar.JarInputStream
import java.util.jar.Manifest

abstract class LunarCoreRunnerPlugin : Plugin<Project> {

    companion object {
        lateinit var lunarcore: Configuration
        lateinit var gameprovider: Configuration
        lateinit var gameproviderLibrary: Configuration

    }

    override fun apply(project: Project) {
        val implementation = project.configurations.getByName("implementation")
        implementation.isCanBeResolved = true
        
        lunarcore = project.configurations.create("lunarcore") {
            implementation.extendsFrom(it)
        }
        gameprovider = project.configurations.create("gameprovider") {
            implementation.extendsFrom(it)
        }
        // does not extend implementation not to add any dependencies to the mod
        gameproviderLibrary = project.configurations.create("gameproviderLibrary")

        // TODO: better way to get version
        val defaultMixinVersion = "0.12.5+mixin.0.8.5"
        val mixinVersion = if (project.properties.containsKey("mixin_version")) {
            val value = project.properties["mixin_version"] as String
            if (value.contains("+mixin.")) {
                value
            } else {
                defaultMixinVersion.split("+mixin.")[0] + "+mixin." + value
            }
        } else {
            defaultMixinVersion
        }

        project.repositories.add(project.repositories.maven { it.url = project.uri("https://maven.fabricmc.net") })
        project.dependencies.add("gameproviderLibrary", "net.fabricmc:sponge-mixin:$mixinVersion")
        gameproviderLibrary.exclude(mapOf("module" to "gson"))
        gameproviderLibrary.exclude(mapOf("module" to "guava"))

        project.tasks.register("runServer", RunServerTask::class.java) { task ->
        }
    }
}