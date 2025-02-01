package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.config.EsuLocale.BaseEsuLocaleData
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
        val booleans: Booleans = Booleans(),
        val format: Format = Format(),
        val commandCaptions: LinkedHashMap<Caption, String> = LinkedHashMap()
    ): ConfigurationPart {

        data class Booleans(
            val enabled: String = "enabled",
            val disabled: String = "disabled",
            val on: String = "on",
            val off: String = "off",
            val yes: String = "yes",
            val no: String = "no",
        ): ConfigurationPart

        data class Format(
            val duration: Duration = Duration(),
        ): ConfigurationPart {
            data class Duration(
                val day: String = "<value>d",
                val hour: String = "<value>h",
                val minute: String = "<value>m",
                val second: String = "<value>s",
                val millis: String = "<value>ms",
                val separator: String = " ",
            ): ConfigurationPart
        }
    }

    companion object {
        var instance: EsuLocale<out BaseEsuLocaleData> by InitOnce()

        fun get() = instance.data
    }

}