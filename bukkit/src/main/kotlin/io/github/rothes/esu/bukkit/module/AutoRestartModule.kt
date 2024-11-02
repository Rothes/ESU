package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.incendo.cloud.parser.standard.DurationParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider
import org.yaml.snakeyaml.internal.Logger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.time.Duration as KDuration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

object AutoRestartModule: CommonModule<AutoRestartModule.ConfigData, AutoRestartModule.ModuleLocale>(ConfigData::class.java, ModuleLocale::class.java) {

    private var task: ScheduledTask? = null
    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    private var pausing: Boolean = false
    private var restartOnOverride: Long? = null
    private var restartOn: Long? = null

    override fun reloadConfig() {
        super.reloadConfig()
        data = ConfigLoader.load(dataPath)
        if (enabled) {
            scheduleTask()
        }
    }

    override fun enable() {
        scheduleTask()

        with(plugin.commandManager) {
            val cmd = commandBuilder("autorestart", "ar")
            command(cmd.literal("check").handler { context ->
                val user = context.sender()
                if (restartOn == null) {
                    user.message(locale, { noTask })
                } else {
                    val restartOn = restartOn!!
                    user.messageTimeParsed((restartOn - System.currentTimeMillis()).milliseconds) { notify }
                }
            })
            val admin = cmd.permission(perm("command.admin"))
            command(admin.literal("reset").handler { context ->
                restartOnOverride = null
                context.sender().message(locale, { overridesReset })
                scheduleTask()
            })
            command(admin.literal("delayed").required("duration", DurationParser.durationParser()).handler { context ->
                val duration = context.get<Duration>("duration")
                restartOnOverride = System.currentTimeMillis() + duration.toMillis()
                scheduleTask()
                context.sender().messageTimeParsed((restartOnOverride!! - System.currentTimeMillis()).milliseconds) { overridesTo }
            })
            command(admin.literal("schedule").required("dateTime", StringParser.greedyStringParser(),
                SuggestionProvider.blockingStrings { context, _ ->
                    listOf(context.sender().localed(locale) { timeFormatter })
                }).handler { context ->
                val raw = context.get<String>("dateTime")
                val localDateTime = try {
                    LocalDateTime.parse(raw, DateTimeFormatterBuilder().parseCaseInsensitive()
                        .parseDefaulting(ChronoField.YEAR, LocalDate.now().year.toLong())
                        .appendPattern(context.sender().localed(locale) { timeFormatter }).toFormatter() )
                } catch (e: DateTimeParseException) {
                    return@handler context.sender().message(locale, { couldNotParseTime }, Placeholder.unparsed("message", e.message ?: ""))
                }
                restartOnOverride = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                scheduleTask()
                context.sender().messageTimeParsed((restartOnOverride!! - System.currentTimeMillis()).milliseconds) { overridesTo }
            })
            command(admin.literal("pause").handler { context ->
                pausing = !pausing
                scheduleTask()
                context.sender().message(locale, { toggledPausing }, Placeholder.unparsed("state", pausing.toString()))
            })
        }
    }

    override fun disable() {
        task?.cancel()
        task = null
    }

    private fun scheduleTask() {
        task?.cancel()
        task = null
        if (pausing) {
            restartOn = null
            return
        }
        val now = System.currentTimeMillis()
        if (restartOnOverride != null && restartOnOverride!! < now) {
            Logger.getLogger(name).warn("Resetting override restart time due to it's behind the current time.")
            restartOnOverride = null
        }
        val restartOn = (if (restartOnOverride == null) {
            val localDateTime = config.restartAt.atDate(data.lastAutoRestartTime)
                .plus(config.restartInterval.toMillis(), ChronoUnit.MILLIS)
            var epoch = config.restartAt.atDate(localDateTime.toLocalDate()).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
            while (epoch < System.currentTimeMillis()) {
                epoch += 1.days.inWholeMilliseconds
            }
            epoch
        } else {
            restartOnOverride!!
        }).also { this.restartOn = it }

        val find = config.notifyRestartAt.find {
            restartOn - now > it.inWholeMilliseconds
        }
        val delayMillis = find?.let { (restartOn - now - it.inWholeMilliseconds) } ?: (restartOn - now)
        task = Scheduler.async(delayMillis / 50) {
            if (find == null) {
                data.lastAutoRestartTime = LocalDate.now()
                ConfigLoader.save(dataPath, data)
                Scheduler.global {
                    Bukkit.getOnlinePlayers().map { it.user }.forEach { user ->
                        user.kick(locale, { kickMessage })
                    }
                    config.commands.forEach {
                        Bukkit.dispatchCommand(ConsoleUser.commandSender, it)
                    }
                }
            } else {
                Bukkit.getOnlinePlayers().map { it.user }.plus(ConsoleUser).forEach { user ->
                    user.messageTimeParsed(find) { notify }
                }
                scheduleTask()
            }
        }
    }

    private fun BukkitUser.messageTimeParsed(duration: KDuration, block: ModuleLocale.() -> String?) {
        val instant = Instant.ofEpochMilli(restartOn!!).atZone(ZoneId.systemDefault())
        val time = if (duration < 1.days) instant.toLocalTime() else instant.toLocalDateTime().format(localed(locale) { timeFormatterP })
        message(locale, block,
            Placeholder.unparsed("interval", duration.toString()),
            Placeholder.unparsed("time", time.toString()))
    }

    data class ModuleData(
        var lastAutoRestartTime: LocalDate = LocalDate.now().minusDays(1),
    ): ConfigurationPart

    data class ConfigData(
        val commands: List<String> = listOf("stop"),
        val notifyRestartAt: List<KDuration> = listOf(KDuration.parse("10m"), KDuration.parse("5m"), KDuration.parse("1m"), KDuration.parse("5s")),
        val restartAt: LocalTime = LocalTime.parse("05:00:00"),
        val restartInterval: Duration = KDuration.parse("3d").toJavaDuration(),
    ): BaseModuleConfiguration()

    data class ModuleLocale(
        val timeFormatter: String = "MM/dd HH:mm:ss",
        val couldNotParseTime: String = "<red>The time you provided could not be parsed: <message>",
        val noTask: String = "<gold>This server has no scheduled restart!",
        val notify: String = "<gold>This server is going to be restarted in <interval> at <time> !",
        val overridesTo: String = "<gold>Overrides restart time in <interval> at <time> !",
        val overridesReset: String = "<gold>Reset restart time overrides. Now it's using the configured values.",
        val toggledPausing: String = "<gold>Toggled pausing to <state>",
        val kickMessage: String = "<gold>Server restarting, please wait a minute."
    ): ConfigurationPart {
        @Transient
        val timeFormatterP = DateTimeFormatter.ofPattern(timeFormatter)!!
    }
}