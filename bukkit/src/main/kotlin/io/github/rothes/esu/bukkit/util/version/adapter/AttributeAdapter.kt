package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.version.Versioned
import org.bukkit.attribute.Attribute

interface AttributeAdapter {

    val maxHealth: Attribute
    val followRange: Attribute
    val knockbackResistance: Attribute
    val movementSpeed: Attribute
    val flyingSpeed: Attribute
    val attackDamage: Attribute
    val attackKnockback: Attribute
    val attackSpeed: Attribute
    val armor: Attribute
    val armorToughness: Attribute
    val fallDamageMultiplier: Attribute
    val luck: Attribute
    val maxAbsorption: Attribute
    val safeFallDistance: Attribute
    val scale: Attribute
    val stepHeight: Attribute
    val gravity: Attribute
    val jumpStrength: Attribute
    val burningTime: Attribute
    val explosionKnockbackResistance: Attribute
    val movementEfficiency: Attribute
    val oxygenBonus: Attribute
    val waterMovementEfficiency: Attribute
    val temptRange: Attribute
    val blockInteractionRange: Attribute
    val entityInteractionRange: Attribute
    val blockBreakSpeed: Attribute
    val miningEfficiency: Attribute
    val sneakingSpeed: Attribute
    val submergedMiningSpeed: Attribute
    val sweepingDamageRatio: Attribute
    val spawnReinforcements: Attribute

    fun of(name: String): Attribute?
    fun getKey(attribute: Attribute): String

    companion object {

        val instance by Versioned(AttributeAdapter::class.java)

        val MAX_HEALTH: Attribute = instance.maxHealth
        val FOLLOW_RANGE: Attribute = instance.followRange
        val KNOCKBACK_RESISTANCE: Attribute = instance.knockbackResistance
        val MOVEMENT_SPEED: Attribute = instance.movementSpeed
        val FLYING_SPEED: Attribute = instance.flyingSpeed
        val ATTACK_DAMAGE: Attribute = instance.attackDamage
        val ATTACK_KNOCKBACK: Attribute = instance.attackKnockback
        val ATTACK_SPEED: Attribute = instance.attackSpeed
        val ARMOR: Attribute = instance.armor
        val ARMOR_TOUGHNESS: Attribute = instance.armorToughness
        val FALL_DAMAGE_MULTIPLIER: Attribute = instance.fallDamageMultiplier
        val LUCK: Attribute = instance.luck
        val MAX_ABSORPTION: Attribute = instance.maxAbsorption
        val SAFE_FALL_DISTANCE: Attribute = instance.safeFallDistance
        val SCALE: Attribute = instance.scale
        val STEP_HEIGHT: Attribute = instance.stepHeight
        val GRAVITY: Attribute = instance.gravity
        val JUMP_STRENGTH: Attribute = instance.jumpStrength
        val BURNING_TIME: Attribute = instance.burningTime
        val EXPLOSION_KNOCKBACK_RESISTANCE: Attribute = instance.explosionKnockbackResistance
        val MOVEMENT_EFFICIENCY: Attribute = instance.movementEfficiency
        val OXYGEN_BONUS: Attribute = instance.oxygenBonus
        val WATER_MOVEMENT_EFFICIENCY: Attribute = instance.waterMovementEfficiency
        val TEMPT_RANGE: Attribute = instance.temptRange
        val BLOCK_INTERACTION_RANGE: Attribute = instance.blockInteractionRange
        val ENTITY_INTERACTION_RANGE: Attribute = instance.entityInteractionRange
        val BLOCK_BREAK_SPEED: Attribute = instance.blockBreakSpeed
        val MINING_EFFICIENCY: Attribute = instance.miningEfficiency
        val SNEAKING_SPEED: Attribute = instance.sneakingSpeed
        val SUBMERGED_MINING_SPEED: Attribute = instance.submergedMiningSpeed
        val SWEEPING_DAMAGE_RATIO: Attribute = instance.sweepingDamageRatio
        val SPAWN_REINFORCEMENTS: Attribute = instance.spawnReinforcements

        val Attribute.key_
            get() = instance.getKey(this)

        fun Attribute.of(name: String) = instance.of(name)

    }

}