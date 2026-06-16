package io.github.rothes.esu.bukkit.util.scheduler.v1

import io.github.rothes.esu.bukkit.util.scheduler.AdvancedBukkitScheduler
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.scheduler.CraftScheduler
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.function.Consumer

/**
 * BukkitScheduler API won't return BukkitTask if a Consumer task is passed.
 * Through this util, the BukkitTask is returned.
 *
 */
object AdvancedBukkitSchedulerImpl : AdvancedBukkitScheduler {

    override fun run(
        plugin: Plugin, delayTicks: Long, periodTicks: Long, task: Consumer<in BukkitTask>
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler() as CraftScheduler
        return scheduler.runTaskTimer(plugin, task as Any, delayTicks, periodTicks)
    }

    override fun runAsync(
        plugin: Plugin, delayTicks: Long, periodTicks: Long, task: Consumer<in BukkitTask>
    ): BukkitTask {
        val scheduler = Bukkit.getScheduler() as CraftScheduler
        return scheduler.runTaskTimerAsynchronously(plugin, task as Any, delayTicks, periodTicks)
    }

}