package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.version.Versioned
import org.bukkit.event.inventory.PrepareAnvilEvent

interface AnvilInvAdapter {

    fun getRenameText(e: PrepareAnvilEvent): String?

    companion object {

        val instance by Versioned(AnvilInvAdapter::class.java)

        val PrepareAnvilEvent.renameText: String?
            get() = instance.getRenameText(this)

    }

}