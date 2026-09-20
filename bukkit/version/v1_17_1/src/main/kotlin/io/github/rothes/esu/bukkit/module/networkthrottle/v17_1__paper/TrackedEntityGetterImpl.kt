package io.github.rothes.esu.bukkit.module.networkthrottle.v17_1__paper

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval.TrackedEntityGetter
import net.minecraft.server.level.ChunkMap
import net.minecraft.world.entity.Entity

object TrackedEntityGetterImpl : TrackedEntityGetter {

    override fun getTrackedEntity(entity: Entity): ChunkMap.TrackedEntity? {
        return entity.tracker
    }

}