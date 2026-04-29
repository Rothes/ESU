package io.github.rothes.esu.bukkit.module.vanillatweaks

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.HangingSign
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

object SignBlock: CommonFeature<Unit, Unit>() {

    init {
        registerFeature(DisableEditingSignText)
    }

    override fun onEnable() {}

    object DisableEditingSignText: CommonFeature<DisableEditingSignText.FeatureConfig, Unit>() {

        override fun checkUnavailable(): Feature.AvailableCheck? {
            return super.checkUnavailable() ?: let {
                if (!ServerCompatibility.isPaper)
                    return Feature.AvailableCheck.fail { "Requires Paper".message }
                null
            }
        }

        override fun onEnable() {
            Listeners.register()
        }

        override fun onDisable() {
            super.onDisable()
            Listeners.unregister()
        }

        private object Listeners : Listener {

            @EventHandler(ignoreCancelled = true)
            fun onOpenSign(e: PlayerOpenSignEvent) {
                val sign = e.sign
                fun handleBlocked() {
                    e.isCancelled = true
                    val sound =
                        if (sign is HangingSign) Sound.BLOCK_HANGING_SIGN_WAXED_INTERACT_FAIL else Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL
                    sign.world.playSound(sign.block.location, sound, SoundCategory.BLOCKS, 1f, 1f)
                }

                val lines = sign.getSide(e.side).lines()
                if (config.allowEditingEmptyLines) {
                    if (lines.all { it.notEmpty() }) handleBlocked()
                } else {
                    if (lines.any { it.notEmpty() }) handleBlocked()
                }
            }

            @EventHandler
            fun onSignEdit(e: SignChangeEvent) {
                val block = e.block.state as Sign
                val oldLines = block.getSide(e.side).lines()
                val newLines = e.lines()
                for ((index, component) in oldLines.withIndex()) {
                    if (component.notEmpty()) {
                        newLines[index] = component
                    }
                }
            }

//        @EventHandler
//        fun onEdit(e: PlayerInteractEvent) {
//            if (e.action != Action.RIGHT_CLICK_BLOCK) return
//            val block = e.clickedBlock ?: return
//            val item = e.item ?: return
//            item as CraftItemStack
//            val handle = item.handle
//            if (handle.item is SignApplicator) return
//
//            val sign = block.state as? Sign ?: return
//
//        }

            private fun Component.notEmpty() = this != Component.empty()

        }


        data class FeatureConfig(
            val allowEditingEmptyLines: Boolean = true,
        ) : BaseFeatureConfiguration()
    }

}