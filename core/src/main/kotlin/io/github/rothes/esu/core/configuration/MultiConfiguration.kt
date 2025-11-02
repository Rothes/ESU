package io.github.rothes.esu.core.configuration

open class MultiConfiguration<T>(
    val configs: Map<String, T>
) {
    open fun <R> get(key: String?, block: (T) -> R?): R? {
        return configs[key]?.let(block) ?: configs.values.firstNotNullOfOrNull { block(it) }
    }

    override fun toString(): String {
        return "MultiConfiguration(configs=$configs)"
    }

}