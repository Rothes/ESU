package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.nextTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.nextTickDeferred
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command

object Spectate: BaseCommand<FeatureToggle.DefaultTrue, Spectate.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("spectate <target>")
            @ShortPerm
            suspend fun spectate(sender: PlayerUser, @Argument("target") target: Player) {
                val caller = sender.player
                if (caller == target) {
                    return sender.message(lang, { cannotSpectateSelf })
                }
                if (caller.gameMode != GameMode.SPECTATOR) {
                    caller.nextTickDeferred {
                        caller.gameMode = GameMode.SPECTATOR
                    }.join()
                }
                target.nextTick {
                    if (caller.checkTickThread())
                        caller.spectatorTarget = target
                    else {
                        caller.tp(target.location) {
                            caller.spectatorTarget = target
                        }
                    }
                    sender.message(lang, { spectatingTarget }, player(target, "target"))
                }
            }
        }) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

    data class Lang(
        val cannotSpectateSelf: MessageData = "<ec>You cannot spectate yourself!".message,
        val spectatingTarget: MessageData = "<pc>Spectating <pdc><target></pdc> now.".message,
    )

}