package io.github.rothes.esu.bukkit.util.scheduler

class FoliaTask(
    private val foliaTask: io.papermc.paper.threadedregions.scheduler.ScheduledTask
): ScheduledTask {

    override fun cancel() {
        foliaTask.cancel()
    }

}