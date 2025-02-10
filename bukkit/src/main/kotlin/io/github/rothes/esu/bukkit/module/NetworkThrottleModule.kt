package io.github.rothes.esu.bukkit.module

import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.networkthrottle.Analyser
import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle
import io.github.rothes.esu.bukkit.module.networkthrottle.HighLatencyAdjust
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.serializer.MapSerializer.DefaultedLinkedHashMap
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.Material
import org.incendo.cloud.annotations.Command
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

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

            @Command("network chunkDataThrottle view")
            @ShortPerm()
            fun chunkDataThrottleView(sender: User) {
                val (minimalChunks, resentChunks) = ChunkDataThrottle.counter
                sender.message("minimalChunks: $minimalChunks ; resentChunks: $resentChunks")
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
        @field:Comment("Helps to reduce chunk upload bandwidth. " +
                "Plugin will compress invisible blocks in chunk data packet." +
                "If necessary, we send a full chunk data again.\n" +
                "This can reduce about 50% bandwidth usage averagely. " +
                "Make sure you enabled network-compression on proxy or this server.")
        val chunkDataThrottle: ChunkDataThrottle = ChunkDataThrottle(),
        @field:Comment("Adjust the settings the players with high latency to lower value.\n" +
                "So they won't affect average quality of all players.")
        val highLatencyAdjust: HighLatencyAdjust = HighLatencyAdjust(),
    ): BaseModuleConfiguration() {

        data class ChunkDataThrottle(
            val enabled: Boolean = false,
            @field:Comment("The bedrock level(minimal height) is never visible unless you are in void.\n" +
                    "We would skip the check, and if you don't like it you can enable it.")
            val minimalHeightInvisibleCheck: Boolean = false,
            @field:Comment("Plugin will convert chunks with all non-visible blocks to single-valued palette format,\n" +
                    "This could save a lot of bandwidth. And since we are conflicting with anti-xray things,\n" +
                    "you can use this for some kind of substitution.\n" +
                    "We choose a random block from the list and make it of a 16*16*16 chunk section.")
            val singleValuedSectionBlock: DefaultedLinkedHashMap<String, List<Material>> = DefaultedLinkedHashMap<String, List<Material>>(
                listOf(Material.BEDROCK)
            ).apply {
                put("world", listOf(
                    Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,
                    Material.EMERALD_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
                    Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
                    Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
                ))
                put("world_nether", listOf(Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE))
            }
        ) {
            val singleValuedSectionBlockIds by lazy {
                with(singleValuedSectionBlock) {
                    DefaultedLinkedHashMap<String, IntArray>((default ?: listOf(Material.BEDROCK)).map { it.globalId }.toIntArray()).also {
                        it.putAll(entries.map { it.key to it.value.map { it.globalId }.toIntArray() })
                    }
                }
            }

            private val Material.globalId
                get() = if (!this.isBlock) error("Material $this is not a block type!") else SpigotConversionUtil.fromBukkitBlockData(createBlockData()).globalId
        }

        data class HighLatencyAdjust(
            val enabled: Boolean = false,
            @field:Comment("Trigger a adjust when player's ping is greater than or equal this.")
            val latencyThreshold: Int = 150,
            @field:Comment("The high ping must keep for the duration to trigger a adjust finally.")
            val duration: Duration = kotlin.time.Duration.parse("1m").toJavaDuration(),
            @field:Comment("Plugin detects CLIENT_SETTINGS packets to reset the view distance for players.\n" +
                    "If true, player must change the client view distance for a reset;\n" +
                    "If false, any new settings could reset the view distance for the player.")
            val newViewDistanceToReset: Boolean = false,
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


