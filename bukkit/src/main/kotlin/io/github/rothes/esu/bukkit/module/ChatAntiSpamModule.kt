package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.command.parser.UserParser
import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners
import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners.notifyUsers
import io.github.rothes.esu.bukkit.module.chatantispam.ChecksMan
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.user
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.displayName_
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.serializer.MapSerializer.DefaultedEnumMap
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.parsed
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import org.bukkit.Bukkit
import org.incendo.cloud.component.DefaultValue
import java.time.Duration
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object ChatAntiSpamModule: BukkitModule<ChatAntiSpamModule.ModuleConfig, ChatAntiSpamModule.ModuleLocale>() {

    private var purgeTask: ScheduledTask? = null

    override fun onEnable() {
        CasDataManager
        purgeTask = Scheduler.asyncTicks(20, 5 * 60 * 20) { CasDataManager.purgeCache(true) }
        CasListeners.enable()
        Bukkit.getOnlinePlayers().map { it.user }.forEach {
            if (it.hasPerm("notify"))
                notifyUsers.add(it)
        }
        val cmd = plugin.commandManager.commandBuilder("antispam", "as").permission(perm("command.admin"))
        registerCommandJvm {
            cmd.literal("data").optional(
                "player", UserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, UserParser()
            ).handler { context ->
                val sender = context.sender()
                val playerUser = context.get<PlayerUser>("player")
                val spamData = CasDataManager.getCache(playerUser)
                if (spamData == null) {
                    sender.message(lang, { command.data.noData }, user(playerUser), sender.msgPrefix)
                } else {
                    sender.message(lang, { command.data.data },
                        user(playerUser), unparsed("debug-data", spamData),
                        sender.msgPrefix,
                        unparsed("filtered-size", spamData.filtered.size),
                        unparsed("requested-size", spamData.requests.size),
                        unparsed("requested-size", spamData.requests.size),
                        unparsed("spam-score", spamData.scores.takeLast(config.muteHandler.spamScore.calculateSize)
                            .sumOf { it.score } / config.muteHandler.spamScore.calculateSize),
                        parsed("mute-duration",
                            if (spamData.muteUntil > System.currentTimeMillis())
                                (spamData.muteUntil - System.currentTimeMillis()).milliseconds
                            else
                                sender.localed(lang) { command.data.notMuted }
                        )
                    )
                }
            }
        }
        registerCommandJvm {
            cmd.literal("notify").handler { context ->
                val user = context.sender()
                if (notifyUsers.contains(user)) {
                    notifyUsers.remove(user)
                    user.message(lang, { command.notify.disabled }, user.msgPrefix)
                } else {
                    notifyUsers.add(user)
                    user.message(lang, { command.notify.enabled }, user.msgPrefix)
                }
            }
        }
        registerCommandJvm {
            cmd.literal("mute").optional(
                "player", UserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, UserParser()
            ).handler { context ->
                val playerUser = context.get<PlayerUser>("player")
                val duration = playerUser.spamData.mute()
                context.sender().message(lang, { command.mute.mutedPlayer },
                    context.sender().msgPrefix,
                    user(playerUser),
                    duration(duration.milliseconds, playerUser),
                )
                CasDataManager.saveSpamDataAsync(playerUser)
            }
        }
        registerCommandJvm {
            cmd.literal("reset").optional(
                "player", UserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, UserParser()
            ).handler { context ->
                val playerUser = context.get<PlayerUser>("player")
                CasDataManager.deleteAsync(playerUser.dbId)
                CasDataManager.getHolder(playerUser).spamData = SpamData()
                context.sender().message(lang, { command.reset.resetPlayer },
                    context.sender().msgPrefix,
                    component("player", playerUser.player.displayName_)
                )
            }
        }
        // Try save one data
        Bukkit.getOnlinePlayers().map { it.user }.firstOrNull { it.logonBefore }?.let {
            CasDataManager.saveSpamDataAsync(it)
        }
    }

    override fun onDisable() {
        super.onDisable()
        purgeTask?.cancel()
        purgeTask = null
        CasListeners.disable()
        try {
            CasDataManager.purgeCache(false)
        } catch (e: NoClassDefFoundError) {
            // Ehh.. Plugin Jar got deleted?
            plugin.err("Failed to purge cache while disabling module $name", e)
        }
        Bukkit.getOnlinePlayers().forEach { CasDataManager.saveSpamDataNow(it.user) }
    }

    override fun onReload() {
        super.onReload()
        if (config.notifyConsole) {
            notifyUsers.add(ConsoleUser)
        } else {
            notifyUsers.remove(ConsoleUser)
        }
    }

    val PlayerUser.spamData
        get() = CasDataManager[this].also { it.lastAccess = max(it.lastAccess, System.currentTimeMillis()) }

    val PlayerUser.addr: String
        get() = player.address!!.hostString

    val User.msgPrefix: TagResolver
        get() = parsed("prefix", localed(lang) { this.prefix })

    data class ModuleConfig(
        @Comment("Enable to notify console anti-spam messages")
        val notifyConsole: Boolean = true,
        val userDataExpiresAfter: Duration = 20.minutes.toJavaDuration(),
        val baseMuteDuration: Duration = 10.minutes.toJavaDuration(),
        val expireSize: ExpireSize = ExpireSize(),
        val expireTime: ExpireTime = ExpireTime(),
        @Comment("""
            Remove one oldest filtered record if the player sends multiple unfiltered chat message in a row.
            Set to non-positive value to disable this, and record only expires after configured time above.
        """)
        val consecutiveUnfilteredThreshold: Int = 3,
        val muteHandler: MuteHandler = MuteHandler(),
        val spamCheck: DefaultedEnumMap<MessageType, SpamCheck> =
            DefaultedEnumMap(MessageType::class.java, SpamCheck()).apply {
                MessageType.entries.forEach { put(it, SpamCheck()) }
            },
        val whisperTargets: WhisperTargets = WhisperTargets()
    ): BaseModuleConfiguration() {

        data class ExpireSize(
            val chatRequest: Int = 10,
            val filtered: Int = -1,
            val messageRecord: Int = 30,
            val whisperTarget: Int = -1,
            val score: Int = 10,
        ): ConfigurationPart

        data class ExpireTime(
            val chatRequest: Duration = 6.minutes.toJavaDuration(),
            val filtered: Duration = 4.5.minutes.toJavaDuration(),
            @Comment("Using base value(hard) plus quadratic function to check expired with a rate.\n" +
                    "Default: 60 + 840 seconds(15m in total) to fully expire")
            val messageRecord: ExpireCurve = ExpireCurve(),
            val whisperTarget: Duration = 4.minutes.toJavaDuration(),
            val score: Long = -1,
        ): ConfigurationPart {
            data class ExpireCurve(
                val hardExpireTime: Duration = 60.seconds.toJavaDuration(),
                val quadraticDividerOffset: Double = 60.0 * 1000,
                val quadraticDivider: Double = 90.0 * 1000,
                val quadraticHeight: Double = 100.0,
            ): ConfigurationPart {
                fun rate(elapsed: Long): Double {
                    if (hardExpireTime.toMillis() > elapsed) {
                        return 1.0
                    }
                    val x = elapsed - hardExpireTime.toMillis()
                    if (quadraticDividerOffset >= x) {
                        return 1.0
                    }
                    val y = -((x + quadraticDividerOffset) / quadraticDivider).pow(2) + quadraticHeight
                    return y / quadraticHeight
                }

                fun expired(elapsed: Long): Boolean {
                    return rate(elapsed) < 0
                }
            }
        }

        data class MuteHandler(
            val baseMuteDuration: Duration = 10.minutes.toJavaDuration(),
            val muteOnFilteredSize: Int = 8,
            val muteDurationMultiplier: MuteDurationMultiplier = MuteDurationMultiplier(),
            val spamScore: SpamScore = SpamScore(),
            val keepMessageRecords: Boolean = true,
            val keepScores: Boolean = true,
        ): ConfigurationPart {

            data class SpamScore(
                val calculateSize: Int = 6,
                val muteOnAverageScore: Double = 0.5625,
                @Comment("Make sure the player get muted can be scored in this range after unmute")
                val safeScoreOnMute: Double = 0.3,
            ): ConfigurationPart

            data class MuteDurationMultiplier(
                @Comment("Last Mute must within this interval to trigger a multiplier")
                val maxMuteInterval: Duration = 15.minutes.toJavaDuration(),
                val multiplier: Double = 2.0,
            ): ConfigurationPart
        }

        data class SpamCheck(
            val notifyFiltered: Boolean = true,
            @Comment("""
                Efficient to "Spam" module with a fixed send rate
            """)
            val fixedRequestMute: FixedRequestMute = FixedRequestMute(),
            val frequency: Frequency = Frequency(),
            val illegalCharacters: IllegalCharacters = IllegalCharacters(),
            val letterCase: LetterCase = LetterCase(),
            val longMessage: LongMessage = LongMessage(),
            val randomCharacters: RandomCharacters = RandomCharacters(),
            val similarityCheck: SimilarityCheck = SimilarityCheck(),
            val spaces: Spaces = Spaces(),
        ): ConfigurationPart {

            data class FixedRequestMute(
                val enabled: Boolean = true,
                val conditions: List<Condition> = listOf(Condition()),
            ): ConfigurationPart {

                data class Condition(
                    val drift: Duration = 2.seconds.toJavaDuration(),
                    val minRequestInterval: Duration = 20.seconds.toJavaDuration(),
                    val samples: Int = 5,
                ): ConfigurationPart
            }

            data class Frequency(
                val maxMessages: Int = 8,
                val maxMessagesPer: Duration = 25.seconds.toJavaDuration(),
                val minimalInterval: Duration = 1.5.seconds.toJavaDuration(),
            ): ConfigurationPart

            data class IllegalCharacters(
                val enabled: Boolean = true
            ): ConfigurationPart

            data class LetterCase(
                val uniformOnCheck: Boolean = true
            ): ConfigurationPart

            data class LongMessage(
                val maxMessageSize: Int = 144
            ): ConfigurationPart

            data class RandomCharacters(
                val removeRandomCharactersOnCheck: Boolean = true
            ): ConfigurationPart

            data class SimilarityCheck(
                val blockOnDisallowCount: LinkedHashMap<Int, Int> = LinkedHashMap<Int, Int>().apply {
                    put(6, 6)
                    put(14, 4)
                    put(36, 2)
                    put(32767, 1)
                },
                val allowRateReducePerRecord: Double = 0.015,
                val baseAllowRate: Double = 0.80,
                val lowestAllowRate: Double = 0.55,
            ): ConfigurationPart

            data class Spaces(
                val removeExtraSpacesOnCheck: Boolean = true,
                val minLength: Int = 9,
                val spaceRate: Double = 0.333,
            ): ConfigurationPart
        }

        data class WhisperTargets(
            val maxTargets: Int = 10,
            val safeTargets: Int = 2,
            val safeTargetsMax: Int = 6,
        ): ConfigurationPart
    }

    data class ModuleLocale(
        val prefix: String = "<sc><b>AS </b><pdc>» ",
        val notify: Notify = Notify(),
        val command: Command = Command(),
        val blockedMessage: MutableMap<String, MessageData> = linkedMapOf(),
    ): ConfigurationPart {

        data class Notify(
            val filtered: String = "<prefix><pc><player><tc>: <sdc><message> <tc>filtered " +
                    "(<pdc><check-type> <chat-type></pdc>)",
            val muted: String = "<prefix><pc><player> <tc>has been muted " +
                    "(<pdc><duration>, <multiplier>x</pdc>)"
        ): ConfigurationPart

        data class Command(
            val data: Data = Data(),
            val notify: Notify = Notify(),
            val mute: Mute = Mute(),
            val reset: Reset = Reset(),
        ): ConfigurationPart {

            data class Data(
                val noData: MessageData = "<prefix><pc>Player <pdc><player> <pc>has no chat data.".message,
                val data: MessageData = """
                    |<prefix><pc>Data of player <pdc><player> :
                    |<tdc><debug-data>
                    |<sc>Filters: <sdc><filtered-size>
                    |<pc>Requests: <pdc><requested-size>
                    |<sc>Score: <sdc><spam-score>
                    |<pc>Mute duration: <pdc><mute-duration>
                """.trimMargin().message,
                val notMuted: String = "Not muted",
            ): ConfigurationPart

            data class Notify(
                val enabled: MessageData = "<prefix><vpc>Receiving spam notify now.".message,
                val disabled: MessageData = "<prefix><vnc>Rejecting spam notify now.".message,
            ): ConfigurationPart

            data class Mute(
                val mutedPlayer: MessageData = "<prefix><pc>Muted <pdc><player></pdc> for <pdc><duration></pdc>.".message,
            ): ConfigurationPart

            data class Reset(
                val resetPlayer: MessageData = "<prefix><pc>Reset data for player <pdc><player></pdc>.".message,
            ): ConfigurationPart

        }

        @PostProcess
        private fun postProcess() {
            for (check in ChecksMan.checks) {
                check.defaultBlockedMessage?.let {
                    blockedMessage.putIfAbsent(check.type, it)
                }
            }
        }

    }

}