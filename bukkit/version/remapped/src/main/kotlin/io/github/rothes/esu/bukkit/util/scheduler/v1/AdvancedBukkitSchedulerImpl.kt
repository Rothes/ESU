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