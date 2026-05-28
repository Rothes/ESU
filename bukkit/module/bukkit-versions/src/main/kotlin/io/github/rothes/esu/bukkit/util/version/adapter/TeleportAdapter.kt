package io.github.rothes.esu.bukkit.util.version.adapter

import org.bukkit.Location
import org.bukkit.entity.Entity

interface TeleportAdapter {

    fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)? = null)

    companion object {

        val instance = try {
            Entity::class.java.getMethod("teleportAsync", Location::class.java)
            ASync
        } catch (_: NoSuchMethodException) {
            Sync
        }

        fun Entity.tp(location: Location, then: ((Boolean) -> Unit)? = null) {
            instance.teleport(this, location, then)
        }

    }

    private object ASync : TeleportAdapter {

        override fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)?) {
            val future = entity.teleportAsync(location)
            if (then != null) {
                future.thenAccept(then)
            }
        }
    }

    private object Sync : TeleportAdapter {

        override fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)?) {
            val success = entity.teleport(location)
            if (then != null) {
                then(success)
            }
        }
    }


}