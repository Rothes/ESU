package io.github.rothes.esu.bukkit.module.networkthrottle.v21__paper

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity

object ApplyCommandImpl : EntityUpdateInterval.ApplyCommand() {

    val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").usIntAccessor

    override fun handleEntity(entity: Entity): Boolean {
        val tracker = entity.`moonrise$getTrackedEntity`() ?: return false
        val se = tracker.serverEntity
        SERVER_ENTITY_UPDATE_INTERVAL[se] = EntityUpdateInterval.INSTANCE.getCurrentInterval(entity.type)
        return true
    }

}