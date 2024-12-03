package io.github.rothes.esu

import io.github.rothes.esu.EsuLocale.BaseEsuLocaleData
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.caption.Caption

abstract class EsuLocale<T: BaseEsuLocaleData> {

    private var data: MultiLocaleConfiguration<T> = load()

    fun get() = data

    fun reloadConfig() {
        data = load()
    }

    protected abstract fun load(): MultiLocaleConfiguration<T>

    open class BaseEsuLocaleData(
        val commandCaptions: LinkedHashMap<Caption, String> = LinkedHashMap()
    ): ConfigurationPart

    companion object {
        var instance: EsuLocale<out BaseEsuLocaleData> by InitOnce()

        fun get() = instance.data
    }

}