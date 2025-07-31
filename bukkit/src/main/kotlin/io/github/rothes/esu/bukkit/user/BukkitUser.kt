package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.audience
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.displayNameV
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.loreV
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

abstract class BukkitUser: User {

    abstract val commandSender: CommandSender
    override val audience: Audience by lazy {
        commandSender.audience
    }

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

    fun <T: ConfigurationPart> item(locales: MultiLocaleConfiguration<T>, block: T.() -> ItemData?, vararg params: TagResolver): ItemStack {
        val itemData = localedOrNull(locales, block) ?: throw NullPointerException()
        return item(itemData, *params)
    }

    fun item(itemData: ItemData, vararg params: TagResolver): ItemStack {
        val item = itemData.create

        item.meta { meta ->
            itemData.displayName?.let {
                meta.displayNameV = buildMinimessage(it, params = params)
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
                meta.loreV = list
            }
        }
        return item
    }

}