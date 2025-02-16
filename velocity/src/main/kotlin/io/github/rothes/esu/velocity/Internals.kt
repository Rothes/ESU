package io.github.rothes.esu.velocity

import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.velocity.user.PlayerUser
import io.github.rothes.esu.velocity.user.VelocityUserManager
import java.util.*

val plugin: EsuPluginVelocity
    get() = EsuCore.instance as EsuPluginVelocity

val Player.user: PlayerUser
    get() = VelocityUserManager[this]
val UUID.playerUser: PlayerUser
    get() = VelocityUserManager[this]