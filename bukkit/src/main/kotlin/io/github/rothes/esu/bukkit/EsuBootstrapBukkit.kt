package io.github.rothes.esu.bukkit

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.AetherLoader
import io.github.rothes.esu.core.util.artifact.MavenResolver
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.util.logging.Level

class EsuBootstrapBukkit: JavaPlugin(), EsuBootstrap {

    init {
        EsuBootstrap.setInstance(this)
        AetherLoader.loadAether()
        MavenResolver.loadKotlin()
    }

    val esu = EsuPluginBukkit(this)

    override fun onLoad() {
        super.onLoad()
    }

    override fun onEnable() {
        esu.onEnable()
    }

    override fun onDisable() {
        esu.onDisable()
    }

    override fun info(message: String) {
        logger.log(Level.INFO, message)
    }

    override fun warn(message: String) {
        logger.log(Level.WARNING, message)
    }

    override fun err(message: String) {
        logger.log(Level.SEVERE, message)
    }

    override fun err(message: String, throwable: Throwable?) {
        logger.log(Level.SEVERE, message, throwable)
    }

    override fun baseConfigPath(): Path {
        return dataFolder.toPath()
    }

}