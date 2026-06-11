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

package io.github.rothes.esu.velocity.module.serverinfo

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.velocity.util.extension.VelocityListener
import io.github.rothes.esu.velocity.util.extension.register
import io.github.rothes.esu.velocity.util.extension.unregister
import java.util.*
import kotlin.jvm.optionals.getOrNull

object MotdFeature : CommonFeature<BaseFeatureConfiguration, Unit>() {

    init {
        registerFeature(Mod)
        registerFeature(Version)
    }

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    abstract class PingModifierFeature<T> : CommonFeature<T, Unit>() {

        override fun onEnable() {}
        abstract fun handlePing(ping: ServerPing.Builder)

    }

    private object Listeners : VelocityListener {

        @Subscribe
        fun onPing(event: ProxyPingEvent) {
            val ping = event.ping.asBuilder()
            for (feature in getFeatures()) {
                if (!feature.enabled || feature !is PingModifierFeature<*>) continue
                feature.handlePing(ping)
            }
            event.ping = ping.build()
        }

    }

    private object Mod : PingModifierFeature<Mod.ModConfig>() {

        override fun handlePing(ping: ServerPing.Builder) {
            config.modType.getOrNull()?.let { modType -> ping.modType(modType) }
        }

        data class ModConfig(
            val modType: Optional<String> = Optional.of("FML"),
        ): BaseFeatureConfiguration()

    }

    private object Version : PingModifierFeature<Version.ModConfig>() {

        override fun handlePing(ping: ServerPing.Builder) {
            val config = config
            ping.version(
                ServerPing.Version(
                    config.protocol.getOrNull() ?: ping.version.protocol,
                    config.name.getOrNull() ?: ping.version.name
                )
            )
        }

        data class ModConfig(
            val name: Optional<String> = Optional.of("Velocity Rebrand"),
            val protocol: Optional<Int> = Optional.empty(),
        ): BaseFeatureConfiguration()

    }

}