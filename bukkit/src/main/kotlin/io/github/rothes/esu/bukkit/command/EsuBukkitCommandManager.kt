package io.github.rothes.esu.bukkit.command

import io.github.rothes.esu.bukkit.command.parser.NmsRegistryValueParsers
import io.github.rothes.esu.bukkit.command.parser.UserParser
import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocationParser
import io.github.rothes.esu.bukkit.config.BukkitEsuLang
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.user.User
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CloudCapability
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import kotlin.jvm.optionals.getOrNull

class EsuBukkitCommandManager: LegacyPaperCommandManager<User>(
    plugin,
    ExecutionCoordinator.asyncCoordinator(),
    SenderMapper.create(
        {
            when (it) {
                is ConsoleCommandSender -> ConsoleUser
                is Player               -> it.user
                else                    -> GenericUser(it)
            }
        },
        { it.commandSender as CommandSender }
    )
) {
    init {
//        if (hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
//            registerBrigadier()
//        } else
        if (hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            registerAsynchronousCompletions()
        }
        settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
        captionRegistry().registerProvider { caption, recipient ->
            recipient.localedOrNull(BukkitEsuLang.get()) {
                commandCaptions[caption]
            }
        }
        parserRegistry().registerParser(ChunkLocationParser.parser())
        parserRegistry().registerParser(UserParser.parser())
        parserRegistry().registerNamedParser("greedyString", StringParser.greedyStringParser())
        EsuExceptionHandlers(exceptionController()).register()

        if (NmsRegistryValueParsers.isSupported) {
            for (parser in NmsRegistryValueParsers.all<User>()) {
                parserRegistry().registerParser(parser)
            }
        }
    }

    override fun hasCapability(capability: CloudCapability): Boolean {
        val sp = super.hasCapability(capability)
        if (capability == CloudBukkitCapabilities.BRIGADIER) {
            if (sp && StackWalker.getInstance().walk { it.skip(1).findFirst() }.getOrNull()?.methodName == "unregisterRootCommand") {
                // org.incendo.cloud.bukkit.BukkitPluginRegistrationHandler.unregisterRootCommand
                // We don't want it to call Player::updateCommands
                return false
            }
        }
        return sp
    }

}