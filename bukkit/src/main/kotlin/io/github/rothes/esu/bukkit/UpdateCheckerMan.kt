package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UpdateChecker
import io.github.rothes.esu.core.util.UpdateChecker.VersionAction
import org.bukkit.Bukkit
import java.util.*

object UpdateCheckerMan {

    private val checker by lazy { UpdateChecker(
        BuildConfig.VERSION_ID.toInt(),
        BuildConfig.VERSION_CHANNEL,
        BuildConfig.PLUGIN_PLATFORM,
        ConsoleUser,
        EnumMap<VersionAction, () -> Unit>(VersionAction::class.java).apply {
            put(VersionAction.PROHIBIT) { Bukkit.getPluginManager().disablePlugin(plugin) }
        },
        { Bukkit.getOnlinePlayers().map { it.user } },
        "esu")
    }
    private var task: ScheduledTask? = null

    init {
        reload()
    }

    fun reload() {
        if (EsuConfig.get().updateChecker) {
            if (task == null) {
                task = Scheduler.async(0, 60 * 60 * 20) {
                    checker.run()
                }
            }
        } else {
            shutdown()
        }
    }

    fun shutdown() {
        task?.cancel()
        task = null
    }

    fun onJoin(user: User) {
        if (EsuConfig.get().updateChecker) {
            checker.onJoin(user)
        }
    }

}