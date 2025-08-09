package io.github.rothes.esu.core.configuration.meta

const val OVERRIDE_ALWAYS = "<ESU_ALWAYS_OVERRIDE>"

@Target(AnnotationTarget.FIELD)
annotation class Comment(
    val value: String = "",
    val overrideOld: Array<String> = [],
)
