package io.github.rothes.esu.bukkit.module

import com.google.common.collect.HashBasedTable
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
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
                val conf = config.commands.find { it.commands.any { it.containsMatchIn(cmd) } }
                if (conf == null) {
                    queue.clear()
                } else {
                    queue.removeIf { now - it > conf.expireInterval }
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

        @EventHandler(priority = EventPriority.LOW)
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            val user = event.player.user
            val command = event.message.substring(1)
            val matched = config.commands.find { group ->
                group.commands.any { cmd ->
                    cmd.containsMatchIn(command)
                }
            }
            if (matched != null) {
                val now = System.currentTimeMillis()
                val queue = hits.get(user, command) ?: ArrayDeque<Long>().also {
                    hits.put(user, command, it)
                }
                queue.removeIf { now - it > matched.expireInterval }
                queue.add(now)

                val count = queue.size
                if (matched.cancelCount >= 0 && count >= matched.cancelCount) {
                    event.isCancelled = true
                }

                if (matched.kickCount   >= 0 && count >= matched.kickCount) {
                    user.kick(locale, { kickMessage[matched.kickMessage] })
                } else if (matched.warnCount >= 0 && count >= matched.cancelCount) {
                    user.message(locale, { warnMessage[matched.warnMessage] })
                }
            }
        }
    }


    data class ModuleConfig(
        @field:Comment("Plugin will increase the count for the command player send if it matches any condition,\n" +
                "and handle the limit with the first limit it hits.")
        val commands: List<CommandGroup> = arrayListOf(
            CommandGroup(listOf("^(.+:)?suicide$".toRegex(), "^(.+:)?kill$".toRegex()), "suicide-spam", "suicide-spam"),
            CommandGroup(listOf(".".toRegex()), "generic-spam", "generic-spam"),
        ),
    ): BaseModuleConfiguration() {
        
        data class CommandGroup(
            val commands: List<Regex> = arrayListOf(),
            @field:Comment("The message key to send to users. You need to set the message in locale configs.")
            val warnMessage: String = "",
            val kickMessage: String = "",
            val cancelCount: Int = 3,
            val warnCount: Int = 3,
            val kickCount: Int = 5,
            val expireInterval: Int = 20 * 1000,
        ): ConfigurationPart
    }


    data class ModuleLocale(
        val warnMessage: Map<String, String> = linkedMapOf(
            Pair("suicide-spam", "<ec>Please do not spam suicide. Continue will lead to a kick."),
            Pair("generic-spam", "<ec>Please do not spam commands. Continue will lead to a kick."),
        ),
        val kickMessage: Map<String, String> = linkedMapOf(
            Pair("suicide-spam", "<ec>Please do not spam suicide."),
            Pair("generic-spam", "<ec>Please do not spam commands."),
        ),
    ): ConfigurationPart

}