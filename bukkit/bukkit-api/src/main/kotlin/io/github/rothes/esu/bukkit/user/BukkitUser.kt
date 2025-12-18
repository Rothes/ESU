package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.bukkit.audience
import io.github.rothes.esu.bukkit.configuration.data.ItemData
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

abstract class BukkitUser: User {

    private var _audience: Audience? = null
    private var _tagResolvers: List<TagResolver>? = null

    abstract override val commandSender: CommandSender
    override val audience: Audience
        get() = _audience ?: commandSender.audience.also { _audience = it }

    override fun getTagResolvers(): Iterable<TagResolver> {
        return _tagResolvers ?: User.DEFAULT_TAG_RESOLVERS.plus(ComponentBukkitUtils.papi(this)).also { _tagResolvers = it }
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

    fun <T: ConfigurationPart> item(locales: MultiLangConfiguration<T>, block: T.() -> ItemData?, vararg params: TagResolver): ItemStack {
        val itemData = localedOrNull(locales, block) ?: throw NullPointerException()
        return item(itemData, *params)
    }

    fun item(itemData: ItemData, vararg params: TagResolver): ItemStack {
        return itemData.parsed(this, *params)
    }

}