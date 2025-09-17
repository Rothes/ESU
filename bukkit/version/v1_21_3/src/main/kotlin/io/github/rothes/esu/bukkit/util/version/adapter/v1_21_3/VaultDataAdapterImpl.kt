package io.github.rothes.esu.bukkit.util.version.adapter.v1_21_3

import io.github.rothes.esu.bukkit.util.version.adapter.VaultDataAdapter
import org.bukkit.block.data.type.Vault


class VaultDataAdapterImpl: VaultDataAdapter {

    override fun getVaultState(vault: Vault): Vault.State {
        return vault.vaultState
    }

    override fun setVaultState(vault: Vault, state: Vault.State) {
        vault.vaultState = state
    }
}