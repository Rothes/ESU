package io.github.rothes.esu.bukkit.util.version.adapter.v21_3

import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

class AttributeAdapterImpl: AttributeAdapter {

    override val maxHealth: Attribute = Attribute.MAX_HEALTH
    override val followRange: Attribute = Attribute.FOLLOW_RANGE
    override val knockbackResistance: Attribute = Attribute.KNOCKBACK_RESISTANCE
    override val movementSpeed: Attribute = Attribute.MOVEMENT_SPEED
    override val flyingSpeed: Attribute = Attribute.FLYING_SPEED
    override val attackDamage: Attribute = Attribute.ATTACK_DAMAGE
    override val attackKnockback: Attribute = Attribute.ATTACK_KNOCKBACK
    override val attackSpeed: Attribute = Attribute.ATTACK_SPEED
    override val armor: Attribute = Attribute.ARMOR
    override val armorToughness: Attribute = Attribute.ARMOR_TOUGHNESS
    override val fallDamageMultiplier: Attribute = Attribute.FALL_DAMAGE_MULTIPLIER
    override val luck: Attribute = Attribute.LUCK
    override val maxAbsorption: Attribute = Attribute.MAX_ABSORPTION
    override val safeFallDistance: Attribute = Attribute.SAFE_FALL_DISTANCE
    override val scale: Attribute = Attribute.SCALE
    override val stepHeight: Attribute = Attribute.STEP_HEIGHT
    override val gravity: Attribute = Attribute.GRAVITY
    override val jumpStrength: Attribute = Attribute.JUMP_STRENGTH
    override val burningTime: Attribute = Attribute.BURNING_TIME
    override val explosionKnockbackResistance: Attribute = Attribute.EXPLOSION_KNOCKBACK_RESISTANCE
    override val movementEfficiency: Attribute = Attribute.MOVEMENT_EFFICIENCY
    override val oxygenBonus: Attribute = Attribute.OXYGEN_BONUS
    override val waterMovementEfficiency: Attribute = Attribute.WATER_MOVEMENT_EFFICIENCY
    override val temptRange: Attribute = Attribute.TEMPT_RANGE
    override val blockInteractionRange: Attribute = Attribute.BLOCK_INTERACTION_RANGE
    override val entityInteractionRange: Attribute = Attribute.ENTITY_INTERACTION_RANGE
    override val blockBreakSpeed: Attribute = Attribute.BLOCK_BREAK_SPEED
    override val miningEfficiency: Attribute = Attribute.MINING_EFFICIENCY
    override val sneakingSpeed: Attribute = Attribute.SNEAKING_SPEED
    override val submergedMiningSpeed: Attribute = Attribute.SUBMERGED_MINING_SPEED
    override val sweepingDamageRatio: Attribute = Attribute.SWEEPING_DAMAGE_RATIO
    override val spawnReinforcements: Attribute = Attribute.SPAWN_REINFORCEMENTS

    override fun of(name: String): Attribute? {
        return Registry.ATTRIBUTE.get(NamespacedKey.fromString(name)!!)
    }

    override fun getKey(attribute: Attribute): String {
        val key = attribute.key
        return if (key.namespace == NamespacedKey.MINECRAFT)
            key.key
        else
            key.toString()
    }

}