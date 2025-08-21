package io.github.rothes.esu.core.configuration.meta

@Target(AnnotationTarget.FIELD)
annotation class RemovedNode(
    val since: String = "",
)
