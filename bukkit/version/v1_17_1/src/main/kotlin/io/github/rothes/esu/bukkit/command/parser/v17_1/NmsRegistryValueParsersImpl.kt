package io.github.rothes.esu.bukkit.command.parser.v17_1

import io.github.rothes.esu.bukkit.command.parser.NmsRegistryValueParser
import io.github.rothes.esu.bukkit.command.parser.NmsRegistryValueParsers
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biome
import org.incendo.cloud.parser.ParserDescriptor

object NmsRegistryValueParsersImpl: NmsRegistryValueParsers {

    private val nmsRegistryAccessHandler by Versioned(NmsRegistryAccessHandler::class.java)
    private val nmsRegistries by Versioned(NmsRegistries::class.java)

    override fun <C> biome(): ParserDescriptor<C, Biome> = ParserDescriptor.of(NmsRegistryValueParser<C, Biome>(nmsRegistryAccessHandler, nmsRegistries.biome), object : TypeToken<Biome>() {})
    override fun <C> entityType() = ParserDescriptor.of(NmsRegistryValueParser<C, EntityType<*>>(nmsRegistryAccessHandler, nmsRegistries.entityType), object : TypeToken<EntityType<*>>() {})

}