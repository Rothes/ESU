package io.github.rothes.esu.core

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.CommandManager
import java.nio.file.Path

interface EsuCore {

    val dependenciesResolved: Boolean
    val initialized: Boolean
    val commandManager: CommandManager<out User>

    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)
    fun err(message: String, throwable: Throwable?)

    fun baseConfigPath(): Path

    companion object {

        var instance: EsuCore by InitOnce()

    }

}