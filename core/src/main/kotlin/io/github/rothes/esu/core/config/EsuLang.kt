package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.config.EsuLang.BaseEsuLangData
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.util.InitOnce
import org.incendo.cloud.caption.Caption

abstract class EsuLang<T: BaseEsuLangData> {

    private var data: MultiLangConfiguration<T> = load()

    fun get() = data

    fun reloadConfig() {
        data = load()
    }

    protected abstract fun load(): MultiLangConfiguration<T>

    open class BaseEsuLangData(
        val updater: Updater = Updater(),
        val booleans: Booleans = Booleans(),
        val format: Format = Format(),
        val commandCaptions: LinkedHashMap<Caption, String> = LinkedHashMap(),
    ): ConfigurationPart {

        data class Updater(
            val checker: Checker = Checker(),
        ) {
            data class Checker(
                val networkError: String = "<ec>Failed to check for update: <message>",
                val unknownChannel: String = "<ec>Failed to check for update, unknown current channel: <channel>",
                val unknownPlatform: String = "<ec>Failed to check for update, unknown current platform: <platform>",
            )
        }

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
        var instance: EsuLang<out BaseEsuLangData> by InitOnce()

        fun get() = instance.data
    }

}