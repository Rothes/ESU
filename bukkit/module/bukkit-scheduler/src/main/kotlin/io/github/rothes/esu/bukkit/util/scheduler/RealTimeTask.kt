package io.github.rothes.esu.bukkit.util.scheduler

object RealTimeTask : PlatformTask {

    override val taskId: Int
        get() = -1

    override fun cancel() {
        // Do nothing
    }

}