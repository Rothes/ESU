package io.github.rothes.esu.bukkit.config.data

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import java.util.UUID

data class AttributeModifierData(
    val key: String = UUID.randomUUID().toString().lowercase(),
    val amount: Double = 0.0,
    val operation: AttributeModifier.Operation? = null,
    val slot: String? = null,
) {

    val namespacedKey by lazy {
        NamespacedKey.fromString(key) ?: error("Invalid name $key")
    }

    val slotGroup: EquipmentSlotGroup by lazy {
        slot?.let { EquipmentSlotGroup.getByName(it) ?: error("Unknown slot $slot") }
            ?: EquipmentSlotGroup.ANY
    }

    val bukkit by lazy {
        // Confirmed on 1.21.1, not sure which version since added
        AttributeModifier(namespacedKey, amount, operation ?: AttributeModifier.Operation.ADD_NUMBER, slotGroup)
    }

}