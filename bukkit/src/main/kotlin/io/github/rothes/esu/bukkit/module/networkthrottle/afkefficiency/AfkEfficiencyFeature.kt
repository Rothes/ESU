/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.networkthrottle.afkefficiency

import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency
import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency.PlayerHolder
import io.github.rothes.esu.bukkit.module.networkthrottle.AfkEfficiency.isInAfkEfficiency
import io.github.rothes.esu.core.module.CommonFeature
import org.bukkit.entity.Player

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

    fun Player.inAfk(): Boolean = isInAfkEfficiency()

}