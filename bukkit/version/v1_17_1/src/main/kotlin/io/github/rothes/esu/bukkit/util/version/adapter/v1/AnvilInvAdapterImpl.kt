package io.github.rothes.esu.bukkit.util.version.adapter.v1

import io.github.rothes.esu.bukkit.util.version.adapter.AnvilInvAdapter
import org.bukkit.event.inventory.PrepareAnvilEvent

object AnvilInvAdapterImpl: AnvilInvAdapter {

    override fun getRenameText(e: PrepareAnvilEvent): String? {
        return e.inventory.renameText
    }

}