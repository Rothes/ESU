package io.github.rothes.esu.bukkit.module.networkthrottle.v21__paper

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import net.minecraft.server.level.ChunkMap
import net.minecraft.world.entity.Entity

object TrackedEntityGetterImpl : EntityUpdateInterval.TrackedEntityGetter {

    override fun getTrackedEntity(entity: Entity): ChunkMap.TrackedEntity? {
        return entity.`moonrise$getTrackedEntity`()
    }

}