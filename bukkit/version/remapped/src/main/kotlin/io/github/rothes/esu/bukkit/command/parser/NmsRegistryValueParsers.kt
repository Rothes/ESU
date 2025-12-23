package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.versioned
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import org.incendo.cloud.parser.ParserDescriptor

interface NmsRegistryValueParsers {

    fun <C> biome(): ParserDescriptor<C, Biome>
    fun <C> entityType(): ParserDescriptor<C, EntityType<*>>

    companion object {

        val isSupported = ServerCompatibility.serverVersion >= "17.1"
        val instance: NmsRegistryValueParsers by lazy { NmsRegistryValueParsers::class.java.versioned() }

        fun <C> all(): List<ParserDescriptor<C, *>> {
            return listOf(
                instance.biome(),
                instance.entityType(),
            )
        }
    }

}