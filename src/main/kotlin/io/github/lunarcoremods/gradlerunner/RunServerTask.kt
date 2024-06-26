package io.github.lunarcoremods.gradlerunner

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class RunServerTask : JavaExec() {

    @OutputDirectory
    @Optional
    val runDir: Property<File> = this.objectFactory.property(File::class.java)

    @Input
    @Optional
    val putModJar: Property<Boolean> = this.objectFactory.property(Boolean::class.java)

    init {
        this.dependsOn("jar")
        mainClass.set("io.github.lunarcoremods.provider.Main")
    }

    private fun getCachedGameProviderJar(): File {
        return LunarCoreRunnerPlugin.gameprovider.singleFile
    }

    @Override
    @TaskAction
    override fun exec() {
        val lunarCoreFile: File = LunarCoreRunnerPlugin.lunarcore.files.first()

        val gameProviderLibraries: List<File> = LunarCoreRunnerPlugin.gameproviderLibrary.toList()

        val runDir = this.runDir.getOrElse(project.file(System.getenv("LUNARCORE_DIR") ?: "run"))
        val librariesDir = File(runDir, "libraries")
        if (!librariesDir.exists()) librariesDir.mkdirs()
        val modsDir = File(runDir, "mods")
        if (!modsDir.exists()) modsDir.mkdir()

        var modFile = project.tasks.getByName("jar").outputs.files.singleFile
        if (putModJar.getOrElse(true)) modFile = modFile.copyTo(File(modsDir, modFile.name), true)

        val lunarCoreJar = File(runDir, lunarCoreFile.name)
        lunarCoreFile.copyTo(lunarCoreJar, true)
        val cachedGameProviderJar = getCachedGameProviderJar()
        val gameProviderJar = File(runDir, cachedGameProviderJar.name)
        cachedGameProviderJar.copyTo(gameProviderJar, true)

        val libraryJars: List<File> = gameProviderLibraries.map { library ->
            val to = File(librariesDir, library.name)
            library.copyTo(to, true)
            to
        }

        this.workingDir = runDir
        val serverClassPath = project.files(lunarCoreJar, gameProviderJar, *libraryJars.toTypedArray())
        this.classpath = serverClassPath.plus(project.files(modFile))

        val mixinJar = libraryJars.first { it.name.contains("mixin") }
        // loads mods from classpath, and debug mixin with hot-swapping
        this.jvmArgs("-Dfabric.development=true", "-javaagent:${mixinJar.absolutePath}")

        val cp = serverClassPath.joinToString(";") { runDir.toPath().relativize(it.toPath()).toString() }
        File(runDir, "args.txt").writeText("-cp $cp\n${mainClass.get()}")

        this.standardInput = System.`in`
        this.isIgnoreExitValue = true

        super.exec()
    }
}