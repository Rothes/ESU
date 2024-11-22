package io.github.rothes.esu.bukkit.util.scheduler

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility.folia
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.concurrent.TimeUnit

object Scheduler {

    fun global(delayTicks: Long, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { func.invoke() }, delayTicks))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLater(plugin, func, delayTicks))
    }

    fun global(func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin) { func.invoke() })
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }

    fun schedule(entity: Entity, delayTicks: Long, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(entity.scheduler.runDelayed(plugin, { func.invoke() }, null, delayTicks)!!)
        else
            BukkitTask(Bukkit.getScheduler().runTaskLater(plugin, func, delayTicks))
    }

    fun schedule(entity: Entity, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(entity.scheduler.run(plugin, { func.invoke() }, null)!!)
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }

    fun schedule(location: Location, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(Bukkit.getRegionScheduler().run(plugin, location) { func.invoke() })
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }

    fun async(delayTicks: Long, periodTicks: Long, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func.invoke() },
                delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS)
            )
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, func, delayTicks, periodTicks))
    }

    fun async(delayTicks: Long, func: () -> Unit): ScheduledTask {
        return if (folia)
            FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func.invoke() },
                delayTicks * 50, TimeUnit.MILLISECONDS))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, func, delayTicks))
    }

}