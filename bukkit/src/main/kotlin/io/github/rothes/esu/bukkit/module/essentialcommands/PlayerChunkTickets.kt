package io.github.rothes.esu.bukkit.module.essentialcommands

import ca.spottedleaf.moonrise.common.PlatformHooks
import ca.spottedleaf.moonrise.common.util.MoonriseConstants
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Command

object PlayerChunkTickets : BaseCommand<FeatureToggle.DefaultTrue, PlayerChunkTickets.Lang>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ChunkLimiterHandler.isSupported) return errFail { "Server not supported".message }
            null
        }
    }

    override fun onEnable() {
        registerCommands(object {
            val HANDLER = ChunkLimiterHandler.instance

            @Command("tickDistance <num>")
            @ShortPerm("tickDistance")
            fun tickDistance(sender: User, num: Int) {
                if (num !in (2..32)) {
                    return sender.miniMessage("<ec>tickDistance should be in range of [2, 32]")
                }
                for (world in Bukkit.getWorlds()) {
                    world.chunkLoader.setTickDistance(num)
                    sender.message("${world.name} to ${world.simulationDistance}")
                }
            }
            @Command("viewDistance <num>")
            @ShortPerm("viewDistance")
            fun viewDistance(sender: User, num: Int) {
                if (num + 1 < 2 && num != -1) {
                    return sender.miniMessage("<ec>viewDistance should be >= 2")
                }
                if (checkMax(sender, num)) {
                    for (world in Bukkit.getWorlds()) {
                        world.chunkLoader.setLoadDistance(num + 1)
                        sender.message("${world.name} to ${world.viewDistance}")
                    }
                }
            }
            @Command("sendDistance <num>")
            @ShortPerm("sendDistance")
            fun sendDistance(sender: User, num: Int) {
                if (num < 0 && num != -1) {
                    return sender.miniMessage("<ec>sendDistance should be >= 0")
                }
                if (checkMax(sender, num)) {
                    for (world in Bukkit.getWorlds()) {
                        if (world.viewDistance < num) {
                            sender.miniMessage("<ec>ViewDistance of ${world.name} is ${world.viewDistance}, you need it higher or eq than $num")
                        }
                        world.chunkLoader.setSendDistance(num)
                        sender.message("${world.name} to ${world.sendViewDistance}")
                    }
                }
            }


            private fun checkMax(user: User, num: Int): Boolean {
                if (MoonriseConstants.MAX_VIEW_DISTANCE < num) {
                    user.miniMessage("<ec>Current MAX_VIEW_DISTANCE is ${MoonriseConstants.MAX_VIEW_DISTANCE}")
                    try {
                        val prop = PlatformHooks.get().brand + ".MaxViewDistance"
                        user.miniMessage("<ec>Add `-D$prop=$num` behind java in your server start commandline to override it")
                    } catch (_: NoClassDefFoundError) {
                        // Not a thing on 1.21.1
                        user.miniMessage("<ec>Your server version doesn't allow to override it safely, need a upgrade.")
                    }
                    return false
                }
                return true
            }

            private val World.chunkLoader
                get() = (this as CraftWorld).handle.`moonrise$getPlayerChunkLoader`()

            @Command("genRateTop")
            @ShortPerm("genRateTop")
            fun genRateTop(sender: User) {
                rate(sender, ChunkLimiterHandler.Type.GENERATE) { genRateTop }
            }

            @Command("loadRateTop")
            @ShortPerm("loadRateTop")
            fun loadRateTop(sender: User) {
                rate(sender, ChunkLimiterHandler.Type.LOAD) { loadRateTop }
            }

            @Command("sendRateTop")
            @ShortPerm("sendRateTop")
            fun sendRateTop(sender: User) {
                rate(sender, ChunkLimiterHandler.Type.SEND) { sendRateTop }
            }

            private fun rate(
                sender: User,
                type: ChunkLimiterHandler.Type,
                langScope: Lang.() -> Lang.ChunkRateTop
            ) {
                val map = Bukkit.getOnlinePlayers()
                    .associateWith { player -> HANDLER.getHighestAllocation(player, type) }
                    .filter { entry -> entry.value != 0L }
                    .toList()
                    .sortedByDescending { it.second }
                if (map.isEmpty()) {
                    sender.message(lang, { langScope().noData })
                } else {
                    sender.message(lang, { langScope().header })
                    map.forEach { (p, v) ->
                        sender.message(
                            lang, { langScope().entry }, player(p), unparsed("rate", v)
                        )
                    }
                }
            }
        })
    }

    data class Lang(
        val genRateTop: ChunkRateTop = ChunkRateTop(
            "<pc>There's no chunk generates at this moment.".message,
            "<pdc>[player]<pc>: <sc>[chunk generate tickets]",
        ),
        val loadRateTop: ChunkRateTop = ChunkRateTop(
            "<pc>There's no chunk loads at this moment.".message,
            "<pdc>[player]<pc>: <sc>[chunk load tickets]",
        ),
        val sendRateTop: ChunkRateTop = ChunkRateTop(
            "<pc>There's no chunk sends at this moment.".message,
            "<pdc>[player]<pc>: <sc>[chunk sends]",
        ),
    ) {
        data class ChunkRateTop(
            val noData: MessageData = "".message,
            val header: String = "",
            val entry: String = "<tdc><player><tc>: <sdc><rate>",
        ): ConfigurationPart
    }

}