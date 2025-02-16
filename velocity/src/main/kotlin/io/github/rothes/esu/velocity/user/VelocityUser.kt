package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart

abstract class VelocityUser: User {

    abstract val commandSender: CommandSource

    override var language: String?
        get() = languageUnsafe ?: clientLocale
        set(value) {
            languageUnsafe = value
        }
    override var colorScheme: String?
        get() = colorSchemeUnsafe
        set(value) {
            colorSchemeUnsafe = value
        }

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    override fun message(message: Component) {
        commandSender.sendMessage(message)
    }

    override fun actionBar(message: Component) {
        commandSender.sendActionBar(message)
    }

    override fun title(parsed: ParsedMessageData.ParsedTitleData) {
        val title = parsed.title
        val subTitle = parsed.subTitle
        val times = parsed.times

        if (title != null && subTitle != null) {
            commandSender.showTitle(Title.title(title, subTitle, times?.adventure))
        } else {
            if (times != null) {
                commandSender.sendTitlePart(TitlePart.TIMES, times.adventure)
            }
            if (title != null) {
                commandSender.sendTitlePart(TitlePart.TITLE, title)
            }
            if (subTitle != null) {
                commandSender.sendTitlePart(TitlePart.SUBTITLE, subTitle)
            }
        }
    }

    override fun playSound(sound: SoundData) {
        commandSender.playSound(sound.adventure, Sound.Emitter.self())
    }

    override fun clearTitle() {
        commandSender.clearTitle()
    }

}