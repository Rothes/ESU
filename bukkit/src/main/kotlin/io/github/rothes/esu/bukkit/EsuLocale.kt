package io.github.rothes.esu.bukkit

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import org.incendo.cloud.bukkit.BukkitDefaultCaptionsProvider
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider
import org.spongepowered.configurate.objectmapping.meta.PostProcess

object EsuLocale {

    private var data: MultiLocaleConfiguration<LocaleData> = load()

    fun get() = data

    fun reloadConfig() {
        data = load()
    }

    private fun load(): MultiLocaleConfiguration<LocaleData> = ConfigLoader.loadMulti(
        EsuCore.instance.baseConfigPath().resolve("locale"), "en_us.yml"
    )

    data class LocaleData(
        val commandCaptions: LinkedHashMap<String, String> = LinkedHashMap()
    ): ConfigurationPart {

        @PostProcess
        fun fillCaptions() {
            StandardCaptionsProvider<Any>().keyMap.forEach { (k, v) ->
                commandCaptions.putIfAbsent(k, v)
            }
            BukkitDefaultCaptionsProvider<Any>().keyMap.forEach { (k, v) ->
                commandCaptions.putIfAbsent(k, v)
            }
        }

        companion object {
            private val DelegatingCaptionProvider<*>.keyMap
                get() = (delegate() as ConstantCaptionProvider).captions().mapKeys { it.key.key() }
        }
    }

}