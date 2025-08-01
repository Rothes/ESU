package io.github.rothes.esu.bukkit.module

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.module.UtilCommandsModule.ModuleLocale.ChunkRateTop
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.user
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.bukkit.parser.location.Location2D
import java.lang.reflect.Field

object UtilCommandsModule: BukkitModule<BaseModuleConfiguration, UtilCommandsModule.ModuleLocale>(
    BaseModuleConfiguration::class.java, ModuleLocale::class.java
) {

    override fun enable() {
        registerCommands(object {
            @Command("ping [player]")
            @ShortPerm("ping")
            fun ping(sender: User, player: Player = sender.player) {
                sender.message(locale, { pingCommand }, player(player), unparsed("ping", player.ping))
            }

            @Command("clientLocale [player]")
            @ShortPerm("clientLocale")
            fun clientLocale(sender: User, player: User = sender) {
                sender.message(locale, { clientLocaleCommand }, user(player), unparsed("locale", player.clientLocale))
            }

            @Command("ip [player]")
            @ShortPerm("ip")
            fun ip(sender: User, player: Player = sender.player) {
                sender.message(locale, { ipCommand }, player(player), unparsed("address", player.address!!.hostString))
            }

            @Command("ipGroup")
            @ShortPerm("ipGroup")
            fun ipGroup(sender: User) {
                val list = Bukkit.getOnlinePlayers()
                    .groupBy { it.address!!.hostString }
                    .filter { it.value.size > 1 }
                    .mapValues { v -> v.value.map { it.name } }
                    .entries.sortedWith(compareBy({ it.value.size }, { it.key }))
                    .asReversed()
                if (list.isNotEmpty()) {
                    list.forEach {
                        sender.message(locale, { ipGroupCommand.entry },
                            unparsed("address", it.key),
                            parsed("players", it.value.joinToString(
                                prefix = sender.localed(locale) { ipGroupCommand.playerPrefix },
                                separator = sender.localed(locale) { ipGroupCommand.playerSeparator })
                            ))
                    }
                } else {
                    sender.message(locale, { ipGroupCommand.noSameIp } )
                }
            }

            @Command("tpChunk <chunk> [world] [player]")
            @ShortPerm("tpChunk")
            fun tpChunk(sender: User, chunk: Location2D, world: World = sender.player.location.world, player: Player = sender.player) {
                sender.message(locale, { tpChunkTeleporting }, player(player))
                val location = Location(world, (chunk.x.toInt() shl 4) + 8.5, 0.0, (chunk.z.toInt() shl 4) + 8.5)
                Scheduler.schedule(location) {
                    val y = if (world.environment == World.Environment.NETHER) {
                        var y = 125
                        while (y > 0) {
                            y--
                            if (world.getBlockAt(location.blockX, y + 2, location.blockZ).type.isEmpty
                                && world.getBlockAt(location.blockX, y + 1, location.blockZ).type.isEmpty
                                && world.getBlockAt(location.blockX, y, location.blockZ).type.isSolid) {
                                break
                            }
                        }
                        y
                    } else {
                        world.getHighestBlockYAt(location)
                    }
                    location.y = y.toDouble() + 1
                    player.tp(location)
                }
            }
        })
        PaperChunkCommands.enable()
    }

    override fun disable() {
        super.reloadConfig()
    }

    private object PaperChunkCommands {

        fun enable() {
            try {
                val clazz = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java // Only paper 1.18+ (to be confirmed)
                registerCommands(object {

                    private val gen = clazz.getDeclaredField("chunkGenerateTicketLimiter").also { it.isAccessible = true }
                    @Command("genRateTop")
                    @ShortPerm("genRateTop")
                    fun genRateTop(sender: User) {
                        rate(sender, gen, GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkGenerateRate) { genRateTop }
                    }

                    private val load = clazz.getDeclaredField("chunkLoadTicketLimiter").also { it.isAccessible = true }
                    @Command("loadRateTop")
                    @ShortPerm("loadRateTop")
                    fun loadRateTop(sender: User) {
                        rate(sender, load, GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkLoadRate) { loadRateTop }
                    }
                    // We don't add sendRate command as it doesn't have the logic for this.

                    private fun rate(sender: User, field: Field, maxRate: Double, lang: ModuleLocale.() -> ChunkRateTop) {
                        val map = Bukkit.getOnlinePlayers().associateWith { player ->
                            (player as CraftPlayer).handle.`moonrise$getChunkLoader`()
                        }.mapValues { entry ->
                            val loaderData = entry.value ?: return@mapValues null
                            val limiter = field[loaderData] as AllocatingRateLimiter
                            maxRate - limiter.previewAllocation(System.nanoTime(), maxRate, maxRate.toLong())
                        }.filter { entry ->
                            val value = entry.value
                            value != null && value > 0
                        }.toList().sortedByDescending { it.second }
                        if (map.isEmpty()) {
                            sender.message(locale, { lang().noData })
                        } else {
                            sender.message(locale, { lang().header })
                            map.forEach { (p, v) ->
                                sender.message(
                                    locale, { lang().entry }, player(p), unparsed("rate", v)
                                )
                            }
                        }
                    }
                })
            } catch (e: NoClassDefFoundError) {
                plugin.warn("Cannot register chunk rate commands: $e")
            }
        }
    }

    private val User.pu
        get() = this as PlayerUser

    private val User.player
        get() = this.pu.player

    data class ModuleLocale(
        val pingCommand: MessageData = "<pdc><player><pc>'s ping is <sdc><ping><sc>ms".message,
        val clientLocaleCommand: MessageData = "<pdc><player><pc>'s client locale is <sdc><locale>".message,
        val ipCommand: MessageData = "<pdc><player><pc>'s ip is <sdc><address>".message,
        val ipGroupCommand: IpGroupCommand = IpGroupCommand(),
        val tpChunkTeleporting: MessageData = "<tc>Teleporting <tdc><player></tdc>...".message,
        val genRateTop: ChunkRateTop = ChunkRateTop(
            "<pc>There's no chunk generate at this moment.".message,
            "<pdc>[player]<pc>: <sc>[chunk generate tickets/sec]",
        ),
        val loadRateTop: ChunkRateTop = ChunkRateTop(
            "<pc>There's no chunk load at this moment.".message,
            "<pdc>[player]<pc>: <sc>[chunk load tickets/sec]",
        ),
    ): ConfigurationPart {

        data class IpGroupCommand(
            val noSameIp: MessageData = "<pc>There's no players on same ip.".message,
            val entry: MessageData = "<tdc><address><tc>: <players>".message,
            val playerPrefix: String = "<sdc>",
            val playerSeparator: String = "<pc>, <sdc>",
        ): ConfigurationPart

        data class ChunkRateTop(
            val noData: MessageData = "".message,
            val header: String = "",
            val entry: String = "<tdc><player><tc>: <sdc><rate>",
        ): ConfigurationPart


    }
}