package io.github.rothes.esu.bukkit.event

import org.bukkit.event.Cancellable

interface CancellableKt: Cancellable {

    var cancelledKt: Boolean

    override fun isCancelled(): Boolean = cancelledKt

    override fun setCancelled(cancel: Boolean) {
        this.cancelledKt = cancel
    }

}