package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.world.entity.Entity
import org.bukkit.entity.Entity as BukkitEntity

interface EntityValidTester {

    operator fun get(bukkitEntity: BukkitEntity): Boolean = isValid(bukkitEntity)

    fun isValid(bukkitEntity: BukkitEntity): Boolean
    fun isValid(entity: Entity): Boolean

}