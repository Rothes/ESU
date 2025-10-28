package io.github.rothes.esu.common.util.extension

import org.incendo.cloud.CommandManager

fun CommandManager<*>.shutdown() {
    rootCommands().forEach { deleteRootCommand(it) }
}