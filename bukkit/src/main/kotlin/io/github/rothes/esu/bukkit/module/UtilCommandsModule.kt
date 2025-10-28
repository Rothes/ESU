package io.github.rothes.esu.bukkit.module

import ca.spottedleaf.moonrise.common.PlatformHooks
import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import ca.spottedleaf.moonrise.common.util.MoonriseConstants
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocation
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
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import java.lang.reflect.Field

object UtilCommandsModule: BukkitModule<BaseModuleConfiguration, UtilCommandsModule.ModuleLocale>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("ping [player]")
            @ShortPerm("ping")
            fun ping(sender: User, player: Player = sender.player) {
                sender.message(lang, { pingCommand }, player(player), unparsed("ping", player.ping))
            }

            @Command("clientLocale [player]")
            @ShortPerm("clientLocale")
            fun clientLocale(sender: User, player: User = sender) {
                sender.message(lang, { clientLocaleCommand }, user(player), unparsed("locale", player.clientLocale))
            }

            @Command("ip [player]")
            @ShortPerm("ip")
            fun ip(sender: User, player: Player = sender.player) {
                sender.message(lang, { ipCommand }, player(player), unparsed("address", player.address!!.hostString))
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
                        sender.message(lang, { ipGroupCommand.entry },
                            unparsed("address", it.key),
                            parsed("players", it.value.joinToString(
                                prefix = sender.localed(lang) { ipGroupCommand.playerPrefix },
                                separator = sender.localed(lang) { ipGroupCommand.playerSeparator })
                            ))
                    }
                } else {
                    sender.message(lang, { ipGroupCommand.noSameIp } )
                }
            }

            @Command("tpChunk <chunk> [world] [player]")
            @ShortPerm("tpChunk")
            fun tpChunk(sender: User, chunk: ChunkLocation, world: World = sender.player.location.world, player: Player = sender.player) {
                sender.message(lang, { tpChunkTeleporting }, player(player))
                val location = Location(world, (chunk.chunkX shl 4) + 8.5, 0.0, (chunk.chunkZ shl 4) + 8.5)
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

    override fun onDisable() {
        super.onReload()
    }

    private object PaperChunkCommands {

        fun enable() {
            try {
                val clazz = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java // Only paper 1.18+ (to be confirmed)
                registerCommands(object {

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
                            sender.message(UtilCommandsModule.lang, { this.lang().noData })
                        } else {
                            sender.message(UtilCommandsModule.lang, { this.lang().header })
                            map.forEach { (p, v) ->
                                sender.message(
                                    UtilCommandsModule.lang, { this.lang().entry }, player(p), unparsed("rate", v)
                                )
                            }
                        }
                    }
                })
            } catch (e: NoClassDefFoundError) {
                plugin.warn("Cannot register chunk rate & view distance commands: $e")
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