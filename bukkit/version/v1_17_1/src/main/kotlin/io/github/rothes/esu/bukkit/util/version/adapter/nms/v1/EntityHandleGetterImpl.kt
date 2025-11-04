package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity

class EntityHandleGetterImpl: EntityHandleGetter {

    override fun getHandle(entity: org.bukkit.entity.Entity): Entity {
        return (entity as CraftEntity).handle
    }

}