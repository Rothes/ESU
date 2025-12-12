package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityValidTester
import org.bukkit.entity.Entity

object EntityValidTesterImpl: EntityValidTester {

    private val HANDLE_GETTER by Versioned(EntityHandleGetter::class.java)

    override fun isValid(bukkitEntity: Entity): Boolean {
        val handle = HANDLE_GETTER.getHandle(bukkitEntity)
        return handle.isAlive && handle.valid
    }

}