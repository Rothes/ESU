package io.github.rothes.esu.bukkit.command.parser.v1_17_1

import io.github.rothes.esu.bukkit.command.parser.MCRegistryValueParser
import io.github.rothes.esu.bukkit.command.parser.MCRegistryValueParsers
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.leangen.geantyref.TypeToken
import net.minecraft.world.entity.EntityType
import org.incendo.cloud.parser.ParserDescriptor

class MCRegistryValueParsersImpl: MCRegistryValueParsers {

    private val mcRegistryAccessHandler by Versioned(MCRegistryAccessHandler::class.java)
    private val nmsRegistries by Versioned(NmsRegistries::class.java)

    override fun <C> entityType() = ParserDescriptor.of(MCRegistryValueParser<C, EntityType<*>>(mcRegistryAccessHandler, nmsRegistries.entityType), object : TypeToken<EntityType<*>>() {})

}