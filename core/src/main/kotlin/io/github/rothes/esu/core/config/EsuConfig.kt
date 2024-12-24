package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.nio.file.Path
import java.util.*

object EsuConfig {

    private var data: ConfigData = load()

    fun get() = data

    fun reloadConfig() {
        data = load()
    }

    private fun load(): ConfigData = ConfigLoader.load(EsuCore.instance.baseConfigPath().resolve("config.yml"))

    data class ConfigData(
        val locale: String = Locale.getDefault().language + '_' + Locale.getDefault().country.lowercase(),
        @field:Comment("Automatically create soft link for locale directories. " +
                "You can specify the path to ESU plugin directory from another server.\n" +
                "Example: /home/user/server/plugins/ESU")
        val localeSoftLinkPath: Optional<Path> = Optional.empty(),
        val database: Database = Database(),
        val defaultColorScheme: String = "amethyst",
    ): ConfigurationPart {

        data class Database(
            @field:Comment("If you don't have a database server,\n" +
                    "set 'jdbc-driver' to 'io.github.rothes.esu.lib.org.h2.Driver'\n" +
                    "and 'jdbc-url' to 'jdbc:h2:file:./plugins/ESU/h2;MODE=MYSQL'")
            val jdbcDriver: String = "com.mysql.jdbc.Driver",
            val jdbcUrl: String = "jdbc:mysql://127.0.0.1:3306/esu",
            val username: String = "root",
            val password: String = "root",
        ): ConfigurationPart
    }
}