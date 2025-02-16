package io.github.rothes.esu.velocity.module

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
import kotlin.jvm.java

object UserNameVerifyModule: VelocityModule<UserNameVerifyModule.ModuleConfig, UserNameVerifyModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override fun enable() {
        plugin.server.eventManager.register(plugin, Listener)
    }

    override fun disable() {
        super.disable()
        plugin.server.eventManager.unregisterListener(plugin, Listener)
    }

    object Listener {

        @Subscribe(order = PostOrder.FIRST)
        fun onLogin(e: LoginEvent) {
            val username = e.player.username
            for ((key, regex) in config.requirements) {
                if (regex.matchEntire(username) == null) {
                    val user = e.player.user
                    user.kick(locale, { kickMessage.messages[key] },
                        component("prefix", user.buildMinimessage(locale, { kickMessage.prefix }))
                    )
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