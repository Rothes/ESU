package io.github.rothes.esu.bukkit.module.networkthrottle.v26_3

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.ReflectionUtils.setter
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.UpdateInterval

object TrackedEntityIntervalUpdaterImpl : EntityUpdateInterval.TrackedEntityIntervalUpdater() {

    val TRACKED_ENTITY_SERVER_ENTITY = ChunkMap.TrackedEntity::class.java.getDeclaredField("serverEntity").usObjAccessor
    val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").setter

    override fun handleEntity(entity: Entity, tracker: ChunkMap.TrackedEntity) {
        val se = TRACKED_ENTITY_SERVER_ENTITY[tracker] as ServerEntity
        val entityType = entity.type
        val interval = if (entityType.hasUpdateInterval()) UpdateInterval.periodic(entityType.updateInterval()) else UpdateInterval.NEVER
        SERVER_ENTITY_UPDATE_INTERVAL.invokeExact(se, interval)
    }

}