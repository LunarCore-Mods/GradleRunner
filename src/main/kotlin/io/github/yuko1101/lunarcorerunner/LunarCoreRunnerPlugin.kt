package io.github.yuko1101.lunarcorerunner

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File



abstract class LunarCoreRunnerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val implementation = project.configurations.getByName("implementation")
        implementation.isCanBeResolved = true
        
        project.configurations.create("lunarcore") {
            implementation.extendsFrom(it)
        }
        project.configurations.create("gameprovider") {
            implementation.extendsFrom(it)
        }
        // does not extend implementation not to add any dependencies to the mod
        project.configurations.create("gameproviderLibrary")

        // TODO: better way to get version
        val defaultMixinVersion = "0.12.5+mixin.0.8.5"
        val mixinVersion by lazy {
            if (project.properties.containsKey("mixin_version")) {
                val value = project.properties["mixin_version"] as String
                if (value.contains("+mixin.")) {
                    value
                } else {
                    defaultMixinVersion.split("+mixin.")[0] + "+mixin." + value
                }
            } else {
                defaultMixinVersion
            }
        }

        project.repositories.add(project.repositories.maven { it.url = project.uri("https://maven.fabricmc.net") })
        project.dependencies.add("gameproviderLibrary", "net.fabricmc:sponge-mixin:$mixinVersion")

        project.tasks.register("runServer", RunServerTask::class.java) {
            it.dependsOn("jar")
            it.doFirst { _ ->
                val modFile: File = project.tasks.getByName("jar").outputs.files.singleFile
                val lunarCoreFile: File = project.configurations.getByName("lunarcore").files.first()
                val gameProvider: File = project.configurations.getByName("gameprovider").singleFile
                val gameProviderLibraries: List<File> = project.configurations.getByName("gameproviderLibrary").toList()

                val runDir = project.file("run")
                val librariesDir = File(runDir, "libraries")
                if (!librariesDir.exists()) librariesDir.mkdirs()
                val modsDir = File(runDir, "mods")
                if (!modsDir.exists()) modsDir.mkdir()

                modFile.copyTo(File(modsDir, modFile.name), true)

                lunarCoreFile.copyTo(File(runDir, lunarCoreFile.name), true)
                gameProvider.copyTo(File(runDir, gameProvider.name), true)

                gameProviderLibraries.forEach { library ->
                    library.copyTo(File(librariesDir, library.name), true)
                }

                // TODO: run server
            }
        }
    }
}