package io.github.rothes.esu.core

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.CommandManager
import java.nio.file.Path

interface EsuCore {

    val commandManager: CommandManager<out User>

    fun warn(message: String)
    fun err(message: String)
    fun err(message: String, throwable: Throwable)

    fun baseConfigPath(): Path

    companion object {

        var instance: EsuCore by InitOnce()

    }

}