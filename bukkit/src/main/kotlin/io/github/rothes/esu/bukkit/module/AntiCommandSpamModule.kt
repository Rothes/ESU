package io.github.rothes.esu.bukkit.module

import com.google.common.collect.HashBasedTable
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.pojo.MessageData
import io.github.rothes.esu.core.configuration.pojo.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.spongepowered.configurate.objectmapping.meta.Comment

object AntiCommandSpamModule: BukkitModule<AntiCommandSpamModule.ModuleConfig, AntiCommandSpamModule.ModuleLocale>(
    ModuleConfig::class.java, ModuleLocale::class.java
) {
    
    private var cacheTask: ScheduledTask? = null

    override fun enable() {
        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        cacheTask = Scheduler.async(5 * 60 * 20L, 5 * 60 * 20L) {
            val now = System.currentTimeMillis()
            Listeners.hits.cellSet().toList().forEach { cell ->
                val cmd = cell.columnKey
                val queue = cell.value
                val conf = config.kickCommands.find { it.commands.contains(cmd) }
                if (conf == null) {
                    queue.clear()
                } else {
                    queue.removeIf { now - it > conf.removeInterval }
                }
                if (queue.isEmpty()) {
                    Listeners.hits.remove(cell.rowKey, cell.columnKey)
                }
            }
        }
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
        cacheTask?.cancel()
        cacheTask = null
    }

    object Listeners: Listener {
        
        val hits: HashBasedTable<User, String, ArrayDeque<Long>> = HashBasedTable.create<User, String, ArrayDeque<Long>>()

        @EventHandler
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            val user = event.player.user
            val command = event.message.substring(1).split(' ', limit = 2)[0]
            val pure = command.split(':').last()
            val matched = config.kickCommands.find { group ->
                group.commands.find { cmd ->
                    if (cmd.contains(':'))
                        command == cmd
                    else
                        pure == cmd
                } != null
            }
            if (matched != null) {
                val now = System.currentTimeMillis()
                val queue = hits.get(user, command) ?: ArrayDeque<Long>().also {
                    hits.put(user, command, it)
                }
                queue.removeIf { now - it > matched.removeInterval }
                queue.add(now)
                if (queue.size >= matched.hitCount) {
                    event.isCancelled = true
                    user.kick(locale, { kickMessage[matched.kickMessage] })
                }
            }
        }
    }


    data class ModuleConfig(
        val kickCommands: List<KickGroup> = arrayListOf(KickGroup("suicide-spam", listOf("suicide", "kill"))),
    ): BaseModuleConfiguration() {
        
        data class KickGroup(
            @field:Comment("The message key to send to users. You need to set the message in locale configs.")
            val kickMessage: String = "",
            val commands: List<String> = arrayListOf(),
            val hitCount: Int = 5,
            val removeInterval: Int = 20 * 1000,
            val addBlocked: Boolean = true,
        ): ConfigurationPart
    }


    data class ModuleLocale(
        val kickMessage: Map<String, String> = linkedMapOf(
            Pair("suicide-spam", "<red>Please do not spam suicide.")
        ),
    ): ConfigurationPart

}