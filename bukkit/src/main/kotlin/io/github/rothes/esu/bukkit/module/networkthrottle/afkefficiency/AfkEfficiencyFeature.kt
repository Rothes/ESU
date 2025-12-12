package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency
import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency.PlayerHolder
import io.github.rothes.esu.core.module.CommonFeature

abstract class AfkEfficiencyFeature<C, L>: CommonFeature<C, L>() {

    override val name: String = javaClass.simpleName.removeSuffix("Efficiency")

    override fun onEnable() {
        for (holder in AfkEfficiency.efficiencyPlayers) {
            if (holder.inAfk)
                onEnableEfficiency(holder)
        }
    }

    abstract fun onEnableEfficiency(playerHolder: PlayerHolder)
    abstract fun onDisableEfficiency(playerHolder: PlayerHolder)

}