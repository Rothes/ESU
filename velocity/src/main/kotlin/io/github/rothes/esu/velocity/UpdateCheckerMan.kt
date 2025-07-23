package io.github.rothes.esu.velocity

import com.velocitypowered.api.scheduler.ScheduledTask
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UpdateChecker
import io.github.rothes.esu.core.util.UpdateChecker.VersionAction
import io.github.rothes.esu.velocity.user.ConsoleUser
import java.time.Duration
import java.util.EnumMap

object UpdateCheckerMan {

    private val checker by lazy { UpdateChecker(
        BuildConfig.VERSION_ID.toInt(),
        BuildConfig.VERSION_CHANNEL,
        BuildConfig.PLUGIN_PLATFORM,
        ConsoleUser,
        EnumMap<VersionAction, () -> Unit>(VersionAction::class.java).apply {
            put(VersionAction.PROHIBIT) { /* TODO */ }
        },
        { plugin.server.allPlayers.map { it.user } },
        "vesu")
    }
    private var task: ScheduledTask? = null

    init {
        reload()
    }

    fun reload() {
        if (EsuConfig.get().updateChecker) {
            if (task == null) {
                task = plugin.server.scheduler.buildTask(plugin, Runnable { checker.run() })
                    .clearDelay()
                    .repeat(Duration.ofHours(1))
                    .schedule()
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