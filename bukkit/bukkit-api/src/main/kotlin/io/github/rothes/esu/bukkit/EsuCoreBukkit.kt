package io.github.rothes.esu.bukkit

import io.github.rothes.esu.core.EsuCore

interface EsuCoreBukkit: EsuCore {

    val enabledHot: Boolean
    val disabledHot: Boolean

    val isEnabled: Boolean
        get() = plugin.isEnabled

}