package io.github.rothes.esu.bukkit.module.optimizations

import com.google.common.collect.ImmutableMap
import io.github.rothes.esu.bukkit.command.parser.NmsRegistryValueParsers
import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.optimizations.BiomeSpawnersFeature.BiomeSettings.WeightedSpawnerData
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryValueSerializers
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiConfiguration
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import net.minecraft.util.random.WeightedList
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.MobSpawnSettings
import org.incendo.cloud.annotations.Command
import java.lang.reflect.Field

object BiomeSpawnersFeature: CommonFeature<BiomeSpawnersFeature.FeatureConfig, Unit>() {

    private const val DIR_NAME = "biome_spawners"

    private var previousSettingHash: Int = 0

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (ServerCompatibility.serverVersion < 17)
                return errFail { "This feature requires Minecraft 1.17+".message }
            null
        }
    }

    override fun onReload() {
        super.onReload()
        if (enabled) applySettings()
    }

    override fun onEnable() {
        registerCommands(object {
            @Command("esu optimizations biomeSpawners dump <biome>")
            @ShortPerm
            fun dump(sender: User, biome: Biome) {
                dumpSettings(biome)
                sender.miniMessage("<pc>Settings of biome <pdc>${getBiomeKey(biome)} <pc>have been dumped.")
            }

            @Command("esu optimizations biomeSpawners dumpAll")
            @ShortPerm
            fun dumpAll(sender: User) {
                val registryAccess by Versioned(NmsRegistryAccessHandler::class.java)
                val registry = registryAccess.getRegistryOrThrow(NmsRegistries::class.java.versioned().biome)
                for (biome in registryAccess.values(registry)) {
                    dump(sender, biome)
                }
            }
        }) {
            it.manager().parserRegistry().registerParser(NmsRegistryValueParsers.instance.biome())
        }
        applySettings()
    }

    private fun applySettings() {
        val settings = loadSettings()
        if (settings.hashCode() == previousSettingHash) return
        val spawnersField = spawnersField
        val converter by Versioned(WeightedSpawnerDataConverter::class.java)
        for ((biome, settings) in settings) {
            spawnersField[biome.mobSettings] = ImmutableMap.copyOf(
                buildMap {
                    for ((category, list) in settings.spawners) {
                        put(category, converter.toWeightedList(list))
                    }
                }
            )
        }
        previousSettingHash = settings.hashCode()
    }

    private fun loadSettings(): List<Pair<Biome, BiomeSettings>> {
        val settings: MultiConfiguration<BiomeSettings> = ConfigLoader.loadMulti(
            module.moduleFolder.resolve(DIR_NAME),
            settings = ConfigLoader.LoaderSettingsMulti(
                yamlLoader = yamlLoader
            )
        )
        val registryAccess by Versioned(NmsRegistryAccessHandler::class.java)
        val registry = registryAccess.getRegistryOrThrow(NmsRegistries::class.java.versioned().biome)
        val keyHandler by Versioned(ResourceKeyHandler::class.java)
        return settings.configs.entries.mapNotNull { (key, settings) ->
            val key = keyHandler.parseResourceKey(registry, key)
            registryAccess.getNullable(registry, key)?.to(settings) ?: let {
                core.warn("[$name] Unknown biome $key")
                null
            }
        }
    }

    private fun dumpSettings(biome: Biome) {
        @Suppress("UNCHECKED_CAST")
        val map = spawnersField[biome.mobSettings] as Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>>
        val converter by Versioned(WeightedSpawnerDataConverter::class.java)
        val data = BiomeSettings(map.mapValues { converter.fromWeightedList(it.value) })
        val key = getBiomeKey(biome)
        val path = module.moduleFolder.resolve(DIR_NAME).resolve("$key.yml")
        val conf = ConfigLoader.loadConfiguration(path, ConfigLoader.LoaderSettings(yamlLoader = yamlLoader))
        conf.node.set(data)
        conf.save()
    }

    private fun getBiomeKey(biome: Biome): String {
        val registryAccess by Versioned(NmsRegistryAccessHandler::class.java)
        val registry = registryAccess.getRegistryOrThrow(NmsRegistries::class.java.versioned().biome)
        val keyHandler by Versioned(ResourceKeyHandler::class.java)
        return keyHandler.getResourceKeyString(registryAccess.getResourceKey(registry, biome))
    }

    private val yamlLoader: (YamlConfigurationLoader.Builder) -> YamlConfigurationLoader.Builder
        get() = {
            it.defaultOptions { op ->
                op.serializers { b ->
                    b.register(NmsRegistryValueSerializers.instance.entityType)
                }
            }
        }

    private val spawnersField: Field
        get() {
            return MobSpawnSettings::class.java.getDeclaredField("spawners").also { it.isAccessible = true }
        }

    data class BiomeSettings(
        val spawners: Map<MobCategory, List<WeightedSpawnerData>> = mapOf(),
    ) {
        data class WeightedSpawnerData(
            val entityType: EntityType<*> = EntityType.PIG,
            val minCount: Int = 1,
            val maxCount: Int = 1,
            val weight: Int = 1,
        )
    }

    interface WeightedSpawnerDataConverter {

        fun fromWeightedList(weightedList: Any) : List<WeightedSpawnerData>
        fun toWeightedList(list: List<WeightedSpawnerData>) : Any

    }

    @Comment("""
        Modify natural spawners of the biome. Use command /esu optimizations biomeSpawners dump <biome>
         to dump current biome settings, and change it in $DIR_NAME directory.
    """)
    class FeatureConfig: BaseFeatureConfiguration()

}