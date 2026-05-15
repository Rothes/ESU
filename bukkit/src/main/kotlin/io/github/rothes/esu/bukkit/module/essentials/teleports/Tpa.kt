package io.github.rothes.esu.bukkit.module.essentials.teleports

import io.github.rothes.esu.bukkit.module.essentials.Teleports
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import java.util.*

object Tpa : CommonFeature<FeatureToggle.DefaultTrue, Unit>() {

    // target -> requester
    private val pendingRequests = mutableMapOf<UUID, UUID>()

    override fun onEnable() {
        registerCommands(object {

            @Command("tpa <target>")
            @ShortPerm
            fun tpa(sender: User, target: Player) {
                if (sender !is PlayerUser) return
                if (sender.player == target) {
                    sender.message(Teleports.lang, { tpaCannotRequestSelf })
                    return
                }

                if (pendingRequests[target.uniqueId] == sender.uuid) {
                    sender.message(Teleports.lang, { tpaRequestAlreadySent })
                    return
                }

                pendingRequests[target.uniqueId] = sender.uuid
                sender.message(Teleports.lang, { tpaRequestSent }, player(target))
                target.user.message(Teleports.lang, { tpaRequestReceived }, player(sender.player))
            }

            @Command("tpaccept [requester]")
            @ShortPerm
            fun tpaccept(sender: User, requester: Player?) {
                if (sender !is PlayerUser) return

                val requesterId = requester?.uniqueId ?: pendingRequests[sender.uuid]
                if (requesterId == null || (requester != null && pendingRequests[sender.uuid] != requesterId)) {
                    sender.message(Teleports.lang, { tpaNoPendingRequest })
                    return
                }

                val requesterPlayer = org.bukkit.Bukkit.getPlayer(requesterId)
                if (requesterPlayer == null) {
                    sender.message(Teleports.lang, { tpaPlayerNotOnline })
                    pendingRequests.remove(sender.uuid)
                    return
                }

                pendingRequests.remove(sender.uuid)
                requesterPlayer.tp(sender.player.location)
                sender.message(Teleports.lang, { tpaRequestAccepted })
                requesterPlayer.user.message(Teleports.lang, { tpaRequestAccepted })
            }

            @Command("tpdeny [requester]")
            @ShortPerm
            fun tpdeny(sender: User, requester: Player?) {
                if (sender !is PlayerUser) return

                val requesterId = requester?.uniqueId ?: pendingRequests[sender.uuid]
                if (requesterId == null || (requester != null && pendingRequests[sender.uuid] != requesterId)) {
                    sender.message(Teleports.lang, { tpaNoPendingRequest })
                    return
                }

                pendingRequests.remove(sender.uuid)
                sender.message(Teleports.lang, { tpaRequestDenied })
                org.bukkit.Bukkit.getPlayer(requesterId)?.user?.message(Teleports.lang, { tpaRequestDenied })
            }

            @Command("tpcancel [target]")
            @ShortPerm
            fun tpcancel(sender: User, target: Player?) {
                if (sender !is PlayerUser) return

                val targetId = target?.uniqueId ?: pendingRequests.entries.find { it.value == sender.uuid }?.key
                if (targetId == null || (target != null && pendingRequests[targetId] != sender.uuid)) {
                    sender.message(Teleports.lang, { tpaNoPendingRequest })
                    return
                }

                pendingRequests.remove(targetId)
                sender.message(Teleports.lang, { tpaRequestCancelled })
                org.bukkit.Bukkit.getPlayer(targetId)?.user?.message(Teleports.lang, { tpaRequestCancelled })
            }
        })
    }

}
