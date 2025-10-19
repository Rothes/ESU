package io.github.rothes.esu.core

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.CommandManager
import java.nio.file.Path

interface EsuCore : EsuLogger {

    val initialized: Boolean
    val commandManager: CommandManager<out User>

    override fun info(message: String) {
        EsuBootstrap.instance.info(message)
    }
    override fun warn(message: String) {
        EsuBootstrap.instance.warn(message)
    }
    override fun err(message: String) {
        EsuBootstrap.instance.err(message)
    }
    override fun err(message: String, throwable: Throwable?) {
        EsuBootstrap.instance.err(message, throwable)
    }

    fun baseConfigPath(): Path {
        return EsuBootstrap.instance.baseConfigPath()
    }

    companion object {

        var instance: EsuCore by InitOnce()

    }

}