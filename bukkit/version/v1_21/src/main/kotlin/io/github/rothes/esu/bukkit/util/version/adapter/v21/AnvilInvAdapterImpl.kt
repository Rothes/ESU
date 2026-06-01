package io.github.rothes.esu.bukkit.util.version.adapter.v21

import io.github.rothes.esu.bukkit.util.version.adapter.AnvilInvAdapter
import org.bukkit.event.inventory.PrepareAnvilEvent

@Suppress("UnstableApiUsage")
object AnvilInvAdapterImpl: AnvilInvAdapter {

    override fun getRenameText(e: PrepareAnvilEvent): String? {
        return e.view.renameText
    }

}