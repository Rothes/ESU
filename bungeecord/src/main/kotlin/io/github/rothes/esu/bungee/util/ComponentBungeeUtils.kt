package io.github.rothes.esu.bungee.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.chat.ComponentSerializer

object ComponentBungeeUtils {

    val Component.bungee
        get() = ComponentSerializer.deserialize(GsonComponentSerializer.gson().serialize(this))

}