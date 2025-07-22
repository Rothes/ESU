package io.github.rothes.esu.bukkit.module.newbieprotect.v1_21_3

import io.github.rothes.esu.bukkit.module.newbieprotect.EntityAttribute
import org.bukkit.attribute.Attribute

class EntityAttributeImpl: EntityAttribute {

    override val FLYING_SPEED: Attribute = Attribute.FLYING_SPEED
    override val MOVEMENT_SPEED: Attribute = Attribute.MOVEMENT_SPEED
    override val FOLLOW_RANGE: Attribute = Attribute.FOLLOW_RANGE

}