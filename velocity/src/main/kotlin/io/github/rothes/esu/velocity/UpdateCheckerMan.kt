package io.github.rothes.esu.velocity

import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UpdateChecker
import io.github.rothes.esu.core.util.UpdateChecker.VersionAction
import io.github.rothes.esu.data.BuildInfo
import io.github.rothes.esu.velocity.user.ConsoleUser
import java.util.*

object UpdateCheckerMan {

    private val checker =
        UpdateChecker(
            BuildInfo.VERSION_ID.toInt(),
            BuildInfo.VERSION_CHANNEL,
            BuildInfo.PLUGIN_PLATFORM,
            ConsoleUser,
            EnumMap<VersionAction, () -> Unit>(VersionAction::class.java).apply {
                put(VersionAction.PROHIBIT) { /* TODO */ }
            },
            { plugin.server.allPlayers.map { it.user } },
            "vesu"
        )

    fun reload() {
        checker.onReload()
    }

    fun shutdown() {
        checker.shutdown()
    }

    fun onJoin(user: User) {
        checker.onJoin(user)
    }

}