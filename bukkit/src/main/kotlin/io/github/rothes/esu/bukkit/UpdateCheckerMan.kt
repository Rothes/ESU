package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UpdateChecker
import io.github.rothes.esu.core.util.UpdateChecker.VersionAction
import org.bukkit.Bukkit
import java.util.*

object UpdateCheckerMan {

    private val checker =
        UpdateChecker(
            BuildConfig.VERSION_ID.toInt(),
            BuildConfig.VERSION_CHANNEL,
            BuildConfig.PLUGIN_PLATFORM,
            ConsoleUser,
            EnumMap<VersionAction, () -> Unit>(VersionAction::class.java).apply {
                put(VersionAction.PROHIBIT) { Bukkit.getPluginManager().disablePlugin(plugin) }
            },
            { Bukkit.getOnlinePlayers().map { it.user } },
            EsuCore.instance.basePermissionNode
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