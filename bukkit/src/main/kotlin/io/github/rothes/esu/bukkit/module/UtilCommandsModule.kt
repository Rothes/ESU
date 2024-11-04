package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.command.parser.PlayerUserParser
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.CommandBuilderSource
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.context.CommandContext

object UtilCommandsModule: CommonModule<BaseModuleConfiguration, UtilCommandsModule.ModuleLocale>(
    BaseModuleConfiguration::class.java, ModuleLocale::class.java
) {

    override fun enable() {
        with(plugin.commandManager) {
            command(playerOptionalCmd("ping") { _, user, player ->
                user.message(locale, { pingCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("ping", player.ping.toString()))
            })
            command(playerOptionalCmd("clientLocale") { _, user, player ->
                user.message(locale, { clientLocaleCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("locale", player.user.clientLocale))
            })
            command(playerOptionalCmd("ip") { _, user, player ->
                user.message(locale, { ipCommand },
                    Placeholder.unparsed("name", player.name),
                    Placeholder.unparsed("address", player.address.hostString))
            })
            command(cmd("ipGroup").handler { context ->
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
            })
        }
    }

    override fun disable() {
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
    ): ConfigurationPart {

        data class IpGroupCommand(
            val noSameIp: String = "<green>There's no players on same ip",
            val entry: String = "<green><address>: <players>",
            val playerPrefix: String = "<dark_aqua>",
            val playerSeparator: String = "<gray>, <dark_aqua>",
        ): ConfigurationPart
    }
}