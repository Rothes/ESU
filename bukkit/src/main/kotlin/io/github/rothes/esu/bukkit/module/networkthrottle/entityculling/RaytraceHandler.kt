package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import org.bukkit.entity.Entity

abstract class RaytraceHandler<C: ConfigurationPart, L: ConfigurationPart>: CommonFeature<C, L>() {

    override val name: String = "RaytraceHandler"

    abstract fun checkConfig(): Feature.AvailableCheck?

    abstract fun onEntityRemove(entity: Entity)

    abstract fun isValid(bukkitEntity: Entity): Boolean

}