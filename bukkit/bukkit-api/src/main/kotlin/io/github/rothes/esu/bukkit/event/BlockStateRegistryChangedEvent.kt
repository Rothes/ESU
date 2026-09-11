package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.event.Nested.Companion.hasListener
import io.github.rothes.esu.bukkit.util.ServerInfo
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
class BlockStateRegistryChangedEvent(override val parentPriority: EventPriority) : Event(true), Nested {

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            if (ServerInfo.PluginEnabled.CraftEngine) {
                CraftEngineHook.register()
            }
        }

        private object CraftEngineHook {
            fun register() {
                Nested.registerNested(CraftEngineReloadEvent::class.java) { _, priority ->
                    if (handlers.hasListener(priority)) BlockStateRegistryChangedEvent(priority).callNested()
                }
            }
        }
    }

}