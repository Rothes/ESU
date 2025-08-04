package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.net.URLConnection
import java.nio.file.Path
import java.util.*

object EsuConfig {

    init {
        // Default to false right now. load() will cache kotlin.isData
        URLConnection.setDefaultUseCaches("jar", false)
    }

    private var data: ConfigData = load()

    fun get() = data

    fun reloadConfig() {
        data = load()

        URLConnection.setDefaultUseCaches("jar", !data.disableJarFileCache)
    }

    private fun load(): ConfigData = ConfigLoader.load(EsuCore.instance.baseConfigPath().resolve("config.yml"))

    data class ConfigData(
        val locale: String = Locale.getDefault().language + '_' + Locale.getDefault().country.lowercase(),
        @field:Comment("""
Enable this will force to print TrueColor messages in console. This would provide a pretty look.
Disable this, or change your terminal software if you see weird chars in console.""")
        val forceTrueColorConsole: Boolean = true,
        @field:Comment("By setting this to true, you can enable legacy color char support.\n" +
                "You will able to use `&` char to set color/formats.")
        val legacyColorChar: Boolean = false,
        @field:Comment("Automatically create soft link for locale directories. " +
                "You can specify the path to ESU plugin directory from another server.\n" +
                "Example: /home/user/server/plugins/ESU")
        val localeSoftLinkPath: Optional<Path> = Optional.empty(),
        val database: Database = Database(),
        val defaultColorScheme: String = "amethyst",
        val updateChecker: Boolean = true,
        @field:Comment("""
Set this to true will disable caching reading files in jars. This is globally in jvm.
If you frequently hot-update plugins, setting this to true will reduce errors,
 but may reduce performance in some cases.""")
        val disableJarFileCache: Boolean = false,
    ): ConfigurationPart {

        data class Database(
            @field:Comment(
                """
By default, we use a H2 database.
If you have a MySQL server, and want to use it,
    set 'jdbc-driver' to 'com.mysql.jdbc.Driver'
    and 'jdbc-url' to 'jdbc:mysql://127.0.0.1:3306/esu'
For MariaDB:
    set 'jdbc-driver' to 'org.mariadb.jdbc.Driver'
    and 'jdbc-url' to 'jdbc:mariadb://127.0.0.1:3306/esu'""")
            val jdbcDriver: String = "org.h2.Driver",
            val jdbcUrl: String = "jdbc:h2:file:./plugins/ESU/h2;MODE=MYSQL",
            val username: String = "root",
            val password: String = "root",
        ): ConfigurationPart
    }
}