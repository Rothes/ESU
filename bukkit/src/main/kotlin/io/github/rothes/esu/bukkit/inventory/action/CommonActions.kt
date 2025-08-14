package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.user.User
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit

object CommonActions {

    val CHAT = ArgumentAction.create("Chat") { user, arg ->
        arg ?: return@create
        (user as PlayerUser).player.chat(arg.parsed(user))
    }
    val CLOSE = SimpleAction.create("Close") { user ->
        (user as? PlayerUser)?.player?.closeInventory()
    }
    val COMMAND = ArgumentAction.create("Command") { user, arg ->
        arg ?: return@create
        Bukkit.dispatchCommand((user as BukkitUser).commandSender, arg.parsed(user))
    }
    val CONSOLE = ArgumentAction.create("Console") { user, arg ->
        arg ?: return@create
        Bukkit.dispatchCommand(ConsoleUser.commandSender, arg.parsed(user))
    }
    val MESSAGE = ArgumentAction.create("Message") { user, arg ->
        arg ?: return@create
        user.message(arg.parsed(user).message)
    }

    private fun String.parsed(user: User): String {
        return if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this
        } else if (user is PlayerUser) {
            PlaceholderAPI.setPlaceholders(user.player, this)
        } else {
            PlaceholderAPI.setPlaceholders(null, this)
        }
    }

}