package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.core.user.User

data class ParsedAction(
    val action: Action,
    val argument: String?,
) {
    fun handle(user: User) {
        action.handle(user, argument)
    }
}