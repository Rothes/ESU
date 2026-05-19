/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.common.module

import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonModule
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.user.UserManager
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.time.toKotlinDuration

object AutoBroadcastModule: CommonModule<AutoBroadcastModule.ModuleConfig, AutoBroadcastModule.ModuleLang>() {

    private var scope: CoroutineScope? = null

    override fun onReload() {
        super.onReload()
        if (enabled)
            scheduleBroadcasts()
    }

    override fun onEnable() {
        scheduleBroadcasts()
    }

    override fun onDisable() {
        super.onDisable()
        scope?.cancel()
        scope = null
    }

    @Synchronized
    private fun scheduleBroadcasts() {
        scope?.cancel()
        val scope = CoroutineScope(Dispatchers.Default)
        this.scope = scope
        for (broadcast in config.broadcasts) {
            if (broadcast.messages.isEmpty()) continue
            scope.launch {
                var i = 0
                while (isActive) {
                    delay(broadcast.interval.toKotlinDuration())
                    val key = broadcast.messages[i++]
                    if (i >= broadcast.messages.size) {
                        i = 0
                    }
                    for (user in UserManager.instance.getUsers()) {
                        if (user.isOnline) // Safe for players just joined
                            user.message(user.localedOrNull(lang) { broadcastMessage[key] } ?: key.message)
                    }
                }
            }
        }
    }


    data class ModuleConfig(
        val broadcasts: List<Broadcast> = listOf(Broadcast()),
    ): BaseModuleConfiguration() {

        data class Broadcast(
            val interval: Duration = Duration.ofMinutes(1),
            val messages: List<String> = listOf("message-1")
        )
    }

    data class ModuleLang(
        val broadcastMessage: Map<String, MessageData> = linkedMapOf(
            Pair("message-1", "<pc>Welcome to my server! Message from ESU!<actionbar><pc>Welcome to my server!".message)
        ),
    )
}