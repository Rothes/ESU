package io.github.rothes.esu.bukkit.module

import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.rothes.esu.bukkit.module.networkthrottle.Analyser
import io.github.rothes.esu.bukkit.module.networkthrottle.ChunkDataThrottle
import io.github.rothes.esu.bukkit.module.networkthrottle.DynamicChunkSendRate
import io.github.rothes.esu.bukkit.module.networkthrottle.HighLatencyAdjust
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.serializer.MapSerializer.DefaultedLinkedHashMap
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.bytes
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.configuration.meta.RenamedFrom
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.PostProcess
import org.bukkit.Material
import org.incendo.cloud.annotations.Command
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
                    .mapValues {
                        val list = it.value.toList()
                        list.size to list.sumOf { it.size.toLong() }
                    }
                    .entries.sortedByDescending { it.value.second }
                if (entries.isEmpty()) {
                    sender.message(locale, { analyser.view.noData })
                    return
                }
                sender.message(locale, { analyser.view.header })
                for ((k, entry) in entries.take(7)) {
                    val (counts, bytes) = entry
                    sender.message(locale, { analyser.view.entry },
                        unparsed("packet-type", k.name.lowercase()),
                        unparsed("counts", counts),
                        bytes(bytes, "size"),
                    )
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
            @ShortPerm("chunkDataThrottle")
            fun chunkDataThrottleView(sender: User) {
                val (minimalChunks, resentChunks, resentBlocks) = ChunkDataThrottle.counter
                sender.message("minimalChunks: $minimalChunks ; resentChunks: $resentChunks ; resentBlocks: $resentBlocks")
            }
        })
        Analyser // Init this
        HighLatencyAdjust.onEnable()
        ChunkDataThrottle.onEnable()
        DynamicChunkSendRate.enable()
    }

    override fun disable() {
        super.disable()
        ChunkDataThrottle.onDisable()
        HighLatencyAdjust.onDisable()
        DynamicChunkSendRate.disable()
        Analyser.stop()
        ConfigLoader.save(dataPath, data)
    }

    override fun reloadConfig() {
        super.reloadConfig()
        if (enabled) {
            if (config.dynamicChunkSendRate.enabled) {
                DynamicChunkSendRate.enable()
            } else {
                DynamicChunkSendRate.disable()
            }
        }
    }


    data class ModuleData(
        val originalViewDistance: MutableMap<UUID, Int> = linkedMapOf(),
    )

    data class ModuleConfig(
        @Comment("""
            Helps to reduce chunk upload bandwidth. Plugin will compress invisible blocks in chunk data packet.
            If necessary, we send a full chunk data again.
            This can save about 50% bandwidth usage in overworld and 30% in nether averagely.
            Make sure you have enabled network-compression on proxy or this server.
            """)
        val chunkDataThrottle: ChunkDataThrottle = ChunkDataThrottle(),
        @Comment("Enable DynamicChunkSendRate. Make sure you have velocity mode on, and installed ESU on velocity.")
        val dynamicChunkSendRate: DynamicChunkSendRate = DynamicChunkSendRate(),
        @Comment("""
            Adjust the settings the players with high latency to lower value.
            So they won't affect average quality of all players.
            """)
        val highLatencyAdjust: HighLatencyAdjust = HighLatencyAdjust(),
    ): BaseModuleConfiguration() {

        data class ChunkDataThrottle(
            val enabled: Boolean = true,
            @Comment("""
                Plugin will resent whole chunk data if resent block amount exceeds this value.
                Set it to -1 will never resent chunk but keep updating nearby blocks, 
                 0 to always resent chunks.
                """)
            val thresholdToResentWholeChunk: Int = 448,
            @Comment("""
                We updates the nearby blocks when a player digs a block immediately.
                If this is enabled, we will check if the block is in the interaction range
                 of the player with a rough calculation.
                """)
            val updateOnLegalInteractOnly: Boolean = true,
            @Comment("How many distance of blocks to update from the center when necessary.")
            val updateDistance: Int = 2,
            @Comment("""
                The bedrock level(minimal height) is never visible unless you are in void.
                We would skip the check, and if you don't like it you can enable it.
                """)
            val minimalHeightInvisibleCheck: Boolean = false,
            @Comment("""
                Same with minimal-height but it's for nether roof. For out-of-the-box, it's true by default.
                It's highly recommend to set it to FALSE if you don't allow players to get above there.
                """,
                overrideOld = ["Same with minimal-height but it's for nether roof."]
            )
            val netherRoofInvisibleCheck: Boolean = true,
            @RenamedFrom("single-valued-section-block-list")
            @Comment("""
                    This feature doesn't support running along with any other anti-xray plugins.
                    You must use the anti-xray here we provide.
                    
                    We will send non-visible blocks to one of the random block in this list.
                    If you don't like to anti-xray, you can set the list to 'bedrock'.
                """,
                overrideOld = ["Plugin will convert chunks with all non-visible blocks to single-valued palette format,\nThis could save a lot of bandwidth. And since we are conflicting with anti-xray things,\nyou can use this for some kind of substitution.\nWe choose a random block from the list and make it of a 16*16*16 chunk section."]
            )
            val antiXrayRandomBlockList: DefaultedLinkedHashMap<String, MutableList<Material>> = DefaultedLinkedHashMap<String, MutableList<Material>>(
                mutableListOf(Material.BEDROCK)
            ).apply {
                put("world", buildList {
                    val cavesUpdate = ServerCompatibility.serverVersion >= Version.fromString("1.17")
                    add(Material.COAL_ORE)
                    if (cavesUpdate) add(Material.COPPER_ORE)
                    addAll(listOf(Material.IRON_ORE, Material.GOLD_ORE,
                        Material.EMERALD_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE))

                    if (cavesUpdate) addAll(listOf(Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE,
                        Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                        Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE))
                }.toMutableList())
                put("world_nether", buildList {
                    add(Material.NETHER_QUARTZ_ORE)
                    if (ServerCompatibility.serverVersion >= Version.fromString("1.16")) {
                        add(Material.NETHER_GOLD_ORE)
                        add(Material.ANCIENT_DEBRIS)
                    }
                }.toMutableList())
                put("world_the_end", mutableListOf(Material.END_STONE))
            },
            @Comment("""
                If enabled, we add a extra block type to chunk section palettes for the random block.
                This will greatly enhance anti-xray capabilities while giving only few bytes of additional bandwidth.
            """)
            val enhancedAntiXray: Boolean = true,
        ) {

            @RemovedNode
            val rebuildPaletteMappings: Boolean? = null

            val antiXrayRandomBlockIds by lazy {
                with(antiXrayRandomBlockList) {
                    DefaultedLinkedHashMap<String, IntArray>((default ?: listOf(Material.BEDROCK)).map { it.globalId }.toIntArray()).also {
                        it.putAll(entries.map { it.key to it.value.map { it.globalId }.toIntArray() })
                    }
                }
            }

            private val Material.globalId
                get() = if (!this.isBlock) error("Material $this is not a block type!") else SpigotConversionUtil.fromBukkitBlockData(createBlockData()).globalId

            @PostProcess
            private fun postProcess() {
                fun checkEmptyBlockList(key: String, list: MutableList<Material>) {
                    if (list.isEmpty()) {
                        list.add(Material.BEDROCK)
                        plugin.warn("[ChunkDataThrottle] Anti-xray random block list of '$key' is empty! We have added bedrock to it.")
                    }
                }
                antiXrayRandomBlockList.default?.let {
                    checkEmptyBlockList("default", it)
                }
                antiXrayRandomBlockList.entries.toList().forEach {
                    checkEmptyBlockList(it.key, it.value)
                }
            }
        }

        data class DynamicChunkSendRate(
            val enabled: Boolean = true,
        )

        data class HighLatencyAdjust(
            val enabled: Boolean = false,
            @Comment("Trigger a adjust when player's ping is greater than or equal this.")
            val latencyThreshold: Int = 150,
            @Comment("The high ping must keep for the duration to trigger a adjust finally.")
            val duration: Duration = kotlin.time.Duration.parse("1m").toJavaDuration(),
            @Comment("Plugin detects CLIENT_SETTINGS packets to reset the view distance for players.\n" +
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
                val header: MessageData = "<pdc>[Packet Type]<pc> <pc>[count]</pc>: <sc>[size]".message,
                val entry: MessageData = "<tdc><packet-type> <pc>x<pdc><counts></pc><tc>: <sdc><size>".message,
                val footer: MessageData = "<pc>The analyser has been running for <duration>".message,
            )
        }
    }

}


