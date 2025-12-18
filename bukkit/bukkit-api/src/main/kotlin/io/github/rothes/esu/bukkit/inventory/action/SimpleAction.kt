package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.core.user.User

abstract class SimpleAction(override val name: String) : Action {

    override fun handle(user: User, argument: String?) {
        handle(user)
    }
    abstract fun handle(user: User)

    override fun toString(): String {
        return "SimpleAction(name='$name')"
    }

    companion object {
        fun create(name: String, func: (User) -> Unit) = object : SimpleAction(name) {
            override fun handle(user: User) {
                func(user)
            }
        }
    }

}