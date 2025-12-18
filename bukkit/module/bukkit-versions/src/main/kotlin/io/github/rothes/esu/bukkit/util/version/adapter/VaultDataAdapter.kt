package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.version.Versioned
import org.bukkit.block.data.type.Vault

interface VaultDataAdapter {

    fun getVaultState(vault: Vault): Vault.State
    fun setVaultState(vault: Vault, state: Vault.State)

    companion object {

        val instance by Versioned(VaultDataAdapter::class.java)

        var Vault.state
            get() = instance.getVaultState(this)
            set(value) = instance.setVaultState(this, value)

    }
}