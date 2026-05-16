package io.github.rothes.esu.bukkit.module.essentials.teleports

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import java.util.*
import java.util.concurrent.TimeUnit

object Tpa : CommonFeature<FeatureToggle.DefaultTrue, Tpa.Lang>() {

    data class Lang(
        val requestSent: MessageData = "<pc>Teleport request sent to <pdc><player></pdc>.".message,
        val requestReceived: MessageData = "<pc><pdc><player></pdc> has requested to teleport to you.\n<pc>Type <click:run_command:'/tpaccept'><tc>/tpaccept</tc></click> to accept or <click:run_command:'/tpdeny'><tc>/tpdeny</tc></click> to deny.".message,
        val noPendingRequest: MessageData = "<ec>You have no pending teleport requests.".message,
        val requestAccepted: MessageData = "<pc>Teleport request accepted.".message,
        val requestDenied: MessageData = "<pc>Teleport request denied.".message,
        val requestCancelled: MessageData = "<pc>Teleport request cancelled.".message,
        val playerNotOnline: MessageData = "<ec>That player is not online.".message,
        val cannotRequestSelf: MessageData = "<ec>You cannot teleport to yourself.".message,
        val requestAlreadySent: MessageData = "<ec>You have already sent a teleport request to this player.".message,
    ) : ConfigurationPart

    // target -> requester
    private val pendingRequests: Cache<UUID, UUID> = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build()

    override fun onEnable() {
        registerCommands(object {

            @Command("tpa <target>")
            @ShortPerm
            fun tpa(sender: User, target: Player) {
                if (sender !is PlayerUser) return
                if (sender.player == target) {
                    sender.message(lang, { cannotRequestSelf })
                    return
                }

                if (pendingRequests.getIfPresent(target.uniqueId) == sender.uuid) {
                    sender.message(lang, { requestAlreadySent })
                    return
                }

                pendingRequests.put(target.uniqueId, sender.uuid)
                sender.message(lang, { requestSent }, player(target))
                target.user.message(lang, { requestReceived }, player(sender.player))
            }

            @Command("tpaccept [requester]")
            @ShortPerm
            fun tpaccept(sender: User, requester: Player? = (sender as? PlayerUser)?.uuid?.let { pendingRequests.getIfPresent(it) }?.let(Bukkit::getPlayer)) {
                if (sender !is PlayerUser) return

                val expectedId = pendingRequests.getIfPresent(sender.uuid)
                if (expectedId == null || (requester != null && expectedId != requester.uniqueId)) {
                    sender.message(lang, { noPendingRequest })
                    return
                }

                if (requester == null) {
                    sender.message(lang, { playerNotOnline })
                    pendingRequests.invalidate(sender.uuid)
                    return
                }

                pendingRequests.invalidate(sender.uuid)
                requester.syncTick {
                    requester.tp(sender.player.location)
                }
                sender.message(lang, { requestAccepted })
                requester.user.message(lang, { requestAccepted })
            }

            @Command("tpdeny [requester]")
            @ShortPerm
            fun tpdeny(sender: User, requester: Player? = (sender as? PlayerUser)?.uuid?.let { pendingRequests.getIfPresent(it) }?.let(Bukkit::getPlayer)) {
                if (sender !is PlayerUser) return

                val expectedId = pendingRequests.getIfPresent(sender.uuid)
                if (expectedId == null || (requester != null && expectedId != requester.uniqueId)) {
                    sender.message(lang, { noPendingRequest })
                    return
                }

                pendingRequests.invalidate(sender.uuid)
                sender.message(lang, { requestDenied })
                requester?.user?.message(lang, { requestDenied })
            }

            @Command("tpcancel [target]")
            @ShortPerm
            fun tpcancel(sender: User, target: Player? = (sender as? PlayerUser)?.uuid?.let { senderId -> pendingRequests.asMap().entries.find { it.value == senderId }?.key }?.let(Bukkit::getPlayer)) {
                if (sender !is PlayerUser) return

                val expectedTargetId = pendingRequests.asMap().entries.find { it.value == sender.uuid }?.key
                if (expectedTargetId == null || (target != null && expectedTargetId != target.uniqueId)) {
                    sender.message(lang, { noPendingRequest })
                    return
                }

                pendingRequests.invalidate(expectedTargetId)
                sender.message(lang, { requestCancelled })
                target?.user?.message(lang, { requestCancelled })
            }
        })
    }

    override fun onDisable() {
        pendingRequests.invalidateAll()
        super.onDisable()
    }

}
