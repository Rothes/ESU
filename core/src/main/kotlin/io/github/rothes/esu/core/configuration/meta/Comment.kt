package io.github.rothes.esu.core.configuration.meta

const val OVERRIDE_DISABLED = "<ESU_NO_OVERRIDE_VALUE>"
const val OVERRIDE_ALWAYS = "<ESU_ALWAYS_OVERRIDE>"

@Target(AnnotationTarget.FIELD)
annotation class Comment(
    val value: String = "",
    val overrideOld: String = OVERRIDE_DISABLED,
)
