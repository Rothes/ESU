package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.serializer.OptionalSerializer
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.util.*
import kotlin.jvm.java

object BetterEventMessagesModule: BukkitModule<BetterEventMessagesModule.ConfigData, EmptyConfiguration>(
    ConfigData::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onDeath(event: PlayerDeathEvent) {
            val message = event.deathMessage() ?: return
            event.deathMessage(handle(message, config.message.death))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onJoin(event: PlayerJoinEvent) {
            val message = event.joinMessage() ?: return
            event.joinMessage(handle(message, config.message.playerJoin))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerQuitEvent) {
            val message = event.quitMessage() ?: return
            event.quitMessage(handle(message, config.message.playerQuit))
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onQuit(event: PlayerAdvancementDoneEvent) {
            val message = event.message() ?: return
            event.message(handle(message, config.message.doneAdvancement))
        }

        private fun handle(message: Component, options: ConfigData.Message.MessageOptions): Component? {
            var ret = message
            val color = options.color
            if (color.isPresent) {
                ret = message.color(color.get())
            }
            if (!options.showInConsole) {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage(ret)
                }
                return null
            }
            return ret
        }

    }

    data class ConfigData(
        @field:Comment("""
Customize the message behaviours.
Set 'color' to '${OptionalSerializer.DISABLED}' or a color to change the default color of the message.
If 'show-in-console' is false, only the online players can see the message.""")
        val message: Message = Message(),
    ): BaseModuleConfiguration() {

        data class Message(
            val death: MessageOptions = MessageOptions(Optional.ofNullable(TextColor.fromHexString("#dd9090"))),
            val doneAdvancement: MessageOptions = MessageOptions(Optional.ofNullable(TextColor.fromHexString("#edde0e"))),
            val playerJoin: MessageOptions = MessageOptions(Optional.ofNullable(NamedTextColor.GRAY)),
            val playerQuit: MessageOptions = MessageOptions(Optional.ofNullable(NamedTextColor.GRAY)),
        ): ConfigurationPart {

            data class MessageOptions(
                val color: Optional<TextColor> = Optional.ofNullable(null),
                val showInConsole: Boolean = true
            ): ConfigurationPart
        }

    }

}