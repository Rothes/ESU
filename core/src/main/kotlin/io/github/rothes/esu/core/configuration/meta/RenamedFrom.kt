package io.github.rothes.esu.core.configuration.meta

@Target(AnnotationTarget.FIELD)
annotation class RenamedFrom(
    val oldName: String = "",
)
