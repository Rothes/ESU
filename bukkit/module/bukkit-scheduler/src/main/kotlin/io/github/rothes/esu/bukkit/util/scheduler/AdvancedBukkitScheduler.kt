package io.github.rothes.esu.bukkit.util.scheduler

import org.bukkit.craftbukkit.scheduler.CraftTask
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.function.Consumer

interface AdvancedBukkitScheduler {

    fun run(
        plugin: Plugin,
        delayTicks: Long = 0,
        periodTicks: Long = CraftTask.NO_REPEATING.toLong(),
        task: Consumer<in BukkitTask>
    ): BukkitTask

    fun runAsync(
        plugin: Plugin,
        delayTicks: Long = 0,
        periodTicks: Long = CraftTask.NO_REPEATING.toLong(),
        task: Consumer<in BukkitTask>
    ): BukkitTask

}