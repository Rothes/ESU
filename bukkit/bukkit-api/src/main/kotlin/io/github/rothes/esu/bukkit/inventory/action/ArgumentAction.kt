package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.core.user.User

abstract class ArgumentAction(override val name: String): Action {

    companion object {
        fun create(name: String, func: (User, String?) -> Unit) = object : ArgumentAction(name) {
            override fun handle(user: User, argument: String?) {
                func(user, argument)
            }
        }
    }
}