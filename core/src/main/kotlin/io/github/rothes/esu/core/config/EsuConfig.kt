package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.util.NetworkUtils.uriLatency
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Comment
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.PostProcess
import io.github.rothes.esu.lib.org.spongepowered.configurate.objectmapping.meta.Setting
import java.net.URLConnection
import java.nio.file.Path
import java.util.*

object EsuConfig {

    init {
        // Default to false right now. load() will cache kotlin.isData
        URLConnection.setDefaultUseCaches("jar", false)
    }

    private lateinit var data: ConfigData

    init {
        reloadConfig()
    }

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
        @field:Setting("maven-repository")
        @field:Comment("""
The Maven repository to download dependencies from. Do not set if you don't know about it.
Delete or set to null will re-run the latency test, and select the best automatically.""")
        private var mavenRepoInternal: MavenRepo = MavenRepo(),
        val updateChecker: Boolean = true,
        @field:Comment("""
Set this to true will disable caching reading files in jars. This is globally in jvm.
If you frequently hot-update plugins, setting this to true will reduce errors,
 but may reduce performance in some cases.""")
        val disableJarFileCache: Boolean = false,
    ): ConfigurationPart {

        val mavenRepo
            get() = mavenRepoInternal

        @PostProcess
        private fun postProcess() {
            if (mavenRepo.id.isEmpty() || mavenRepo.url.isEmpty())
                mavenRepoInternal = findBestMavenRepo()
        }

        companion object {
            fun findBestMavenRepo(): MavenRepo {
                EsuCore.instance.info("Running latency test of maven repositories...")
                val def = MavenRepo("central", "https://maven-central.storage-download.googleapis.com/maven2/")
                val repos = listOf(
                    MavenRepo("aliyun", "https://maven.aliyun.com/repository/public/"),
                    def,
                    MavenRepo("central", "https://maven-central-eu.storage-download.googleapis.com/maven2/"),
                    MavenRepo("central", "https://maven-central-asia.storage-download.googleapis.com/maven2/"),
                )
                val tested = repos.map {
                    it to it.url.uriLatency
                }.sortedBy { if (it.second >= 0) it.second else Long.MAX_VALUE }
                for ((repo, latency) in tested) {
                    EsuCore.instance.info("'${repo.url}': ${latency}ms")
                }
                val best = tested.first()
                return if (best.second != Long.MAX_VALUE) best.first else def
            }
        }

        data class MavenRepo(
            val id: String = "",
            val url: String = "",
        )

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
        )
    }
}