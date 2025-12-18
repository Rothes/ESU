package io.github.rothes.esu.bukkit.util.version.adapter.nms

import org.bukkit.entity.Entity

interface EntityValidTester {

    operator fun get(bukkitEntity: Entity): Boolean = isValid(bukkitEntity)

    fun isValid(bukkitEntity: Entity): Boolean

}