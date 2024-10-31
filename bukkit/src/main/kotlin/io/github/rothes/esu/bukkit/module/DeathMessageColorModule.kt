package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

object DeathMessageColorModule: CommonModule<DeathMessageColorModule.ConfigData, EmptyConfiguration>(
    ConfigData::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
    }

    override fun disable() {
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        fun onDeath(event: PlayerDeathEvent) {
            val deathMessage = event.deathMessage() ?: return
            event.deathMessage(deathMessage.color(config.deathMessageColor))
        }

    }

    data class ConfigData(
        val deathMessageColor: TextColor = TextColor.fromHexString("#dd9090")!!,
    ): BaseModuleConfiguration()

}