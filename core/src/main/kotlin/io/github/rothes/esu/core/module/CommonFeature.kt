package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.MultiLocaleConfiguration
import java.lang.reflect.ParameterizedType

abstract class CommonFeature<C: ConfigurationPart, L: ConfigurationPart> : Feature<C, L> {

    override val name: String = javaClass.simpleName.removeSuffix("Feature")
    final override var enabled: Boolean = false
        private set
    final override var parent: Feature<*, *>? = null
        private set
    protected var internalModule: Module<*, *>? = null
    override val module: Module<*, *>
        get() = internalModule ?: error("Feature $name is not attached to a module")

    final override val configClass: Class<C>
    final override val langClass: Class<L>
    init {
        val actualTypeArguments = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
        @Suppress("UNCHECKED_CAST")
        configClass = actualTypeArguments[0] as Class<C>
        @Suppress("UNCHECKED_CAST")
        langClass = actualTypeArguments[1] as Class<L>
    }

    final override lateinit var config: C
        protected set
    final override val lang: MultiLocaleConfiguration<L> = MultiLocaleConfiguration(mutableMapOf())

    final override fun setConfigInstance(instance: C) {
        config = instance
    }

    final override fun setEnabled(value: Boolean) {
        enabled = value
        super.setEnabled(value)
    }

    final override fun setParent(parent: Feature<*, *>?) {
        this.parent = parent
        var temp = parent
        while (temp?.parent != null) {
            temp = temp.parent
        }
        if (temp !is Module<*, *>) {
            temp = null
        }
        internalModule = temp
    }

    protected val children = linkedMapOf<String, Feature<*, *>>()

    override fun getFeatureMap(): Map<String, Feature<*, *>> {
        return children.toMap()
    }

    override fun getFeatures(): List<Feature<*, *>> {
        return children.values.toList()
    }

    override fun getFeature(name: String): Feature<*, *>? {
        return children[name.lowercase()]
    }

    override fun registerFeature(child: Feature<*, *>) {
        synchronized(children) {
            var feature: Feature<*, *> = this
            while (feature.parent != null) {
                feature = feature.parent!!
            }
            require(getFeature(child.name.lowercase()) == null) {
                "Duplicate child name ${child.name.lowercase()} for root feature ${feature.name}"
            }
            children[child.name.lowercase()] = child
            child.setParent(this)
        }
    }

}