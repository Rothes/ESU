package io.github.rothes.esu.bukkit.util.version.adapter.v0

import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import org.bukkit.attribute.Attribute

class AttributeAdapterImpl: AttributeAdapter {

    override val maxHealth: Attribute = Attribute.GENERIC_MAX_HEALTH
    override val followRange: Attribute = Attribute.GENERIC_FOLLOW_RANGE
    override val knockbackResistance: Attribute = Attribute.GENERIC_KNOCKBACK_RESISTANCE
    override val movementSpeed: Attribute = Attribute.GENERIC_MOVEMENT_SPEED
    override val flyingSpeed: Attribute = Attribute.GENERIC_FLYING_SPEED
    override val attackDamage: Attribute = Attribute.GENERIC_ATTACK_DAMAGE
    override val attackKnockback: Attribute = Attribute.GENERIC_ATTACK_KNOCKBACK
    override val attackSpeed: Attribute = Attribute.GENERIC_ATTACK_SPEED
    override val armor: Attribute = Attribute.GENERIC_ARMOR
    override val armorToughness: Attribute = Attribute.GENERIC_ARMOR_TOUGHNESS
    override val fallDamageMultiplier: Attribute = Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER
    override val luck: Attribute = Attribute.GENERIC_LUCK
    override val maxAbsorption: Attribute = Attribute.GENERIC_MAX_ABSORPTION
    override val safeFallDistance: Attribute = Attribute.GENERIC_SAFE_FALL_DISTANCE
    override val scale: Attribute = Attribute.GENERIC_SCALE
    override val stepHeight: Attribute = Attribute.GENERIC_STEP_HEIGHT
    override val gravity: Attribute = Attribute.GENERIC_GRAVITY
    override val jumpStrength: Attribute = Attribute.GENERIC_JUMP_STRENGTH // Was HORSE_JUMP_STRENGTH
    override val burningTime: Attribute = Attribute.GENERIC_BURNING_TIME
    override val explosionKnockbackResistance: Attribute = Attribute.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE
    override val movementEfficiency: Attribute = Attribute.GENERIC_MOVEMENT_EFFICIENCY
    override val oxygenBonus: Attribute = Attribute.GENERIC_OXYGEN_BONUS
    override val waterMovementEfficiency: Attribute = Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY
    override val temptRange: Attribute
        get() = error("Not exists before 1.21.1")
    override val blockInteractionRange: Attribute = Attribute.PLAYER_BLOCK_INTERACTION_RANGE
    override val entityInteractionRange: Attribute = Attribute.PLAYER_ENTITY_INTERACTION_RANGE
    override val blockBreakSpeed: Attribute = Attribute.PLAYER_BLOCK_BREAK_SPEED
    override val miningEfficiency: Attribute = Attribute.PLAYER_MINING_EFFICIENCY
    override val sneakingSpeed: Attribute = Attribute.PLAYER_SNEAKING_SPEED
    override val submergedMiningSpeed: Attribute = Attribute.PLAYER_SUBMERGED_MINING_SPEED
    override val sweepingDamageRatio: Attribute = Attribute.PLAYER_SWEEPING_DAMAGE_RATIO
    override val spawnReinforcements: Attribute = Attribute.ZOMBIE_SPAWN_REINFORCEMENTS

    override fun of(name: String): Attribute? {
        return Attribute.entries.find { it.name.substringAfter('_') == name.uppercase() }
    }

    override fun getKey(attribute: Attribute): String {
        return attribute.name.substringAfter('_').lowercase()
    }

}