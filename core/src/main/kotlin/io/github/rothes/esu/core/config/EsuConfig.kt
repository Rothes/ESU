/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.config

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.user.LogUser
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
        LogUser.onReload()
    }

    private fun load(): ConfigData = ConfigLoader.load(EsuCore.instance.baseConfigPath.resolve("config.yml"))

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
                    "H2"      -> "jdbc:h2:file:./plugins/${EsuCore.instance.baseConfigPath.name}/h2;MODE=MYSQL"
                    "MYSQL"   -> "jdbc:mysql://$host:$port/$database"
                    "MARIADB" -> "jdbc:mariadb://$host:$port/$database"
                    else      -> error("Unsupported database type: $databaseType")
                }

        }
    }
}