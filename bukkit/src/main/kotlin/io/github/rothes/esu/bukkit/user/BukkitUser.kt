package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.inventory.Book
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

    override fun title(title: ParsedMessageData.ParsedTitleData) {
        val mainTitle = title.title
        val subTitle = title.subTitle
        val times = title.times

        if (mainTitle != null && subTitle != null) {
            commandSender.showTitle(Title.title(mainTitle, subTitle, times?.adventure))
        } else {
            if (times != null) {
                commandSender.sendTitlePart(TitlePart.TIMES, times.adventure)
            }
            if (mainTitle != null) {
                commandSender.sendTitlePart(TitlePart.TITLE, mainTitle)
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
        return item(itemData, *params)
    }

    fun item(itemData: ItemData, vararg params: TagResolver): ItemStack {
        val item = itemData.create

        item.editMeta { meta ->
            itemData.displayName?.let {
                meta.displayName(buildMinimessage(it, params = params))
            }
            itemData.lore?.let { lore ->
                val built = lore.map { buildMinimessage(it, params = params) }
                val list = arrayListOf<Component>()
                built.forEach { component ->
                    val serialize = MiniMessage.miniMessage().serialize(component)
                    if (serialize.contains('\n') || serialize.contains("<br>")) {
                        list.addAll(serialize.split("<br>", "\n").map { MiniMessage.miniMessage().deserialize(it) })
                    } else {
                        list.add(component)
                    }
                }
                meta.lore(list)
            }
        }
        return item
    }

    fun openBook(book: Book.Builder) {
        openBook(book.build())
    }

    fun openBook(book: Book) {
        commandSender.openBook(book)
    }

}