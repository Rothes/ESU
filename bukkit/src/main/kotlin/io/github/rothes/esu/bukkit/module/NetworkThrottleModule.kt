package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.networkthrottle.Analyser
import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle
import io.github.rothes.esu.bukkit.module.networkthrottle.HighLatencyAdjust
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

object NetworkThrottleModule: BukkitModule<NetworkThrottleModule.ModuleConfig, NetworkThrottleModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun enable() {
        data = ConfigLoader.load(dataPath)
        registerCommands(object {
            @Command("network analyser start")
            @ShortPerm("analyser")
            fun analyserStart(sender: User) {
                if (Analyser.start()) {
                    sender.message(locale, { analyser.started })
                } else {
                    sender.message(locale, { analyser.alreadyStarted })
                }
            }

            @Command("network analyser stop")
            @ShortPerm("analyser")
            fun analyserStop(sender: User) {
                if (Analyser.stop()) {
                    sender.message(locale, { analyser.stopped })
                } else {
                    sender.message(locale, { analyser.alreadyStopped })
                }
            }

            @Command("network analyser view")
            @ShortPerm("analyser")
            fun analyserView(sender: User) {
                val entries = Analyser.records
                    .mapValues { it.value.toList().sumOf { it.size.toLong() } }
                    .entries.sortedByDescending { it.value }
                if (entries.isEmpty()) {
                    sender.message(locale, { analyser.view.noData })
                    return
                }
                sender.message(locale, { analyser.view.header })
                for ((k, bytes) in entries.take(7)) {
                    sender.message(locale, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("size", when {
                            bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
                            bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
                            bytes >= 1 shl 10 -> "%.1f KB".format(bytes.toDouble() / (1 shl 10))
                            else              -> "$bytes bytes"
                        }))
                }
                sender.message(locale, { analyser.view.footer },
                    duration(
                        (if (Analyser.running) {
                            System.currentTimeMillis() - Analyser.startTime
                        } else {
                            Analyser.stopTime - Analyser.startTime
                        }).milliseconds, sender
                    ))
            }
        })
        Analyser // Init this
        HighLatencyAdjust.onEnable()
        ChunkDataThrottle.onEnable()
    }

    override fun disable() {
        super.disable()
        ChunkDataThrottle.onDisable()
        HighLatencyAdjust.onDisable()
        Analyser.stop()
        ConfigLoader.save(dataPath, data)
    }


    data class ModuleData(
        val minimalChunks: MutableMap<UUID, MutableList<Long>> = linkedMapOf(),
        val originalViewDistance: MutableMap<UUID, Int> = linkedMapOf(),
    )

    data class ModuleConfig(
        @field:Comment("Helps to reduce chunk upload bandwidth.\n" +
                "Plugin will only send visible blocks if players are moving fast,\n" +
                "If necessary, we send a full chunk data again.\n" +
                "This can reduce 33% ~ 50% bandwidth usage averagely.")
        val chunkDataThrottle: ChunkDataThrottle = ChunkDataThrottle(),
        @field:Comment("Adjust the settings the players with high latency to lower value.\n" +
                "So they won't affect average quality of all players.")
        val highLatencyAdjust: HighLatencyAdjust = HighLatencyAdjust(),
    ): BaseModuleConfiguration() {

        data class ChunkDataThrottle(
            val enabled: Boolean = false,
            val maxHeightToProceed: Int = 140,
            val cacheExpireTicks: Int = 2 * 60 * 60 * 20,
        )

        data class HighLatencyAdjust(
            val enabled: Boolean = false,
            val latencyThreshold: Int = 150,
            val minViewDistance: Int = 5,
        )
    }

    data class ModuleLang(
        val highLatencyAdjust: HighLatencyAdjust = HighLatencyAdjust(),
        val analyser: Analyser = Analyser(),
    ): ConfigurationPart {

        data class HighLatencyAdjust(
            val adjustedWarning: MessageData = ("<ec><b>Warning: </b><pc>Your network latency seems to be high. \n" +
                    "To enhance your experience, we have adjusted your view distance. " +
                    "You can always adjust it yourself in the game options.").message,
        )

        data class Analyser(
            val started: MessageData = "<pc>Started the analyser.".message,
            val stopped: MessageData = "<pc>Stopped the analyser.".message,
            val alreadyStarted: MessageData = "<ec>The analyser is already running.".message,
            val alreadyStopped: MessageData = "<ec>The analyser is already stopped.".message,

            val view: View = View(),
        ) {

            data class View(
                val noData: MessageData = "<pc>There's no data for view.".message,
                val header: MessageData = "<pdc>[Packet Type]<pc>: <sc>[size]".message,
                val entry: MessageData = "<tdc><packet-type><tc>: <sdc><size>".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }
    }

}


