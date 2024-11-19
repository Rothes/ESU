package io.github.rothes.esu.bukkit.module

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.command.parser.PlayerUserParser
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.papermc.paper.configuration.GlobalConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.incendo.cloud.CommandBuilderSource
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.bukkit.parser.WorldParser
import org.incendo.cloud.bukkit.parser.location.Location2D
import org.incendo.cloud.bukkit.parser.location.Location2DParser
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.context.CommandContext

object UtilCommandsModule: BukkitModule<BaseModuleConfiguration, UtilCommandsModule.ModuleLocale>(
    BaseModuleConfiguration::class.java, ModuleLocale::class.java
) {

    override fun enable() {
        registerCommand {
            playerOptionalCmd("ping") { _, user, player ->
                user.message(locale, { pingCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("ping", player.ping.toString()))
            }
        }
        registerCommand {
            playerOptionalCmd("clientLocale") { _, user, player ->
                user.message(locale, { clientLocaleCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("locale", player.user.clientLocale))
            }
        }
        registerCommand {
            playerOptionalCmd("ip") { _, user, player ->
                user.message(locale, { ipCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("address", player.address.hostString))
            }
        }
        registerCommand {
            cmd("ipGroup").handler { context ->
                val user = context.sender()
                val list = Bukkit.getOnlinePlayers()
                    .groupBy { it.address.hostString }
                    .filter { it.value.size > 1 }
                    .mapValues { v -> v.value.map { it.name } }
                    .entries.sortedWith(compareBy({ it.value.size }, { it.key }))
                    .asReversed()
                if (list.isNotEmpty()) {
                    list.forEach {
                        user.message(locale, { ipGroupCommand.entry },
                            Placeholder.unparsed("address", it.key),
                            Placeholder.parsed("players", it.value.joinToString(
                                prefix = user.localed(locale) { ipGroupCommand.playerPrefix },
                                separator = user.localed(locale) { ipGroupCommand.playerSeparator })
                            ))
                    }
                } else {
                    user.message(locale, { ipGroupCommand.noSameIp } )
                }
            }
        }
        registerCommand {
            cmd("tpChunk")
                .required("chunk", Location2DParser.location2DComponent())
                .optional("world", WorldParser.worldParser(), DefaultValue.dynamic { (it.sender() as PlayerUser).player.location.world })
                .optional("player", PlayerParser.playerParser(), DefaultValue.dynamic { (it.sender() as PlayerUser).player }, PlayerUserParser())
                .handler { context ->
                    val location2D = context.get<Location2D>("chunk")
                    val sender = context.sender()
                    val player = context.get<Player>("player")
                    val world = context.get<World>("world")
                    sender.message(locale, { tpChunkTeleporting }, component("player", player.displayName()))
                    val location = Location(world, (location2D.x.toInt() shl 4) + 8.5, 0.0, (location2D.z.toInt() shl 4) + 8.5)
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
                        player.teleportAsync(location)
                    }
                }
        }
        registerCommand {
            cmd("genRateTop")
                .handler { context ->
                    val map = Bukkit.getOnlinePlayers()
                        .associateWith { player ->
                            (player as CraftPlayer).handle.`moonrise$getChunkLoader`()
                        }
                        .mapValues { entry ->
                            val loaderData = entry.value ?: return@mapValues null
                            val field = loaderData.javaClass.getDeclaredField("chunkGenerateTicketLimiter").also {
                                it.isAccessible = true
                            }
                            val limiter = field[loaderData] as AllocatingRateLimiter
                            val maxRate = GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkGenerateRate
                            maxRate - limiter.previewAllocation(System.nanoTime(), maxRate, maxRate.toLong())
                        }
                        .filter { entry ->
                            val value = entry.value
                            value != null && value > 0
                        }
                        .toList()
                        .sortedWith(compareBy { it.second })
                        .asReversed()
                    if (map.isNotEmpty()) {
                        context.sender().message(locale, { genRateTop.header })
                        map.forEach { (p, v) ->
                            context.sender().message(locale, { genRateTop.entry },
                                component("player", p.displayName()),
                                parsed("gen-rate", v))
                        }
                    } else {
                        context.sender().message(locale, { genRateTop.noData })
                    }
                }
        }
    }

    override fun disable() {
        super.reloadConfig()
    }

    private fun <C> CommandBuilderSource<C>.cmd(root: String) =
        commandBuilder(root).permission(perm("command.$root"))

    private fun <C> CommandBuilderSource<C>.playerOptionalCmd(root: String, handler: (CommandContext<C>, User, Player) -> Unit) =
        cmd(root)
            .optional("player", PlayerParser.playerParser(), DefaultValue.dynamic { (it.sender() as PlayerUser).player }, PlayerUserParser())
            .handler { context ->
                val player = context.get<Player>("player")
                handler(context, context.sender() as User, player)
            }

    data class ModuleLocale(
        val pingCommand: String = "<green><name>'s ping is <dark_aqua><ping>ms",
        val clientLocaleCommand: String = "<green><name>'s client locale is <dark_aqua><locale>",
        val ipCommand: String = "<green><name>'s ip is <dark_aqua><address>",
        val ipGroupCommand: IpGroupCommand = IpGroupCommand(),
        val tpChunkTeleporting: String = "<yellow>Teleporting <player>...",
        val genRateTop: GenRateTop = GenRateTop(),
    ): ConfigurationPart {

        data class IpGroupCommand(
            val noSameIp: String = "<green>There's no players on same ip",
            val entry: String = "<green><address>: <players>",
            val playerPrefix: String = "<dark_aqua>",
            val playerSeparator: String = "<gray>, <dark_aqua>",
        ): ConfigurationPart

        data class GenRateTop(
            val noData: String = "<green>There's no chunk generation at this moment.",
            val header: String = "<dark_green>[player]<gray>: <aqua>[chunk generations/sec]",
            val entry: String = "<green><player><gray>: <dark_aqua><gen-rate>",
        ): ConfigurationPart
    }
}