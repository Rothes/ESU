package io.github.rothes.esu.bukkit.module.networkthrottle.v17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object EntityUpdateIntervalImpl: EntityUpdateInterval() {

    private val ENTITY_TYPE_UPDATE_INTERVAL = EntityType::class.java.getDeclaredField("updateInterval").usIntAccessor

    override operator fun get(entityType: EntityType<*>): Int {
        return ENTITY_TYPE_UPDATE_INTERVAL[entityType]
    }

    override operator fun set(entityType: EntityType<*>, interval: Int) {
        ENTITY_TYPE_UPDATE_INTERVAL[entityType] = interval
    }

    object TrackedEntityIntervalUpdaterImpl : TrackedEntityIntervalUpdater() {

        val TRACKED_ENTITY_SERVER_ENTITY = ChunkMap.TrackedEntity::class.java.getDeclaredField("serverEntity").usObjAccessor
        val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").usIntAccessor

        override fun handleEntity(entity: Entity): Boolean {
            val tracker = entity.tracker ?: return false
            val se = TRACKED_ENTITY_SERVER_ENTITY[tracker] as ServerEntity
            SERVER_ENTITY_UPDATE_INTERVAL[se] = this@EntityUpdateIntervalImpl[entity.type]
            return true
        }

    }

}