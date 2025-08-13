package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.audience
import io.github.rothes.esu.bukkit.config.data.ItemData
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.displayName_
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.lore_
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.net.kyori.adventure.audience.Audience
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
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
        return itemData.parsed(this, *params)
    }

}