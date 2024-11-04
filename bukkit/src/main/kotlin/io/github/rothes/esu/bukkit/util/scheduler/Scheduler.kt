package io.github.rothes.esu.bukkit.util.scheduler

import io.github.rothes.esu.bukkit.plugin
import org.bukkit.Bukkit
import java.util.concurrent.TimeUnit

object Scheduler {

    fun global(delayTicks: Long, func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { func.invoke() }, delayTicks))
    }

    fun global(func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin) { func.invoke() })
    }

    fun async(delayTicks: Long, periodTicks: Long, func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func.invoke() },
            delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS))
    }

    fun async(delayTicks: Long, func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func.invoke() },
            delayTicks * 50, TimeUnit.MILLISECONDS))
    }

}