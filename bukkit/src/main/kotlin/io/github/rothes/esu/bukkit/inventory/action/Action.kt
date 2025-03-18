package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.core.user.User

interface Action {

    val name: String
    val id: String
        get() = name.lowercase()

    fun handle(user: User, argument: String?)

}