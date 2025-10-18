package io.github.rothes.esu.core.util.extension

inline fun <T> T.ifLet(condition: Boolean, block: T.() -> T): T {
    return if (condition) block(this) else this
}

inline fun <T> T.ifLet(condition: () -> Boolean, block: T.() -> T): T {
    return ifLet(condition(), block)
}