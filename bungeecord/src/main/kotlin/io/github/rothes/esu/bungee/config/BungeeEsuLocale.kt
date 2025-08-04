package io.github.rothes.esu.bungee.config

import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.bungee.config.BungeeEsuLocale.BungeeLocaleData
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider
import org.spongepowered.configurate.objectmapping.meta.PostProcess

object BungeeEsuLocale: EsuLocale<BungeeLocaleData>() {

    init {
        instance = this
    }

    override fun load(): MultiLocaleConfiguration<BungeeLocaleData> = ConfigLoader.loadMulti(
        EsuCore.Companion.instance.baseConfigPath().resolve("lang"), "en_us.yml"
    )

    class BungeeLocaleData: BaseEsuLocaleData() {

        @PostProcess
        fun fillCaptions() {
            StandardCaptionsProvider<Any>().keyMap.forEach { (k, v) ->
                commandCaptions.putIfAbsent(k, v)
            }
        }

        companion object {
            private val DelegatingCaptionProvider<*>.keyMap
                get() = (delegate() as ConstantCaptionProvider).captions().mapKeys { it.key }
        }
    }

}