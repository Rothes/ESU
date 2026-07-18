package io.github.rothes.esu.bukkit.util.version.adapter.adventure

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.lib.adventure.bossbar.BossBar
import io.github.rothes.esu.lib.adventure.bossbar.BossBarImplementation
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.BossEvent
import org.bukkit.entity.Player
import java.util.function.Function

class BossBarImplementationImpl(private val bar: BossBar): BossBar.Listener, BossBarImplementation {

    private var minecraft: ServerBossEvent? = null

    private fun minecraft(): ServerBossEvent {
        return minecraft ?: ServerBossEvent(
            Mth.createInsecureUUID(RandomSource.create()),
            bar.name().toMinecraft(),
            bar.color().toMinecraft(),
            bar.overlay().toMinecraft(),
        ).also {
            bar.addListener(this)
            minecraft = it
        }
    }

    fun show(player: Player) {
        minecraft().addPlayer(player.handle)
    }

    fun hide(player: Player) {
        val vanilla = minecraft ?: return
        vanilla.removePlayer(player.handle)
        if (vanilla.players.isEmpty()) {
            bar.removeListener(this)
            this.minecraft = null
        }
    }

    override fun bossBarNameChanged(bar: BossBar, oldName: Component, newName: Component) {
        broadcast { ClientboundBossEventPacket.createUpdateNamePacket(it) }
    }

    override fun bossBarProgressChanged(bar: BossBar, oldProgress: Float, newProgress: Float) {
        broadcast { ClientboundBossEventPacket.createUpdateProgressPacket(it) }
    }

    override fun bossBarColorChanged(bar: BossBar, oldColor: BossBar.Color, newColor: BossBar.Color) {
        broadcast { ClientboundBossEventPacket.createUpdateStylePacket(it) }
    }

    override fun bossBarOverlayChanged(bar: BossBar, oldOverlay: BossBar.Overlay, newOverlay: BossBar.Overlay) {
        broadcast { ClientboundBossEventPacket.createUpdateStylePacket(it) }
    }

    override fun bossBarFlagsChanged(bar: BossBar, flagsAdded: Set<BossBar.Flag>, flagsRemoved: Set<BossBar.Flag>) {
        broadcast { ClientboundBossEventPacket.createUpdatePropertiesPacket(it) }
    }

    private fun broadcast(fn: Function<BossEvent, ClientboundBossEventPacket>) {
        minecraft?.broadcast(fn)
    }

}