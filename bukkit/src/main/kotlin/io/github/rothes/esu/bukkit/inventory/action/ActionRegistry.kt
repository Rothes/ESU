package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.plugin
import java.lang.reflect.Modifier

object ActionRegistry {

    private val registry = hashMapOf<String, Action>()

    init {
        CommonActions::class.java.declaredFields.filter {
            Action::class.java.isAssignableFrom(it.type) && it.modifiers and Modifier.STATIC != 0
        }.forEach {
            it.isAccessible = true
            register(it.get(null) as Action)
        }
    }

    fun register(vararg actions: Action) {
        actions.forEach { register(action = it) }
    }

    fun register(action: Action): Boolean {
        if (registry.containsKey(action.name)) {
            return false
        }
        registry[action.id.lowercase()] = action
        return true
    }

    fun unregister(action: Action): Boolean {
        return registry.remove(action.id, action)
    }

    fun parseActions(string: List<String>): List<ParsedAction> {
        return string.mapNotNull { parseAction(it) }
    }

    fun parseAction(string: String?): ParsedAction? {
        string ?: return null
        if (string.isEmpty() || string.first() != '[')
            return null
        val index = string.indexOf(']')
        if (index == -1)
            return null
        val name = string.substring(1, index).lowercase()
        val action = registry[name]
        if (action == null) {
            plugin.warn("Unknown action '$string'")
            return null
        }
        return ParsedAction(action, string.substring(index + 1).removePrefix(" ").ifEmpty { null })
    }

}