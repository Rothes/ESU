package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object PlayersWatchModule: BukkitModule<PlayersWatchModule.ConfigData, PlayersWatchModule.ModuleLocale>() {

    val watching: ConcurrentHashMap<PlayerUser, WatchOptions> = ConcurrentHashMap()
    val players: LinkedHashSet<Player> = LinkedHashSet()

    override fun onEnable() {
        Listeners.register()

        Bukkit.getOnlinePlayers().forEach {
            it.spectatorTarget
        }
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
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
        @Comment("In ticks.")
        val switchWatchInterval: Long = 6 * 20
    ): BaseModuleConfiguration()

    data class ModuleLocale(
        val a: String = ""
    ): ConfigurationPart

}