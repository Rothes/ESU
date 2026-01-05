package io.github.rothes.esu.common.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.user.UserManager
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.Placeholder
import kotlinx.coroutines.*
import org.incendo.cloud.parser.standard.DurationParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.time.Duration as KDuration

abstract class AbstractAutoRestartModule: CommonModule<AbstractAutoRestartModule.ModuleConfig, AbstractAutoRestartModule.ModuleLocale>() {

    override val name: String = "AutoRestart"

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    private var task: CoroutineScope? = null
    private var pausing: Boolean = false
    private var restartOn: Long? = null

    abstract val consoleUser: User
    abstract val rootCommand: String
    abstract val rootCommandAlias: String
    abstract fun runCommands()

    override fun doReload() {
        data = ConfigLoader.load(dataPath)
        super.doReload()
    }

    override fun onReload() {
        super.onReload()
        if (enabled) {
            scheduleTask()
        }
    }

    override fun onEnable() {
        scheduleTask()
        
        val cmd = EsuCore.instance.commandManager.commandBuilder(rootCommand, rootCommandAlias)
        val admin = cmd.permission(perm("command.admin"))
        registerCommandJvm {
            cmd.literal("check").handler { context ->
                val user = context.sender()
                val restartOn = restartOn
                if (restartOn == null) {
                    user.message(lang, { noTask })
                } else {
                    user.messageTimeParsed((restartOn - System.currentTimeMillis()).milliseconds) { notify }
                }
            }
        }
        registerCommandJvm {
            admin.literal("reset").handler { context ->
                data.restartOnOverride = null
                scheduleTask()
                context.sender().message(lang, { overridesReset })
                ConfigLoader.save(dataPath, data)
            }
        }
        registerCommandJvm {
            admin.literal("delayed").required("duration", DurationParser.durationParser()).handler { context ->
                val duration = context.get<Duration>("duration")
                data.restartOnOverride =
                    System.currentTimeMillis() + duration.toMillis() + 500 // Add some delay for notify
                scheduleTask()
                context.sender()
                    .messageTimeParsed((data.restartOnOverride!! - System.currentTimeMillis()).milliseconds) { overridesTo }
                ConfigLoader.save(dataPath, data)
            }
        }
        registerCommandJvm {
            admin.literal("schedule").required(
                "dateTime",
                StringParser.greedyStringParser(),
                SuggestionProvider.blockingStrings { context, _ ->
                    listOf(
                        context.sender().localed(lang) { timeFormatter })
                }).handler { context ->
                val raw = context.get<String>("dateTime")
                val localDateTime = try {
                    LocalDateTime.parse(
                        raw,
                        DateTimeFormatterBuilder().parseCaseInsensitive()
                            .parseDefaulting(ChronoField.YEAR, LocalDate.now().year.toLong()).appendPattern(
                                context.sender()
                                    .localed(lang) { timeFormatter })
                            .toFormatter()
                    )
                } catch (e: DateTimeParseException) {
                    return@handler context.sender().message(
                        lang,
                        { couldNotParseTime },
                        Placeholder.unparsed("message", e.message ?: "")
                    )
                }
                data.restartOnOverride = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                scheduleTask()
                context.sender()
                    .messageTimeParsed((data.restartOnOverride!! - System.currentTimeMillis()).milliseconds) { overridesTo }
                ConfigLoader.save(dataPath, data)
            }
        }
        registerCommandJvm {
            admin.literal("pause").handler { context ->
                pausing = !pausing
                scheduleTask()
                context.sender().message(
                    lang,
                    { toggledPausing },
                    Placeholder.unparsed("state", pausing.toString())
                )
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        task?.cancel()
        task = null
    }

    @Synchronized
    private fun scheduleTask() {
        task?.cancel()
        if (pausing) {
            restartOn = null
            return
        }
        val now = System.currentTimeMillis()
        if (data.restartOnOverride != null && data.restartOnOverride!! < now) {
            Logger.getLogger(name).warning("Resetting override restart time due to behind the current time.")
            data.restartOnOverride = null
            ConfigLoader.save(dataPath, data)
        }
        val restartOn = (if (data.restartOnOverride == null) {
            val localDateTime = config.restartAt.atDate(data.lastAutoRestartTime)
                .plus(config.restartInterval.toMillis(), ChronoUnit.MILLIS)
            var epoch = config.restartAt.atDate(localDateTime.toLocalDate()).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
            while (epoch < System.currentTimeMillis()) {
                epoch += 1.days.inWholeMilliseconds
            }
            epoch
        } else {
            data.restartOnOverride!!
        }).also { this.restartOn = it }

        val find = config.notifyRestartAt.find {
            restartOn - now > it.inWholeMilliseconds
        }
        val delayMillis = find?.let { (restartOn - now - it.inWholeMilliseconds) } ?: (restartOn - now)
        val scope = CoroutineScope(Dispatchers.Default)
        task = scope
        scope.launch {
            delay(delayMillis + 50) // Wait some time so message won't repeat
            if (find == null) {
                data.lastAutoRestartTime = LocalDate.now()
                data.restartOnOverride = null
                ConfigLoader.save(dataPath, data)

                if (config.kickPlayersOnRestart) {
                    UserManager.instance.getUsers().forEach { user ->
                        if (user.isOnline) user.kick(lang, { kickMessage })
                    }
                }

                runCommands()
            } else {
                UserManager.instance.getUsers().plus(consoleUser).forEach { user ->
                    if (user.isOnline) user.messageTimeParsed(find) { notify }
                }
                scheduleTask()
            }
        }
    }

    private fun User.messageTimeParsed(duration: KDuration, block: ModuleLocale.() -> MessageData?) {
        val instant = Instant.ofEpochMilli(restartOn!!).atZone(ZoneId.systemDefault())
        val time = if (duration < 1.days) instant.toLocalTime() else instant.toLocalDateTime().format(localed(
            lang
        ) { timeFormatterP })
        message(lang, block, duration(duration, this, "interval"), unparsed("time", time))
    }

    data class ModuleData(
        var lastAutoRestartTime: LocalDate = LocalDate.now().minusDays(1),
        var restartOnOverride: Long? = null,
    ): ConfigurationPart

    data class ModuleConfig(
        val commands: List<String> = listOf("stop"),
        val kickPlayersOnRestart: Boolean = true,
        val notifyRestartAt: List<KDuration> = listOf(KDuration.parse("10m"), KDuration.parse("5m"), KDuration.parse("1m"), KDuration.parse("30s"), KDuration.parse("5s")),
        val restartAt: LocalTime = LocalTime.parse("05:00:00"),
        val restartInterval: Duration = KDuration.parse("3d").toJavaDuration(),
    ): BaseModuleConfiguration()

    data class ModuleLocale(
        val timeFormatter: String = "MM/dd HH:mm:ss",
        val couldNotParseTime: MessageData = "<ec>The time you provided could not be parsed: <edc><message>".message,
        val noTask: MessageData = "<pc>This server has no scheduled restart!".message,
        val notify: MessageData = ("<sc><st>一一一一一一一一一一一一一一一一一一一一一一一一</st><br>" +
                "<pc>This server is restarting in <pdc><interval></pdc> at <pdc><time></pdc> !<br>" +
                "<sc><st>一一一一一一一一一一一一一一一一一一一一一一一一" +
                "<sound:minecraft:ui.stonecutter.take_result>").message,
        val overridesTo: MessageData = "<pc>Overrides restart time to <pdc><interval></pdc> at <pdc><time></pdc> !".message,
        val overridesReset: MessageData = "<pc>Reset restart time overrides. Now it's using the configured values.".message,
        val toggledPausing: MessageData = "<pc>Toggled pausing to <pdc><state>".message,
        val kickMessage: String = "<pc>Server restarting, please wait a minute."
    ): ConfigurationPart {
        @Transient
        val timeFormatterP = DateTimeFormatter.ofPattern(timeFormatter)!!
    }
}