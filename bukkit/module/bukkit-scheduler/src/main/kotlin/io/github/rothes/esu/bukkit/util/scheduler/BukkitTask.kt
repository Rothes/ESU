package io.github.rothes.esu.bukkit.util.scheduler

import org.bukkit.scheduler.BukkitTask

class BukkitTask(
    private val bukkitTask: BukkitTask
): ScheduledTask {

    override fun cancel() {
        bukkitTask.cancel()
    }

}