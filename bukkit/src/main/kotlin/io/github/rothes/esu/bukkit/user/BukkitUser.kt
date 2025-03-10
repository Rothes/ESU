package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

abstract class BukkitUser: User {

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

    override val name: String
        get() = commandSender.name

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

    fun <T: ConfigurationPart> item(locales: MultiLocaleConfiguration<T>, block: T.() -> ItemData?, vararg params: TagResolver): ItemStack {
        val itemData = localedOrNull(locales, block) ?: throw NullPointerException()
        val item = itemData.getItem()

        item.editMeta { meta ->
            itemData.displayName?.let {
                meta.displayName(buildMinimessage(it, params = params))
            }
            itemData.lore?.let {
                val built = it.map { buildMinimessage(it, params = params) }
                val list = arrayListOf<Component>()
                built.forEach { component ->
                    val serialize = MiniMessage.miniMessage().serialize(component)
                    if (serialize.contains('\n')) {
                        list.addAll(serialize.split("\n").map { MiniMessage.miniMessage().deserialize(it) })
                    } else {
                        list.add(component)
                    }
                }
                meta.lore(list)
            }
        }
        return item
    }

}