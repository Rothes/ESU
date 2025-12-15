package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.kotlin.extension.commandBuilder
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.parser.standard.FloatParser
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.ConcurrentHashMap

object Speed : BaseCommand<Speed.Config, Speed.Lang>() {

    init {
        registerFeature(WalkSpeed)
        registerFeature(FlySpeed)
    }

    private val speedHandler by Versioned(NmsPlayerSpeedHandler::class.java)

    override fun onEnable() {
        registerSpeedCommands()
    }

    private fun BaseCommand<*, *>.registerSpeedCommands() {
        val handlerGetter: (Player) -> SpeedCommand<*> =
            if (this is SpeedCommand<*>) {
                { this }
            } else
                { player -> if (player.isFlying) FlySpeed else WalkSpeed }
        withCommandManager {
            commandBuilder(name + "Get") {
                permission(this@Speed.cmdShortPerm())
                handler { ctx ->
                    val sender = ctx.sender()
                    val player = ctx.getOrSupplyDefault("player") { (sender as PlayerUser).player }
                    handlerGetter(player).getSpeed(sender, player)
                }.regCmd()

                permission(this@Speed.cmdShortPerm("others"))
                optional("player", PlayerParser.playerParser())
                regCmd()
            }
            commandBuilder(name) {
                permission(this@Speed.cmdShortPerm())

                handler { ctx ->
                    val sender = ctx.sender()
                    val player = ctx.getOrSupplyDefault("player") { (sender as PlayerUser).player }
                    val speedCommand = handlerGetter(player)
                    val value = ctx.getOrNull("value") ?: speedCommand.defaultValue
                    speedCommand.setSpeed(sender, value, player)
                }

                fun argType(scope: MutableCommandBuilder<User>.() -> Unit) {
                    copy {
                        scope()
                        regCmd()

                        permission(this@Speed.cmdShortPerm("others"))
                        optional("player", PlayerParser.playerParser())
                        regCmd()
                    }
                }

                argType {
                    literal("reset")
                }
                argType {
                    required("value", FloatParser.floatParser()) {
                        suggestionProvider(SuggestionProvider.blockingStrings { ctx, _ ->
                            val sender = ctx.sender()
                            val player = if (sender is PlayerUser) sender.player else null
                            val current =
                                if (player != null) with(handlerGetter(player)) { player.speed }
                                else if (this@registerSpeedCommands is SpeedCommand<*>) this@registerSpeedCommands.defaultValue
                                else 0.1f
                            listOf(current.toString())
                        })
                    }
                }
            }
        }
    }

    data class Config(
        @Comment("""
            Reset speed for players leaving the server or on disable.
            If set to false, modified speed may exist forever.
        """)
        val autoResetSpeed: Boolean = true,
    ): BaseFeatureConfiguration(true)

    data class Lang(
        val speedGet: MessageData = "<pc><capitalize:'<type>'> speed of <pdc><player><pc> is <pdc><value><pc>.".message,
        val speedSet: MessageData = "<pc>Set <type> speed of <pdc><player><pc> to <pdc><value><pc>.".message,
    )

    interface NmsPlayerSpeedHandler {
        fun getWalkSpeed(bukkitPlayer: Player): Float
        fun getFlySpeed(bukkitPlayer: Player): Float
        fun setWalkSpeed(bukkitPlayer: Player, speed: Float)
        fun setFlySpeed(bukkitPlayer: Player, speed: Float)
    }

    abstract class SpeedCommand<L: SpeedType> : BaseCommand<Unit, L>() {

        abstract var Player.speed: Float
        abstract val defaultValue: Float
        private val changed = ConcurrentHashMap.newKeySet<Player>()
        private val listener = Listeners()

        fun getSpeed(sender: User, player: Player) {
            sender.message(
                Speed.lang, { speedGet }, player(player), unparsed("value", player.speed),
                component("type", sender.buildMiniMessage(lang, { typeName })),
            )
        }

        fun setSpeed(sender: User, speed: Float, player: Player) {
            player.speed = speed
            sender.message(
                Speed.lang, { speedSet }, player(player), unparsed("value", speed),
                component("type", sender.buildMiniMessage(lang, { typeName })),
            )
            changed.add(player)
        }

        override fun onEnable() {
            registerSpeedCommands()
            listener.register()
        }

        override fun onDisable() {
            super.onDisable()
            listener.unregister()
            if (Speed.config.autoResetSpeed) {
                for (player in changed) {
                    player.speed = defaultValue
                }
            }
            changed.clear()
        }

        inner class Listeners : Listener {
            @EventHandler
            fun onQuit(event: PlayerQuitEvent) {
                if (changed.remove(event.player) && Speed.config.autoResetSpeed)
                    event.player.speed = defaultValue
            }
        }
    }

    abstract class SpeedType(val typeName: String) : ConfigurationPart

    object WalkSpeed : SpeedCommand<WalkSpeed.WalkSpeedType>() {

        override var Player.speed: Float
            get() = speedHandler.getWalkSpeed(this)
            set(value) { speedHandler.setWalkSpeed(this, value) }
        override val defaultValue: Float = 0.1f

        class WalkSpeedType: SpeedType("walk")

    }

    object FlySpeed : SpeedCommand<FlySpeed.FlySpeedType>() {

        override var Player.speed: Float
            get() = speedHandler.getFlySpeed(this)
            set(value) { speedHandler.setFlySpeed(this, value) }
        override val defaultValue: Float = 0.05f

        class FlySpeedType: SpeedType("fly")

    }

}