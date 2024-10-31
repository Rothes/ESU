package io.github.rothes.esu.bukkit.util.scheduler

import io.github.rothes.esu.bukkit.plugin
import org.bukkit.Bukkit
import java.util.concurrent.TimeUnit

object Scheduler {

    fun async(delay: Long, period: Long, func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { func.invoke() },
            delay * 50, period * 50, TimeUnit.MILLISECONDS))
    }

    fun async(delay: Long, func: () -> Unit): ScheduledTask {
        return FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, { func.invoke() },
            delay * 50, TimeUnit.MILLISECONDS))
    }

}