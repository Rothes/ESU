package io.github.rothes.esu.core.util.function

@FunctionalInterface
fun interface IntConsumer2 {

    operator fun invoke(first: Int, second: Int)

}