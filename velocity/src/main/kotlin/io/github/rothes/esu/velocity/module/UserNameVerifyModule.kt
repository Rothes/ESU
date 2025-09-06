package io.github.rothes.esu.velocity.module

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.velocity.user
import kotlin.jvm.java

object UserNameVerifyModule: VelocityModule<UserNameVerifyModule.ModuleConfig, UserNameVerifyModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override fun enable() {
        registerListener(Listener)
    }

    object Listener {

        @Subscribe(order = PostOrder.LATE)
        fun onLogin(e: ServerPreConnectEvent) {
            // We need to use this event to make sure the client has sent their settings, for locale.
            val username = e.player.username
            for ((key, regex) in config.requirements) {
                if (regex.matchEntire(username) == null) {
                    val user = e.player.user
                    user.kick(locale, { kickMessage.messages[key] },
                        component("prefix", user.buildMiniMessage(locale, { kickMessage.prefix }))
                    )
                    e.result = ServerPreConnectEvent.ServerResult.denied()
                    break
                }
            }
        }
    }

    data class ModuleLang(
        val kickMessage: KickMessage = KickMessage(),
    ): ConfigurationPart {

        data class KickMessage(
            val prefix: String = "<pc>You have failed username verification: <br><br>",
            val messages: Map<String, String> = linkedMapOf(
                "length" to "<prefix>" +
                        "<pdc>Username length must in 3-16",
                "ascii" to "<prefix>" +
                        "<pdc>Only alphanumeric characters (letters and numbers)<br>" +
                        "and underscores (_) are allowed in username",
            ),
        )
    }

    data class ModuleConfig(
        val requirements: Map<String, Regex> = linkedMapOf(
            "length" to "^.{3,16}$".toRegex(),
            "ascii" to "^[a-zA-Z0-9_]+$".toRegex(),
        ),
    ): BaseModuleConfiguration()

}