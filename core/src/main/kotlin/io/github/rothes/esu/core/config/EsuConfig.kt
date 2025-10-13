package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.PostProcess
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Setting
import java.net.URLConnection
import java.nio.file.Path
import java.util.*
import kotlin.io.path.name

object EsuConfig {

    init {
        // Default to false right now. load() will cache kotlin.isData
        URLConnection.setDefaultUseCaches("jar", false)
    }

    internal var initialized = false
    private lateinit var data: ConfigData

    init {
        reloadConfig()
    }

    fun get() = data

    fun reloadConfig() {
        data = load()
        initialized = true

        URLConnection.setDefaultUseCaches("jar", !data.disableJarFileCache)
    }

    private fun load(): ConfigData = ConfigLoader.load(EsuCore.instance.baseConfigPath().resolve("config.yml"))

    data class ConfigData(
        val locale: String = Locale.getDefault().language + '_' + Locale.getDefault().country.lowercase(),
        @Comment("""
            Enable this will force to print TrueColor messages in console. This would provide a pretty look.
            Disable this, or change your terminal software if you see weird chars in console.
        """)
        val forceTrueColorConsole: Boolean = true,
        @Comment("""
            By setting this to true, you can enable legacy color char support.
            You will able to use `&` char to set color/formats.
        """)
        val legacyColorChar: Boolean = false,
        @Comment("""
            Automatically create soft link for locale directories.
            You can specify the path to ESU plugin directory from another server.
            Example: /home/user/server/plugins/ESU
        """)
        val localeSoftLinkPath: Optional<Path> = Optional.empty(),
        val database: Database = Database(),
        val defaultColorScheme: String = "amethyst",
        val updateChecker: Boolean = true,
        @Comment("""
            Set this to true will disable caching reading files in jars. This is globally in jvm.
            If you frequently hot-update plugins, setting this to true will reduce errors,
             but may reduce performance in some specific scenarios.
        """)
        val disableJarFileCache: Boolean = false,
    ): ConfigurationPart {

        @field:Setting("maven-repository")
        @RemovedNode("0.11.0")
        private var mavenRepoInternal: Unit = Unit

        data class Database(
            @Comment("""
                The database software you want to use.
                Supports 'H2'(built-in file system db), 'MySQL', 'MariaDB'
            """)
            var databaseType: String = "H2",
            @Comment("""
                Below settings are only needed when you have a database server.
            """)
            var host: String = "127.0.0.1",
            var port: Int = 3306,
            var database: String = "esu",
            var username: String = "root",
            var password: String = "root",
        ) {

            val driverClassName: String
                get() = when (databaseType.uppercase()) {
                    "H2"      -> "org.h2.Driver"
                    "MYSQL"   -> "com.mysql.jdbc.Driver"
                    "MARIADB" -> "org.mariadb.jdbc.Driver"
                    else      -> error("Unsupported database type: $databaseType")
                }

            val url: String
                get() = when (databaseType.uppercase()) {
                    "H2"      -> "jdbc:h2:file:./plugins/${EsuCore.instance.baseConfigPath().name}/h2;MODE=MYSQL"
                    "MYSQL"   -> "jdbc:mysql://$host:$port/$database"
                    "MARIADB" -> "jdbc:mariadb://$host:$port/$database"
                    else      -> error("Unsupported database type: $databaseType")
                }

            @RemovedNode("0.9.1")
            val jdbcDriver: String? = null
            @RemovedNode("0.9.1")
            val jdbcUrl: String? = null

            @PostProcess
            private fun upgrade() {
                if (jdbcUrl != null) {
                    val split = jdbcUrl.split(':', limit = 3)
                    if (split.size < 3) return
                    when (split[1]) {
                        "h2" -> {
                            databaseType = "H2"
                        }
                        "mysql",
                        "mariadb" -> {
                            databaseType = split[1].uppercase()
                            val reg = "//(.+):(.+)/(.+)".toRegex().matchEntire(split[2])
                            if (reg != null) {
                                host = reg.groups[1]?.value ?: host
                                port = reg.groups[2]?.value?.toIntOrNull() ?: port
                                database = reg.groups[3]?.value ?: database
                            }
                        }
                    }
                }
            }

        }
    }
}