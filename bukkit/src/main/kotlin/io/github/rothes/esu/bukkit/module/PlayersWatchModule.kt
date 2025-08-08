package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Comment
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayersWatchModule: BukkitModule<PlayersWatchModule.ConfigData, PlayersWatchModule.ModuleLocale>(
    ConfigData::class.java, ModuleLocale::class.java
) {

    val watching: ConcurrentHashMap<PlayerUser, WatchOptions> = ConcurrentHashMap()
    val players: LinkedHashSet<Player> = LinkedHashSet()

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)

        Bukkit.getOnlinePlayers().forEach {
            it.spectatorTarget
        }
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
    }

    object Listeners: Listener {

        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            players.add(event.player)
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            players.remove(event.player)
        }
    }

    data class WatchOptions(
        var loop: Boolean = true,
        var onIndex: Int = (0 .. watching.size).random(),
    )

    data class ConfigData(
        @field:Comment("In ticks.")
        val switchWatchInterval: Long = 6 * 20
    ): BaseModuleConfiguration()

    data class ModuleLocale(
        val a: String = ""
    ): ConfigurationPart

}