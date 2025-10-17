package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.ServerCommandEvent

object BlockedCommandsModule: BukkitModule<BlockedCommandsModule.ModuleConfig, BlockedCommandsModule.ModuleLocale>() {

    override fun enable() {
        Listeners.register()
    }

    override fun disable() {
        super.disable()
        Listeners.unregister()
    }

    object Listeners: Listener {

        @EventHandler
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            event.isCancelled = blocked(event.player.user, event.message.substring(1))
        }

        @EventHandler
        fun onCommand(event: ServerCommandEvent) {
            event.isCancelled = blocked(ConsoleUser, event.command)
        }

        private fun blocked(user: BukkitUser, command: String): Boolean {
            val matched = config.blockingCommands.find { group ->
                if (group.consoleUserExcluded && user is ConsoleUser) {
                    return@find false
                }

                group.commands.any { cmd ->
                    cmd.containsMatchIn(command)
                }
            }
            if (matched != null) {
                val key = matched.blockedMessage
                user.message(user.localedOrNull(locale) { blockedMessage[key] } ?: key.message)
                return true
            }
            return false
        }
    }


    data class ModuleConfig(
        val blockingCommands: List<BlockingGroup> = arrayListOf(BlockingGroup("no-suicide", listOf("^(.+:)?suicide$".toRegex(), "^(.+:)?kill$".toRegex()))),
    ): BaseModuleConfiguration() {

        data class BlockingGroup(
            @Comment("The message key to send to users. You need to set the message in locale configs.")
            val blockedMessage: String = "",
            @Comment("The commands to block. Using regex.")
            val commands: List<Regex> = arrayListOf(),
            val consoleUserExcluded: Boolean = true,
        ): ConfigurationPart

    }

    data class ModuleLocale(
        val blockedMessage: Map<String, MessageData> = linkedMapOf(
            Pair("no-suicide", "<gold>Do not kill yourself! We still love you...".message)
        ),
    ): ConfigurationPart

}