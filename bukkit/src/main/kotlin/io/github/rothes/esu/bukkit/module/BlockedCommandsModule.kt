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
        }

        private fun blocked(user: BukkitUser, command: String): Boolean {
            val matched = config.blockingCommands.find { group ->
                if (group.consoleUserExcluded && user is ConsoleUser) {
                    return@find false
                }

                group.commands.find { cmd ->
                    command.matches(cmd)
                } != null
            }
            if (matched != null) {
                val key = matched.blockedMessage
                user.minimessage(user.localedOrNull(locale) { blockedMessage[key] } ?: key)
                return true
            }
            return false
        }
    }


    data class ModuleConfig(
        val blockingCommands: List<BlockingGroup> = arrayListOf(BlockingGroup("no-suicide", listOf("^(.+:)?suicide$".toRegex(), "^(.+:)?kill$".toRegex()))),
    ): BaseModuleConfiguration() {

        data class BlockingGroup(
            @field:Comment("The message key to send to users. You need to set the message in locale configs.")
            val blockedMessage: String = "",
            @field:Comment("The commands to block. Using regex.")
            val commands: List<Regex> = arrayListOf(),
            val consoleUserExcluded: Boolean = true,
        ): ConfigurationPart

    }

    data class ModuleLocale(
        val blockedMessage: Map<String, String> = linkedMapOf(
            Pair("no-suicide", "<gold>Do not kill yourself! We still love you...")
        ),
    ): ConfigurationPart

}