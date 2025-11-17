package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.util.extension.math.floorI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.bukkit.Location
import org.bukkit.World
import kotlin.time.Duration.Companion.seconds

object WorldUtils {

    suspend fun findSafeSpot(column: Location): Location? {
        val deferred = CompletableDeferred<Location?>()
        val location = column.clone()
        location.x += 0.5
        location.z += 0.5
        Scheduler.schedule(location) {
            val world = requireNotNull(location.world) { "Location world is null" }
            val y = if (world.environment == World.Environment.NETHER) {
                val x = location.x.floorI()
                val z = location.z.floorI()
                var y = 125
                while (true) {
                    if (y == 0) {
                        deferred.complete(null)
                        return@schedule
                    }
                    y--
                    if (world.getBlockAt(x, y + 2, z).type.isAir
                        && world.getBlockAt(x, y + 1, z).type.isAir
                        && world.getBlockAt(x, y, z).type.isSolid) {
                        break
                    }
                }
                y
            } else {
                world.getHighestBlockYAt(location)
            }
            location.y = y.toDouble() + 1
            deferred.complete(location)
        }
        return withTimeout(1.seconds) {
            deferred.await()
        }
    }

}