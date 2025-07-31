package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayNameV
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

object ComponentBukkitUtils {

    fun player(player: Player, key: String = "player"): TagResolver.Single {
        return Placeholder.component(key, player.displayNameV)
    }

    fun user(user: User, key: String = "player"): TagResolver.Single {
        return when (user) {
            is PlayerUser -> player(user.player, key)
            is GenericUser -> component(key, user.commandSender.name())
            else -> unparsed(key, user.name)
        }
    }

}