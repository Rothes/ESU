package io.github.rothes.esu.velocity.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import io.github.rothes.esu.velocity.config.VelocityEsuLang.VelocityLangData
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider

object VelocityEsuLang: EsuLang<VelocityLangData>() {

    init {
        instance = this
    }

    override fun load(): MultiLangConfiguration<VelocityLangData> = ConfigLoader.loadMulti(
        EsuCore.instance.baseConfigPath().resolve("lang"), "en_us"
    )

    class VelocityLangData: BaseEsuLangData() {

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