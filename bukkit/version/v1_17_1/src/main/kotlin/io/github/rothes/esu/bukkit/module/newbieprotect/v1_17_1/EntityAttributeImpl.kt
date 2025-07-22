package io.github.rothes.esu.bukkit.module.newbieprotect.v1_17_1

import io.github.rothes.esu.bukkit.module.newbieprotect.EntityAttribute
import org.bukkit.attribute.Attribute

class EntityAttributeImpl: EntityAttribute {

    override val FLYING_SPEED: Attribute = Attribute.GENERIC_FLYING_SPEED
    override val MOVEMENT_SPEED: Attribute = Attribute.GENERIC_MOVEMENT_SPEED
    override val FOLLOW_RANGE: Attribute = Attribute.GENERIC_FOLLOW_RANGE

}