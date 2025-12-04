package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.versioned
import net.minecraft.world.entity.EntityType
import org.incendo.cloud.parser.ParserDescriptor

interface MCRegistryValueParsers {

    fun <C> entityType(): ParserDescriptor<C, EntityType<*>>

    companion object {

        val isSupported = ServerCompatibility.serverVersion >= "17.1"
        val instance: MCRegistryValueParsers by lazy { MCRegistryValueParsers::class.java.versioned() }

        fun <C> all(): List<ParserDescriptor<C, *>> {
            return listOf(
                instance.entityType()
            )
        }
    }

}