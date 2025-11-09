package io.github.rothes.esu.core.module

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.MultiLangConfiguration
import io.github.rothes.esu.core.user.User
import org.incendo.cloud.CloudCapability
import org.incendo.cloud.Command
import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.internal.CommandNode
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import java.lang.reflect.ParameterizedType

abstract class CommonFeature<C, L> : Feature<C, L> {

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
        var clazz: Class<*> = javaClass
        while (clazz.genericSuperclass !is ParameterizedType) {
            clazz = clazz.superclass
            if (clazz === Object::class.java) {
                error("Cannot find config/lang classes of feature $name")
            }
        }
        val actualTypeArguments = (clazz.genericSuperclass as ParameterizedType).actualTypeArguments
        @Suppress("UNCHECKED_CAST")
        configClass = actualTypeArguments[0] as Class<C>
        @Suppress("UNCHECKED_CAST")
        langClass = actualTypeArguments[1] as Class<L>
    }

    private var configValue: C? = null

    final override val config: C
        get() = configValue ?: error("Config is not loaded for feature $name")
    final override val lang: MultiLangConfiguration<L> = MultiLangConfiguration(mutableMapOf())

    override val permissionNode: String by lazy { (parent?.permissionNode ?: EsuCore.instance.basePermissionNode) + "." + name.lowercase() }

    override fun onDisable() {
        unregisterCommands()
    }

    final override fun setConfigInstance(instance: C) {
        configValue = instance
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

    protected val registeredCommands = LinkedHashSet<Command<out User>>()

    protected fun unregisterCommands() {
        with(EsuCore.instance.commandManager) {
            registeredCommands.forEach {
                val components = it.components()
                if (components.size == 1) {
                    if (hasCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION))
                        deleteRootCommand(it.rootComponent().name())
                } else {
                    @Suppress("UNCHECKED_CAST")
                    var node = commandTree().rootNode() as CommandNode<User>
                    for (component in components) {
                        node = node.getChild(component as CommandComponent<User>) ?: return@forEach
                    }
                    var parent = node.parent()!!
                    parent.removeChild(node)
                    while (parent.children().isEmpty() && parent.command() == null) {
                        val p = parent.parent() ?: break
                        p.removeChild(parent)
                        parent = p
                    }
                }
            }
            registeredCommands.clear()
        }
    }

    fun registerCommand(block: CommandManager<User>.() -> Command.Builder<User>) {
        with(EsuCore.instance.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun registerCommands(obj: Any, modifier: ((AnnotationParser<User>) -> Unit)? = null) {
        with(EsuCore.instance.commandManager) {
            val annotationParser = AnnotationParser(this, User::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                val perm = if (a.value.isNotEmpty()) "command.${a.value}" else "command"
                b.permission(perm(perm))
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
        }
    }

}