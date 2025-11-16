package io.github.rothes.esu.bukkit.module.essencialcommands

import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.Bukkit
import org.incendo.cloud.annotations.Command

object IpGroup : CommonFeature<FeatureToggle.DefaultTrue, IpGroup.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("ipGroup")
            @ShortPerm
            fun ipGroup(sender: User) {
                val list = Bukkit.getOnlinePlayers()
                    .groupBy { it.address!!.hostString }
                    .filter { it.value.size > 1 }
                    .mapValues { v -> v.value.map { it.name } }
                    .entries.sortedWith(compareBy({ it.value.size }, { it.key }))
                    .asReversed()
                if (list.isNotEmpty()) {
                    list.forEach {
                        sender.message(
                            lang, { entry },
                            unparsed("address", it.key),
                            parsed("players", it.value.joinToString(
                                prefix = sender.localed(lang) { playerPrefix },
                                separator = sender.localed(lang) { playerSeparator })
                            ))
                    }
                } else {
                    sender.message(lang, { noSameIp } )
                }
            }
        })
    }

    data class Lang(
        val noSameIp: MessageData = "<pc>There's no players on same ip.".message,
        val entry: MessageData = "<tdc><address><tc>: <players>".message,
        val playerPrefix: String = "<sdc>",
        val playerSeparator: String = "<pc>, <sdc>",
    )

}