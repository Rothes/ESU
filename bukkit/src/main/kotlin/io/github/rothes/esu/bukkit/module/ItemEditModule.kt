package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotations.Command

object ItemEditModule: BukkitModule<BaseModuleConfiguration, ItemEditModule.ModuleLang>(
    BaseModuleConfiguration::class.java, ModuleLang::class.java
) {

    override fun enable() {
        registerCommands(object {
            @Command("itemEdit customModelData clear")
            fun customModelData(sender: User) {
                val item = sender.hand ?: return
                item.meta { meta ->
                    meta.setCustomModelData(null)
                }
                sender.cleared("customModelData")
            }

            @Command("itemEdit customModelData <value>")
            fun customModelData(sender: User, value: Int) {
                val item = sender.hand ?: return
                item.meta { meta ->
                    meta.setCustomModelData(value)
                }
                sender.set("customModelData", value)
            }

        }) {
            it.registerBuilderModifier(Command::class.java) { _, builder -> builder.permission(perm("command")) }
        }
    }

    private val User.hand: ItemStack?
        get() {
            if (this !is PlayerUser) {
                message(locale, { playerOnlyCommand })
                return null
            }
            val hand = player.inventory.itemInMainHand
            if (hand.type == Material.AIR) {
                message(locale, { itemInHandRequired })
                return null
            }
            return hand
        }
    private fun User.cleared(attribute: String) {
        message(locale, { attributeCleared }, unparsed("attribute", attribute))
    }
    private fun User.set(attribute: String, value: Any) {
        message(locale, { attributeSet }, unparsed("attribute", attribute), unparsed("value", value))
    }

    data class ModuleLang(
        val playerOnlyCommand: MessageData = "<ec>Only players can use this command.".message,
        val itemInHandRequired: MessageData = "<ec>A item must be hold in your main hand.".message,

        val attributeCleared: MessageData = "<pc>The <pdc><attribute></pdc> is cleared.".message,
        val attributeSet: MessageData = "<pc>The <pdc><attribute></pdc> has been set to <pdc><value></pdc>.".message,
    ): ConfigurationPart
}