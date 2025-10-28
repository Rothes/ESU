package io.github.rothes.esu.bukkit.config

import io.github.rothes.esu.bukkit.config.BukkitEsuLang.BukkitLangData
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.config.EsuLang
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.lib.configurate.objectmapping.meta.PostProcess
import org.incendo.cloud.bukkit.BukkitDefaultCaptionsProvider
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import org.incendo.cloud.caption.StandardCaptionsProvider

object BukkitEsuLang: EsuLang<BukkitLangData>() {

    init {
        instance = this
    }

    override fun load(): MultiLangConfiguration<BukkitLangData> = ConfigLoader.loadMulti(
        EsuCore.instance.baseConfigPath().resolve("lang"), "en_us"
    )

    class BukkitLangData: BaseEsuLangData() {

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
                get() = (delegate() as ConstantCaptionProvider).captions().mapKeys { it.key }
        }
    }

}