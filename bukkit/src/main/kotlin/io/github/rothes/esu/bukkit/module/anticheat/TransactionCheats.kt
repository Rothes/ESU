package io.github.rothes.esu.bukkit.module.anticheat

import io.github.rothes.esu.core.module.CommonFeature

object TransactionCheats : CommonFeature<Unit, Unit>() {

    override val name: String = "Transaction"

    init {
//        registerFeature(PingPong)
    }

    override fun onEnable() {}

}