package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import org.bukkit.entity.Player
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object CullDataManager {

    private val map = mutableMapOf<Player, UserCullData>()
    private val lock = ReentrantReadWriteLock()

    operator fun get(player: Player): UserCullData {
        return lock.read { map[player] } ?: createData(player)
    }

    fun allData(): List<UserCullData> {
        return lock.read { map.values.toList() }
    }

    fun createData(player: Player): UserCullData {
        val userCullData = UserCullData(player)
        lock.write {
            map[player] = userCullData
        }
        return get(player)
    }

    fun remove(player: Player) {
        lock.write {
            map.remove(player)
        }
    }

    fun showAll() {
        lock.read {
            map.values.forEach { it.showAll() }
        }
    }

    fun shutdown() {
        showAll()
        lock.write {
            map.clear()
        }
    }

}