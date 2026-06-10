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

package io.github.rothes.esu.velocity.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import io.github.rothes.esu.velocity.config.VelocityEsuLang.VelocityLangData
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider

object VelocityEsuLang: EsuLang<VelocityLangData>() {

    init {
        instance = this
    }

    override fun load(): MultiLangConfiguration<VelocityLangData> = ConfigLoader.loadMulti(
        EsuCore.instance.baseConfigPath.resolve("lang"), "en_us"
    )

    class VelocityLangData: BaseEsuLangData() {

        @PostProcess
        fun fillCaptions() {
            StandardCaptionsProvider<Any>().keyMap.forEach { (k, v) ->
                commandCaptions.putIfAbsent(k, v)
            }
        }

        companion object {
            private val DelegatingCaptionProvider<*>.keyMap
                get() = (delegate() as ConstantCaptionProvider).captions().mapKeys { it.key }
        }
    }

}