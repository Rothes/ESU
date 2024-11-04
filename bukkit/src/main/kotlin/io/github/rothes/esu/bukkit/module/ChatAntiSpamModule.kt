package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.command.parser.PlayerUserParser
import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners
import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners.notifyUsers
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.bukkit.module.chatantispam.user.CasDataManager
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.scheduler.ScheduledTask
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.serializer.MapSerializer.DefaultedEnumMap
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.description.Description
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

object ChatAntiSpamModule: CommonModule<ChatAntiSpamModule.ConfigData, EmptyConfiguration>(
    ConfigData::class.java, EmptyConfiguration::class.java
) {

    private var purgeTask: ScheduledTask? = null

    override fun enable() {
        purgeTask = Scheduler.async(20, 5 * 60 * 20) { purgeCache(true) }
        CasDataManager
        Bukkit.getPluginManager().registerEvents(CasListeners, plugin)
        Bukkit.getOnlinePlayers().map { it.user }.forEach {
            if (it.hasPerm("notify"))
                notifyUsers.add(it)
        }
        with(plugin.commandManager) {
            val cmd = commandBuilder("antispam", Description.of("Esu $name commands"), "as").permission(perm("command.admin"))
            command(
                cmd.literal("notify")
                    .handler { context ->
                        val user = context.sender()
                        if (notifyUsers.contains(user)) {
                            notifyUsers.remove(user)
                            user.message("§cRejecting spam notify")
                        } else {
                            notifyUsers.add(user)
                            user.message("§aReceiving spam notify")
                        }
                    }
            ).command(
                cmd.literal("reset")
                    .optional("player", PlayerUserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, PlayerUserParser())
                    .handler { context ->
                        val playerUser = context.get<PlayerUser>("player")
                        CasDataManager.deleteAsync(playerUser.dbId)
                        CasDataManager.cacheById.remove(playerUser.dbId)
                        CasDataManager.cacheByIp.remove(playerUser.addr)
                        context.sender().message("§aReset for player ${playerUser.name}")
                    }
            ).command(
                cmd.literal("data")
                    .optional("player", PlayerUserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, PlayerUserParser())
                    .handler { context ->
                        val playerUser = context.get<PlayerUser>("player")
                        context.sender().message(CasDataManager.cacheByIp[playerUser.player.address.hostString]?.let { spamData ->
                            """§aData of player ${playerUser.name} :
                            |  §3$spamData
                            |  §aFilters: ${spamData.filtered.size}
                            |  §2Requests: ${spamData.requests.size}
                            |  §aScore: ${spamData.scores.takeLast(config.muteHandler.spamScore.calculateSize).sumOf { it.score } / config.muteHandler.spamScore.calculateSize}
                            |  §2Mute duration: ${if (spamData.muteUntil > System.currentTimeMillis()) (spamData.muteUntil - System.currentTimeMillis()).milliseconds else "Not muted"}
                            """.trimMargin("|  ")
                        } ?: "§aPlayer ${playerUser.name} has no chat data")
                    }
            ).command(
                cmd.literal("mute")
                    .optional("player", PlayerUserParser.parser(), DefaultValue.dynamic { it.sender() as PlayerUser }, PlayerUserParser())
                    .handler { context ->
                        val playerUser = context.get<PlayerUser>("player")
                        val duration = playerUser.spamData.mute()
                        context.sender().message("§aMuted ${playerUser.name} for ${duration.milliseconds}")
                        CasDataManager.saveSpamDataAsync(playerUser)
                    }
            )
        }
        // Try save one data
        Bukkit.getOnlinePlayers().firstOrNull()?.let {
            CasDataManager.saveSpamDataAsync(it.user)
        }
    }

    override fun disable() {
        purgeTask?.cancel()
        purgeTask = null
        HandlerList.unregisterAll(CasListeners)
        try {
            purgeCache(false)
        } catch (e: NoClassDefFoundError) {
            // Ehh.. Plugin Jar got deleted?
            plugin.err("Failed to purge cache while disabling module $name", e)
        }
        Bukkit.getOnlinePlayers().forEach { CasDataManager.saveSpamData(it.user) }
    }

    override fun reloadConfig() {
        super.reloadConfig()
        if (config.notifyConsole) {
            notifyUsers.add(ConsoleUser)
        } else {
            notifyUsers.remove(ConsoleUser)
        }
    }

    private fun purgeCache(deleteDb: Boolean) {
        val time = System.currentTimeMillis()
        val handler = Consumer<MutableMap<*, SpamData>> { map ->
            map.entries
                .filter { time - it.value.lastAccess > config.userDataExpiresAfter }
                .forEach {
                    map.remove(it.key)
                    if (deleteDb)
                        CasDataManager.deleteAsync(it.key)
                }
        }
        handler.accept(CasDataManager.cacheById)
        handler.accept(CasDataManager.cacheByIp)
    }

    val PlayerUser.spamData
        get() = CasDataManager[this].also { it.lastAccess = max(it.lastAccess, System.currentTimeMillis()) }

    val PlayerUser.addr: String
        get() = player.address.hostString

    private const val SECOND: Long = 1000
    private const val MINUTE: Long = 60 * SECOND

    data class ConfigData(
        val notifyConsole: Boolean = true,
        val userDataExpiresAfter: Long = 20 * MINUTE,
        val baseMuteDuration: Long = 10 * MINUTE,
        val expireSize: ExpireSize = ExpireSize(),
        val expireTime: ExpireTime = ExpireTime(),
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
            val messageRecord: Int = -1,
            val whisperTarget: Int = -1,
            val score: Int = 10,
        ): ConfigurationPart

        data class ExpireTime(
            val chatRequest: Long = (6 * MINUTE).toLong(),
            val filtered: Long = (4.5 * MINUTE).toLong(),
            @field:Comment("Using base value(hard) plus quadratic function to check expired with a rate.\n" +
                    "Default: 60 + 840 seconds(15m in total) to fully expire")
            val messageRecord: ExpireCurve = ExpireCurve(),
            val whisperTarget: Long = (4 * MINUTE).toLong(),
            val score: Long = -1,
        ): ConfigurationPart {
            data class ExpireCurve(
                val hardExpireTime: Long = 60 * SECOND,
                val quadraticDividerOffset: Double = 60.0 * SECOND,
                val quadraticDivider: Double = 90.0 * SECOND,
                val quadraticHeight: Double = 100.0,
            ): ConfigurationPart {
                fun rate(elapsed: Long): Double {
                    if (hardExpireTime > elapsed) {
                        return 1.0
                    }
                    val x = elapsed - hardExpireTime
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
            val baseMuteDuration: Long = 10 * MINUTE,
            val muteOnFilteredSize: Int = 8,
            val muteDurationMultiplier: MuteDurationMultiplier = MuteDurationMultiplier(),
            val spamScore: SpamScore = SpamScore(),
            val keepMessageRecords: Boolean = true,
            val keepScores: Boolean = true,
        ): ConfigurationPart {

            data class SpamScore(
                val calculateSize: Int = 6,
                val muteOnAverageScore: Double = 0.5625,
                @field:Comment("Make sure the player get muted can be scored in this range after unmute")
                val safeScoreOnMute: Double = 0.3,
            ): ConfigurationPart

            data class MuteDurationMultiplier(
                @field:Comment("Last Mute must within this interval to trigger a multiplier")
                val maxMuteInterval: Long = 15 * MINUTE,
                val multiplier: Double = 2.0,
            ): ConfigurationPart
        }

        data class SpamCheck(
            val notifyFiltered: Boolean = true,
            @field:Comment("Efficient to \"Spam\" module with a fixed send rate")
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
                    val drift: Long = (2 * SECOND).toLong(),
                    val minRequestInterval: Long = 20 * SECOND,
                    val samples: Int = 5,
                ): ConfigurationPart
            }

            data class Frequency(
                val maxMessages: Int = 10,
                val maxMessagesPer: Long = 25 * SECOND,
                val minimalInterval: Long = 2 * SECOND,
            ): ConfigurationPart

            data class IllegalCharacters(
                val enabled: Boolean = true
            ): ConfigurationPart

            data class LetterCase(
                val uniformOnCheck: Boolean = true
            ): ConfigurationPart

            data class LongMessage(
                val maxMessageSize: Int = 56
            ): ConfigurationPart

            data class RandomCharacters(
                val removeRandomCharactersOnCheck: Boolean = true
            ): ConfigurationPart

            data class SimilarityCheck(
                val blockOnDisallowCount: LinkedHashMap<Int, Int> = LinkedHashMap<Int, Int>().apply {
                    put(6, 5)
                    put(14, 2)
                    put(32767, 1)
                },
                val allowRateReducePerRecord: Double = 0.015,
                val baseAllowRate: Double = 0.60,
                val lowestAllowRate: Double = 0.40,
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
        ): ConfigurationPart
    }

}