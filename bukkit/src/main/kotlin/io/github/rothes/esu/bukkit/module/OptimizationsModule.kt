package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.optimizations.TicketTypeHandler
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent

object OptimizationsModule: BukkitModule<OptimizationsModule.ModuleConfig, EmptyConfiguration>() {

    override fun onEnable() {
        registerListener(object : Listener {
            @EventHandler
            fun onLiquidSpread(e: BlockFromToEvent) {
                if (e.block.blockData is Waterlogged) {
                    val config = config.waterlogged
                    if (config.disableWaterSpread) {
                        e.isCancelled = true
                    }
                }
            }
            @EventHandler
            fun onPistonPushWaterlogged(e: BlockPistonExtendEvent) {
                handleWaterloggedPush(e.blocks, e)
            }
            @EventHandler
            fun onPistonPushWaterlogged(e: BlockPistonRetractEvent) {
                handleWaterloggedPush(e.blocks, e)
            }

            private fun handleWaterloggedPush(blocks: MutableList<Block>, e: BlockPistonEvent) {
                val config = config.waterlogged
                if (config.disableWaterloggedBlockPush) {
                    if (blocks.any { (it.blockData as? Waterlogged)?.isWaterlogged == true }) {
                        e.isCancelled = true
                        return
                    }
                }
                if (!config.disableWaterSpread || !config.keepWaterAfterPistonPush) {
                    return
                }
                val face = e.direction
                for (block in blocks) {
                    val waterlogged = block.blockData as? Waterlogged ?: continue
                    if (waterlogged.isWaterlogged)
                        Scheduler.schedule(block.location, 3) {
                            val moved = block.getRelative(face)
                            val blockData = moved.blockData as Waterlogged
                            blockData.isWaterlogged = true
                            moved.blockData = blockData
                        }
                }
            }
        })
        applyTicketType()
    }

    override fun onReload() {
        super.onReload()
        if (enabled)
            applyTicketType()
    }

    private fun applyTicketType() {
        for ((key, value) in config.ticketType.startupSettings) {
            val ticketType = TicketTypeHandler.handler.getTicketTypeMap()[key]
            if (ticketType == null) {
                log("$key ticket type not found")
                continue
            }
            ticketType.expiryTicks = value
        }
    }


    data class ModuleConfig(
        @Comment("""
            Change server ticket type settings.
            Tickets control chunk loading. For details, check https://minecraft.wiki/w/Chunk#Tickets
            Setting expiry-ticks to 0 or negative value makes the chunk load forever until the ticket
             is manually removed.
        """)
        val ticketType: TicketType = TicketType(),
        val waterlogged: Waterlogged = Waterlogged(),
    ): BaseModuleConfiguration() {

        data class TicketType(
            @Comment("""
                Change the expiry ticks value once the module is enabled or reloaded.
                For example, if you set `portal: 1`, chunk loaded by portal teleport will be removed
                 right after 1 game tick, so that "chunk loader" will not be working any more.
            """)
            val startupSettings: Map<String, Long> = TicketTypeHandler.handler.getTicketTypeMap().mapValues { it.value.expiryTicks }
        )

        data class Waterlogged(
            @Comment("Enable this will disable water spread from waterlogged blocks.")
            val disableWaterSpread: Boolean = false,
            @Comment("If enabled, water in waterlogged blocks will always be refilled after a piston push.")
            val keepWaterAfterPistonPush: Boolean = false,
            @Comment("""
                If enabled, waterlogged blocks cannot be pushed by pistons.
                This can block any ocean maker flying machine.
            """)
            val disableWaterloggedBlockPush: Boolean = false,
        )
    }

}