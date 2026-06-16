/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.scheduler

import io.github.rothes.esu.bukkit.util.ServerInfo.isFolia
import io.github.rothes.esu.bukkit.util.extension.createChild
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.EsuBootstrap
import kotlinx.coroutines.CompletableDeferred
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

object Scheduler {

    private val bukkitScheduler = versioned<AdvancedBukkitScheduler>()

    fun global(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin) { func(FoliaTask(it)) })
        else
            BukkitTask(bukkitScheduler.run(plugin) { func(BukkitTask(it)) })
    }
    fun global(delayTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { func(FoliaTask(it)) }, delayTicks))
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks) { func(BukkitTask(it)) })
    }
    fun global(delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { func(FoliaTask(it)) }, delayTicks, periodTicks))
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks, periodTicks) { func(BukkitTask(it)) })
    }

    fun schedule(entity: Entity, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.run(plugin, { func(FoliaTask(it)) }, null) ?: return null)
        else
            BukkitTask(bukkitScheduler.run(plugin) { func(BukkitTask(it)) })
    }
    fun schedule(entity: Entity, delayTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.runDelayed(plugin, { func(FoliaTask(it)) }, null, delayTicks) ?: return null)
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks) { func(BukkitTask(it)) })
    }
    fun schedule(entity: Entity, delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return if (isFolia)
            FoliaTask(entity.scheduler.runAtFixedRate(plugin, { func(FoliaTask(it)) }, null, delayTicks, periodTicks) ?: return null)
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks, periodTicks) { func(BukkitTask(it)) })
    }

    fun schedule(location: Location, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().run(plugin, location) { func(FoliaTask(it)) })
        else
            BukkitTask(bukkitScheduler.run(plugin) { func(BukkitTask(it)) })
    }
    fun schedule(location: Location, delayTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().runDelayed(plugin, location, { func(FoliaTask(it)) }, delayTicks))
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks) { func(BukkitTask(it)) })
    }
    fun schedule(location: Location, delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, { func(FoliaTask(it)) }, delayTicks, periodTicks))
        else
            BukkitTask(bukkitScheduler.run(plugin, delayTicks, periodTicks) { func(BukkitTask(it)) })
    }

    fun async(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runNow(plugin) { func(FoliaTask(it)) })
        else
            BukkitTask(bukkitScheduler.runAsync(plugin) { func(BukkitTask(it)) })
    }
    fun async(delay: Duration, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func(FoliaTask(it)) },
                delay.inWholeMilliseconds, TimeUnit.MILLISECONDS))
        else
            BukkitTask(bukkitScheduler.runAsync(plugin, delay.inWholeMilliseconds / 50) { func(BukkitTask(it)) })
    }
    fun async(delay: Duration, period: Duration, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func(FoliaTask(it)) },
                delay.inWholeMilliseconds, period.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            )
        else
            BukkitTask(bukkitScheduler.runAsync(plugin, delay.inWholeMilliseconds / 50, period.inWholeMilliseconds / 50) { func(BukkitTask(it)) })
    }
    fun asyncTicks(delayTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func(FoliaTask(it)) },
                delayTicks * 50, TimeUnit.MILLISECONDS))
        else
            BukkitTask(bukkitScheduler.runAsync(plugin, delayTicks) { func(BukkitTask(it)) })
    }
    fun asyncTicks(delayTicks: Long, periodTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask {
        return if (isFolia)
            FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func(FoliaTask(it)) },
                delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS)
            )
        else
            BukkitTask(bukkitScheduler.runAsync(plugin, delayTicks, periodTicks) { func(BukkitTask(it)) })
    }

    fun <T> Entity.onTickDeferred(plugin: Plugin = esuPlugin, func: () -> T): CompletableDeferred<T> {
        val deferred = CompletableDeferred<T>()
        onTick(plugin) {
            deferred.complete(func())
        } ?: error("Failed to schedule task for entity $this")
        return deferred
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

    fun Entity.onTick(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return schedule(this, plugin.alwaysEnabled(), func)
    }

    fun Entity.nextTick(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return schedule(this, 1, plugin.alwaysEnabled(), func)
    }

    fun Entity.syncTick(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        if (checkTickThread()) {
            func(RealTimeTask)
            return null
        }
        return schedule(this, plugin.alwaysEnabled(), func) ?: error("Failed to schedule task for entity $this")
    }

    fun Entity.delayedTick(delayTicks: Long, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return schedule(this, delayTicks, plugin, func)
    }

    fun Entity.fixedTick(periodTicks: Long, delayTicks: Long = periodTicks, plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit): PlatformTask? {
        return schedule(this, delayTicks, periodTicks, plugin, func)
    }

    fun Location.syncTick(plugin: Plugin = esuPlugin, func: (PlatformTask) -> Unit) {
        if (checkTickThread())
            func(RealTimeTask)
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

    private val esuPlugin: Plugin
        get() = EsuBootstrap.instance as Plugin

}