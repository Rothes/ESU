package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.user.PlayerUser
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

object ComponentBukkitUtils {

    fun player(player: Player, key: String = "player"): TagResolver.Single {
        return Placeholder.component(key, player.displayName())
    }

    fun user(user: PlayerUser, key: String = "player"): TagResolver.Single {
        return player(user.player, key)
    }

}