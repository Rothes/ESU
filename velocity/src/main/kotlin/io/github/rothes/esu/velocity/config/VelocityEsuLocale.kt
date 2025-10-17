package io.github.rothes.esu.velocity.config

import io.github.rothes.esu.core.config.EsuLocale
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import io.github.rothes.esu.velocity.config.VelocityEsuLocale.VelocityLocaleData
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider

object VelocityEsuLocale: EsuLocale<VelocityLocaleData>() {

    init {
        instance = this
    }

    override fun load(): MultiLocaleConfiguration<VelocityLocaleData> = ConfigLoader.loadMulti(
        EsuCore.instance.baseConfigPath().resolve("lang"), "en_us.yml"
    )

    class VelocityLocaleData: BaseEsuLocaleData() {

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