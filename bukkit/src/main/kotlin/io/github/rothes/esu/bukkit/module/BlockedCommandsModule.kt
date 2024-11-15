package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.ServerCommandEvent
import org.spongepowered.configurate.objectmapping.meta.Comment

object BlockedCommandsModule: BukkitModule<BlockedCommandsModule.ModuleConfig, BlockedCommandsModule.ModuleLocale>(
    ModuleConfig::class.java, ModuleLocale::class.java
) {

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            event.isCancelled = blocked(event.player.user, event.message.substring(1))
        }

        @EventHandler
        fun onCommand(event: ServerCommandEvent) {
            event.isCancelled = blocked(ConsoleUser, event.command)
            println(event.command)
        }

        private fun blocked(user: BukkitUser, rawCommand: String): Boolean {
            val command = rawCommand.split(' ', limit = 2)[0]
            val pure = command.split(':').last()
            val matched = config.blockingCommands.find { group ->
                if (group.consoleUserExcluded && user is ConsoleUser) {
                    return@find false
                }

                group.commands.find { cmd ->
                    if (cmd.contains(':'))
                        command == cmd
                    else
                        pure == cmd
                } != null
            }
            if (matched != null) {
                user.minimessage(user.localedOrNull(locale) { blockMessage[matched.blockedMessage] } ?: matched.blockedMessage)
                return true
            }
            return false
        }
    }


    data class ModuleConfig(
        val blockingCommands: List<BlockingGroup> = arrayListOf(BlockingGroup("no-suicide", listOf("suicide", "kill"))),
    ): BaseModuleConfiguration() {

        data class BlockingGroup(
            @field:Comment("The message key to send to users. You need to set the message in locale configs.")
            val blockedMessage: String = "",
            val commands: List<String> = arrayListOf(),
            val consoleUserExcluded: Boolean = true,
        ): ConfigurationPart

    }

    data class ModuleLocale(
        val blockMessage: Map<String, String> = linkedMapOf(
            Pair("no-suicide", "<gold>Do not kill yourself! We still love you...")
        ),
    ): ConfigurationPart

}