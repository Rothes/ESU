package io.github.rothes.esu.bukkit.util.version.adapter.nms.v20_2__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity

object EntityHandleGetterImpl: EntityHandleGetter {

    override fun getHandle(entity: org.bukkit.entity.Entity): Entity {
        return (entity as CraftEntity).handleRaw
    }
}