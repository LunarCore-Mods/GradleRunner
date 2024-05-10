package io.github.lunarcoremods.gradlerunner

import net.fabricmc.accesswidener.AccessWidener
import net.fabricmc.accesswidener.AccessWidenerClassVisitor
import net.fabricmc.accesswidener.AccessWidenerReader
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

abstract class AccessWidenerTransformer : TransformAction<AccessWidenerTransformer.Parameters> {

    interface Parameters : TransformParameters {
        @get:Input
        val wideners: ListProperty<String>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val widener = AccessWidener()
        val reader = AccessWidenerReader(widener)
        parameters.wideners.get().forEach {
            reader.read(it.toByteArray())
        }

        val targets = widener.targets.map { it.replace('.', '/') + ".class" }.toSet()

        JarFile(inputArtifact.get().asFile).use { inputJar ->
            if (targets.isEmpty() || targets.all { inputJar.getEntry(it) == null }) {
                outputs.file(inputArtifact)
                return
            }
        }

        println("Transforming ${inputArtifact.get().asFile.name} with access widener")

        val inputFile = inputArtifact.get().asFile
        val outputFile = outputs.file("widened-${inputFile.name}")

        JarInputStream(inputFile.inputStream()).use { input ->
            JarOutputStream(outputFile.outputStream()).use { output ->
                val manifest = input.manifest
                if (manifest != null) {
                    val manifestEntry = ZipEntry("META-INF/MANIFEST.MF")
                    output.putNextEntry(manifestEntry)
                    manifest.write(output)
                    output.closeEntry()
                }

                var jarEntry = input.nextJarEntry
                while (jarEntry != null) {
                    output.putNextEntry(jarEntry)
                    if (targets.contains(jarEntry.name)) {
                        output.write(transformClass(widener, input.readAllBytes()))
                    } else {
                        output.write(input.readAllBytes())
                    }
                    output.closeEntry()
                    jarEntry = input.nextJarEntry
                }
            }
        }
    }

    private fun transformClass(widener: AccessWidener, bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(0)
        val visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, widener)
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }
}