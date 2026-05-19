/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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

object ItemEditModule: BukkitModule<BaseModuleConfiguration, ItemEditModule.ModuleLang>() {

    override fun onEnable() {
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
                message(lang, { playerOnlyCommand })
                return null
            }
            val hand = player.inventory.itemInMainHand
            if (hand.type == Material.AIR) {
                message(lang, { itemInHandRequired })
                return null
            }
            return hand
        }
    private fun User.cleared(attribute: String) {
        message(lang, { attributeCleared }, unparsed("attribute", attribute))
    }
    private fun User.set(attribute: String, value: Any) {
        message(lang, { attributeSet }, unparsed("attribute", attribute), unparsed("value", value))
    }

    data class ModuleLang(
        val playerOnlyCommand: MessageData = "<ec>Only players can use this command.".message,
        val itemInHandRequired: MessageData = "<ec>A item must be hold in your main hand.".message,

        val attributeCleared: MessageData = "<pc>The <pdc><attribute></pdc> is cleared.".message,
        val attributeSet: MessageData = "<pc>The <pdc><attribute></pdc> has been set to <pdc><value></pdc>.".message,
    ): ConfigurationPart
}