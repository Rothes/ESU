package io.github.rothes.esu.bungee.user

import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.bungee.adventure
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.CommandSender

abstract class BungeeUser: User {

    abstract val commandSender: CommandSender

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
        adventure.sender(commandSender).sendMessage(message)
    }

    override fun actionBar(message: Component) {
        adventure.sender(commandSender).sendActionBar(message)
    }

    override fun title(title: ParsedMessageData.ParsedTitleData) {
        val mainTitle = title.title
        val subTitle = title.subTitle
        val times = title.times

        if (mainTitle != null && subTitle != null) {
            adventure.sender(commandSender).showTitle(Title.title(mainTitle, subTitle, times?.adventure))
        } else {
            if (times != null) {
                adventure.sender(commandSender).sendTitlePart(TitlePart.TIMES, times.adventure)
            }
            if (mainTitle != null) {
                adventure.sender(commandSender).sendTitlePart(TitlePart.TITLE, mainTitle)
            }
            if (subTitle != null) {
                adventure.sender(commandSender).sendTitlePart(TitlePart.SUBTITLE, subTitle)
            }
        }
    }

    override fun playSound(sound: SoundData) {
        adventure.sender(commandSender).playSound(sound.adventure, Sound.Emitter.self())
    }

    override fun clearTitle() {
        adventure.sender(commandSender).clearTitle()
    }

}