package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.kotlin.extension.commandBuilder

abstract class PlayerOptionalCommand<C, L>: BaseCommand<C, L>() {

    protected open val receivesSilentFlag: Boolean
        get() = true

    override fun onEnable() {
        withCommandManager {
            commandBuilder(name) {
                copy {
                    permission(cmdShortPerm())
                    senderType(PlayerUser::class.java)
                    handler { ctx ->
                        val sender = ctx.sender() as PlayerUser
                        onPerform(sender, sender.player, true)
                    }
                    regCmd()
                }
                permission(cmdShortPerm("other"))
                required("player", PlayerParser.playerParser())
                if (receivesSilentFlag) flag("silent")
                handler { ctx ->
                    val sender = ctx.sender()
                    val player = ctx.get<Player>("player")
                    val silent = ctx.flags().isPresent("silent")
                    onPerform(sender, player, silent || sender is PlayerUser && sender.player == player)
                }
                regCmd()
            }
        }
    }

    abstract fun onPerform(sender: User, player: Player, silent: Boolean)

}