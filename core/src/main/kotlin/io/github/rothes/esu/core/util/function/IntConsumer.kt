package io.github.rothes.esu.core.util.function

@FunctionalInterface
fun interface IntConsumer {

    operator fun invoke(value: Int)

}