package io.github.yuko1101.lunarcorerunner

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin

abstract class RunServerTask : DefaultTask() {
    init {
        description = "Runs LunarCore server with the mod"

        group = BasePlugin.BUILD_GROUP
    }
}