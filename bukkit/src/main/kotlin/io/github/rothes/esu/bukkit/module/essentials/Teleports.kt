package io.github.rothes.esu.bukkit.module.essentials

import io.github.rothes.esu.bukkit.module.essentials.teleports.Tpa
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature

object Teleports : CommonFeature<Unit, Teleports.Lang>() {

    init {
        registerFeature(Tpa)
    }

    override fun onEnable() {}

    data class Lang(
        val tpaRequestSent: MessageData = "<pc>Teleport request sent to <pdc><player></pdc>.".message,
        val tpaRequestReceived: MessageData = "<pc><pdc><player></pdc> has requested to teleport to you.\n<pc>Type <click:run_command:'/tpaccept'><tc>/tpaccept</tc></click> to accept or <click:run_command:'/tpdeny'><tc>/tpdeny</tc></click> to deny.".message,
        val tpaNoPendingRequest: MessageData = "<ec>You have no pending teleport requests.".message,
        val tpaRequestAccepted: MessageData = "<pc>Teleport request accepted.".message,
        val tpaRequestDenied: MessageData = "<pc>Teleport request denied.".message,
        val tpaRequestCancelled: MessageData = "<pc>Teleport request cancelled.".message,
        val tpaPlayerNotOnline: MessageData = "<ec>That player is not online.".message,
        val tpaCannotRequestSelf: MessageData = "<ec>You cannot teleport to yourself.".message,
        val tpaRequestAlreadySent: MessageData = "<ec>You have already sent a teleport request to this player.".message,
    ) : ConfigurationPart

}
