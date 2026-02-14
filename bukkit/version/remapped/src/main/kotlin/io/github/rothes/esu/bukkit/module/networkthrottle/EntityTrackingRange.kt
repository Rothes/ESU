package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usNullableObjAccessor
import net.minecraft.server.level.ChunkMap
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.spigotmc.SpigotWorldConfig
import org.spigotmc.TrackingRange as SpigotTrackingRange

object EntityTrackingRange : CommonFeature<EntityTrackingRange.FeatureConfig, EntityTrackingRange.FeatureLang>() {

    private val teAccessor = Entity::class.java.getDeclaredField(if (ServerCompatibility.serverVersion >= 21) "trackedEntity" else "tracker").usNullableObjAccessor
    private val ctrAccessor = EntityType::class.java.getDeclaredField("clientTrackingRange").usIntAccessor
    private var lastOverride: Map<EntityType<*>, Int>? = null

    override fun onReload() {
        super.onReload()
        if (enabled) {
            applyConfig()
        }
    }

    override fun onEnable() {
        applyConfig()
        registerCommands(object {
            @Command("esu networkThrottle entityTrackingRange updateTrackedEntities")
            @ShortPerm
            fun updateTrackedEntities(sender: User) {
                updateTrackedEntities()
                sender.message(lang, { updatedTrackedEntities })
            }

            @Command("esu networkThrottle entityTrackingRange spigotTrackingRange <trackingType>")
            @ShortPerm
            fun spigotTrackingRangeGet(sender: User, trackingType: SpigotTrackingType) {
                for (level in Bukkit.getWorlds().map { it as CraftWorld }.map { it.handle }) {
                    val spigotConfig = level.spigotConfig
                    sender.message(lang, { spigotTrackingRange.get },
                        unparsed("type", trackingType.name.lowercase()),
                        unparsed("world", level.world.name),
                        unparsed("value", trackingType.accessor[spigotConfig])
                    )
                }
            }

            @Command("esu networkThrottle entityTrackingRange spigotTrackingRange <trackingType> <range>")
            @ShortPerm
            fun spigotTrackingRangeSet(sender: User, trackingType: SpigotTrackingType, range: Int) {
                for (level in Bukkit.getWorlds().map { it as CraftWorld }.map { it.handle }) {
                    val spigotConfig = level.spigotConfig
                    trackingType.accessor[spigotConfig] = range
                }
                sender.message(lang, { spigotTrackingRange.set },
                    unparsed("type", trackingType.name.lowercase()),
                    unparsed("value", range)
                )
            }

            @Command("esu networkThrottle entityTrackingRange clientTrackingRange <entityType>")
            @ShortPerm
            fun clientTrackingRangeGet(sender: User, entityType: EntityType<*>) {
                sender.message(lang, { clientTrackingRange.get },
                    unparsed("type", entityType.descriptionId),
                    unparsed("value", ctrAccessor[entityType])
                )
            }

            @Suggestions("spigotTrackingType")
            fun spigotTrackingType() = listOf("players", "animals", "monsters", "misc", "display", "other")
        })
    }

    private fun applyConfig() {
        val current = config.clientTrackingRangeOverride
        if (lastOverride != current) {
            for ((entityType, range) in current) {
                ctrAccessor[entityType] = range
            }
            updateTrackedEntities()
        }
        lastOverride = current
    }

    private fun updateTrackedEntities() {
        val lookup by Versioned(LevelEntitiesHandler::class.java)
        val range = ChunkMap.TrackedEntity::class.java.getDeclaredField("range").usIntAccessor
        for (level in Bukkit.getWorlds().map { it as CraftWorld }.map { it.handle }) {
            val entities = lookup.getEntitiesAll(level)
            for (entity in entities) {
                val trackedEntity = teAccessor[entity] ?: continue
                range[trackedEntity] = SpigotTrackingRange.getEntityTrackingRange(
                    entity,
                    entity.type.clientTrackingRange() shl 4 // * 16
                )
            }
        }
    }

    enum class SpigotTrackingType(
        val field: String
    ) {
        PLAYERS("playerTrackingRange"),
        ANIMALS("animalTrackingRange"),
        MONSTERS("monsterTrackingRange"),
        MISC("miscTrackingRange"),
        DISPLAY("displayTrackingRange"),
        OTHER("otherTrackingRange");

        val accessor by lazy { SpigotWorldConfig::class.java.getDeclaredField(field).usIntAccessor }
    }

    data class FeatureConfig(
        @Comment("""
            Modify the client tracking range of an entity type.
            Unit in chunks.
        """)
        val clientTrackingRangeOverride: Map<EntityType<*>, Int> = mapOf(EntityType.PIG to EntityType.PIG.clientTrackingRange()),
    ): BaseFeatureConfiguration()

    data class FeatureLang(
        val updatedTrackedEntities: MessageData = "<pc>Updated all tracked entities.".message,
        val clientTrackingRange: ClientTrackingRange = ClientTrackingRange(),
        val spigotTrackingRange: SpigotTrackingRange = SpigotTrackingRange(),
    ) {
        data class ClientTrackingRange(
            val get: MessageData = "<pc>Client tracking range of <pdc><type> <pc>is <pdc><value> <pc>chunks.".message,
        )
        data class SpigotTrackingRange(
            val get: MessageData = "<pc>Spigot tracking range of <pdc><type> <pc>in world <pdc><world> <pc>is <pdc><value> <pc>blocks.".message,
            val set: MessageData = ("<pc>Set spigot tracking range of <pdc><type> <pc>to <pdc><value> <pc>blocks in this run." +
                    "<chat><sc>Remember to <click:run_command:'/esu networkThrottle entityTrackingRange updateTrackedEntities'><sdc>[Update]</sdc></click> to make it take affect.").message,
        )
    }

}