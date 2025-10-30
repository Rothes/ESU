package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonFeature
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

abstract class RaytraceHandler<C: ConfigurationPart, L: ConfigurationPart>: CommonFeature<C, L>() {

    override val name: String = "RaytraceHandler"

    abstract fun updatePlayer(bukkitPlayer: Player, userCullData: UserCullData)

    abstract fun raytrace(from: Vector, to: Vector, world: World): Boolean

    abstract fun getEntityId(entity: Entity): Int

}