package io.github.rothes.esu.bukkit.module.vanillatweaks

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.adapter.AnvilInvAdapter.Companion.renameText
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.extension.charSize
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent

object AnvilFeature : CommonFeature<Unit, Unit>() {

    init {
        registerFeature(Renaming)
    }

    override fun onEnable() {
    }

    object Renaming: CommonFeature<Renaming.FeatureConfig, Unit>() {

        override fun onEnable() {
            Listeners.register(plugin)
        }

        override fun onDisable() {
            super.onDisable()
            Listeners.unregister()
        }

        private object Listeners: Listener {

            @EventHandler(priority = EventPriority.HIGHEST)
            fun onAnvilRename(e: PrepareAnvilEvent) {
                val renameText = e.renameText ?: return
                val size = renameText.charSize()
                val limit = config.maxRenameCharSize
                if (limit in 0 ..< size) {
                    e.result = null
                }
            }
        }

        data class FeatureConfig(
            val maxRenameCharSize: Int = -1,
        ) : BaseFeatureConfiguration()
    }

}