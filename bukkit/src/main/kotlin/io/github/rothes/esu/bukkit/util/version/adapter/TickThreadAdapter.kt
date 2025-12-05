package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

interface TickThreadAdapter {

    fun isOwnedByCurrentRegion(entity: Entity): Boolean = Bukkit.isPrimaryThread()
    fun isOwnedByCurrentRegion(location: Location): Boolean = Bukkit.isPrimaryThread()

    companion object {

        val instance = if (ServerCompatibility.isFolia) Folia else CB

        fun Entity.checkTickThread(): Boolean = instance.isOwnedByCurrentRegion(this)
        fun Location.checkTickThread(): Boolean = instance.isOwnedByCurrentRegion(this)

    }

    private object CB: TickThreadAdapter

    private object Folia: TickThreadAdapter {

        override fun isOwnedByCurrentRegion(entity: Entity): Boolean {
            return Bukkit.isOwnedByCurrentRegion(entity)
        }

        override fun isOwnedByCurrentRegion(location: Location): Boolean {
            return Bukkit.isOwnedByCurrentRegion(location)
        }

    }

}