package io.github.rothes.esu.core

interface EsuLogger {

    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)
    fun err(message: String, throwable: Throwable?)

}