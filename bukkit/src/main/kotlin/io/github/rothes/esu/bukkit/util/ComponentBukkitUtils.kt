package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayName_
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import me.clip.placeholderapi.PlaceholderAPIPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object ComponentBukkitUtils {

    private val PAPI_TAG_NAMES = setOf("placeholderapi", "papi")
    private val HAS_PLACEHOLDER_API = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

    fun player(player: Player, key: String = "player"): TagResolver.Single {
        return Placeholder.component(key, player.displayName_)
    }

    fun user(user: User, key: String = "player"): TagResolver.Single {
        return when (user) {
            is PlayerUser -> player(user.player, key)
            is GenericUser -> component(key, user.commandSender.name().esu)
            else -> unparsed(key, user.name)
        }
    }

    fun papi(user: User): TagResolver {
        val player = if (user is PlayerUser) user.player else null
        return TagResolver.resolver(PAPI_TAG_NAMES) { arg, context ->
            val papi = arg.popOr("One argument expected for papi tag").value()
            if (HAS_PLACEHOLDER_API) {
                val split = papi.split('_', limit = 2)
                val expansion = PlaceholderAPIPlugin.getInstance().localExpansionManager.getExpansion(split[0].lowercase())
                    ?: return@resolver Tag.inserting(Component.text(papi))
                val result = expansion.onRequest(player, split.getOrElse(1) { "" })

                val type = if (arg.hasNext()) arg.pop().lowerValue() else "plain"
                when (type) {
                    "plain" -> Tag.inserting(Component.text(result ?: papi))
                    "minimessage" -> Tag.inserting(context.deserialize(result ?: papi))
                    else -> error("Unknown text type $type")
                }

            } else {
                Tag.inserting(Component.text(papi))
            }
        }
    }

}