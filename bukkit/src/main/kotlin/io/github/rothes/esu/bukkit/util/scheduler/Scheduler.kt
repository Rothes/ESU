package io.github.rothes.esu.bukkit.util.scheduler

import io.github.rothes.esu.bukkit.util.ServerCompatibility.isFolia
import io.github.rothes.esu.bukkit.util.extension.createChild
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import kotlinx.coroutines.CompletableDeferred
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import io.github.rothes.esu.bukkit.bootstrap as esuPlugin

object Scheduler {

    fun global(plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin) { func.invoke() })
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }
    fun global(delayTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { func.invoke() }, delayTicks))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLater(plugin, func, delayTicks))
    }
    fun global(delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { func.invoke() }, delayTicks, periodTicks))
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, func, delayTicks, periodTicks))
    }

    fun schedule(entity: Entity, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.run(plugin, { func.invoke() }, null) ?: return null)
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }
    fun schedule(entity: Entity, delayTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.runDelayed(plugin, { func.invoke() }, null, delayTicks) ?: return null)
        else
            BukkitTask(Bukkit.getScheduler().runTaskLater(plugin, func, delayTicks))
    }
    fun schedule(entity: Entity, delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.runAtFixedRate(plugin, { func.invoke() }, null, delayTicks, periodTicks) ?: return null)
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, func, delayTicks, periodTicks))
    }

    fun schedule(location: Location, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().run(plugin, location) { func.invoke() })
        else
            BukkitTask(Bukkit.getScheduler().runTask(plugin, func))
    }
    fun schedule(location: Location, delayTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().runDelayed(plugin, location, { func.invoke() }, delayTicks))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLater(plugin, func, delayTicks))
    }
    fun schedule(location: Location, delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, { func.invoke() }, delayTicks, periodTicks))
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, func, delayTicks, periodTicks))
    }

    fun async(plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runNow(plugin) { func.invoke() })
        else
            BukkitTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, func))
    }
    fun async(delay: Duration, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func.invoke() },
                delay.inWholeMilliseconds, TimeUnit.MILLISECONDS))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, func,
                delay.inWholeMilliseconds / 50))
    }
    fun async(delay: Duration, period: Duration, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func.invoke() },
                delay.inWholeMilliseconds, period.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            )
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, func,
                delay.inWholeMilliseconds / 50, period.inWholeMilliseconds / 50))
    }
    fun asyncTicks(delayTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func.invoke() },
                delayTicks * 50, TimeUnit.MILLISECONDS))
        else
            BukkitTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, func, delayTicks))
    }
    fun asyncTicks(delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func.invoke() },
                delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS)
            )
        else
            BukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, func, delayTicks, periodTicks))
    }

    fun <T> Entity.nextTickDeferred(plugin: Plugin = esuPlugin, func: () -> T): CompletableDeferred<T> {
        val deferred = CompletableDeferred<T>()
        nextTick(plugin) {
            deferred.complete(func())
        } ?: error("Failed to schedule task for entity $this")
        return deferred
    }

    fun <T> Entity.syncTickDeferred(plugin: Plugin = esuPlugin, func: () -> T): CompletableDeferred<T> {
        val deferred = CompletableDeferred<T>()
        syncTick(plugin) {
            deferred.complete(func())
        }
        return deferred
    }

    fun Entity.nextTick(plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return schedule(this, plugin.alwaysEnabled(), func)
    }

    fun Entity.syncTick(plugin: Plugin = esuPlugin, func: () -> Unit) {
        if (checkTickThread())
            func()
        else
            schedule(this, plugin.alwaysEnabled(), func) ?: error("Failed to schedule task for entity $this")
    }

    fun Entity.delayedTick(delayTicks: Long, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return schedule(this, delayTicks, plugin, func)
    }

    fun Entity.fixedTick(periodTicks: Long, delayTicks: Long = periodTicks, plugin: Plugin = esuPlugin, func: () -> Unit): ScheduledTask? {
        return schedule(this, delayTicks, periodTicks, plugin, func)
    }

    fun Location.syncTick(plugin: Plugin = esuPlugin, func: () -> Unit) {
        if (checkTickThread())
            func()
        else
            schedule(this, plugin.alwaysEnabled(), func)
    }

    fun cancelTasks(plugin: Plugin = esuPlugin) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
            Bukkit.getAsyncScheduler().cancelTasks(plugin)
        } else {
            Bukkit.getScheduler().cancelTasks(plugin)
        }
    }

    /**
     * Create a wrapped plugin instance that bypasses "Plugin attempted to register task while disabled" check
     */
    private fun Plugin.alwaysEnabled(): Plugin = createChild(name = "$name (force-enabled)", forceEnabled = true)

}