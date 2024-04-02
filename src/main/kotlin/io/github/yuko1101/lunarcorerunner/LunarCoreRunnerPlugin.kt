package io.github.yuko1101.lunarcorerunner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import java.io.File

abstract class LunarCoreRunnerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val implementation = project.configurations.getByName("implementation")
        implementation.isCanBeResolved = true
        
        val lunarcore = project.configurations.create("lunarcore") {
            implementation.extendsFrom(it)
        }
        val gameprovider = project.configurations.create("gameprovider") {
            implementation.extendsFrom(it)
        }
        // does not extend implementation not to add any dependencies to the mod
        val gameproviderLibrary = project.configurations.create("gameproviderLibrary")

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
        gameproviderLibrary.exclude(mapOf("module" to "gson"))
        gameproviderLibrary.exclude(mapOf("module" to "guava"))

        project.tasks.register("runServer", JavaExec::class.java) { task ->
            task.dependsOn("jar")
            task.doFirst { _ ->
                val modFile: File = project.tasks.getByName("jar").outputs.files.singleFile
                val lunarCoreFile: File = lunarcore.files.first()
                val gameProvider: File = gameprovider.singleFile
                val gameProviderLibraries: List<File> = gameproviderLibrary.toList()

                val runDir = project.file("run")
                val librariesDir = File(runDir, "libraries")
                if (!librariesDir.exists()) librariesDir.mkdirs()
                val modsDir = File(runDir, "mods")
                if (!modsDir.exists()) modsDir.mkdir()

                modFile.copyTo(File(modsDir, modFile.name), true)

                val lunarCoreJar = File(runDir, lunarCoreFile.name)
                val gameProviderJar = File(runDir, gameProvider.name)
                lunarCoreFile.copyTo(lunarCoreJar, true)
                gameProvider.copyTo(gameProviderJar, true)

                val libraryJars: List<File> = gameProviderLibraries.map { library ->
                    val to = File(librariesDir, library.name)
                    library.copyTo(to, true)
                    to
                }

                task.workingDir = runDir
                task.classpath = project.files(lunarCoreJar, gameProviderJar, *libraryJars.toTypedArray())
            }

            task.mainClass.set("io.github.yuko1101.provider.Main")
            println(task.commandLine.joinToString(" "))

            // TODO: make not to stop the task while running the server
        }
    }
}