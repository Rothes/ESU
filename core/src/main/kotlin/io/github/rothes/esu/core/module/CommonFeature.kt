/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

abstract class CommonFeature<C, L> : Feature<C, L> {

    override val name: String = javaClass.simpleName.removeSuffix("Feature")
    final override var enabled: Boolean = false
        private set
    final override var parent: Feature<*, *>? = null
        private set
    override val module: Module<*, *>
        get() {
            var feat = parent
            while (feat?.parent != null) {
                feat = feat.parent
            }
            if (feat !is Module<*, *>) {
                error("Feature $name is not attached to a module")
            }
            return feat
        }

    final override val configClass: Class<C>
    final override val langClass: Class<L>
    init {
        var typeMap = HashMap<String, Type>(1)
        var clazz: Class<*> = javaClass
        while (true) {
            val type = clazz.genericSuperclass
            if (type is ParameterizedType) {
                if (type.actualTypeArguments.size == 2)
                    break
                typeMap[(type.rawType as Class<*>).typeParameters[0].name] = type.actualTypeArguments[0]
            }
            clazz = clazz.superclass
            if (clazz === Any::class.java) {
                @Suppress("USELESS_ELVIS") // In case of name is overridden, name is not init yet
                error("Cannot find config/lang classes of feature ${name ?: javaClass.simpleName}")
            }
        }
        val actualTypeArguments = (clazz.genericSuperclass as ParameterizedType).actualTypeArguments
        fun Type.actualClass(): Class<*> {
            return (if (this is TypeVariable<*>) typeMap[this.name] else this) as Class<*>
        }
        @Suppress("UNCHECKED_CAST")
        configClass = actualTypeArguments[0].actualClass() as Class<C>
        @Suppress("UNCHECKED_CAST")
        langClass = actualTypeArguments[1].actualClass() as Class<L>
    }

    private var _config: C? = null
    private var _lang: MultiLangConfiguration<L>? = null

    final override val config: C
        get() = _config ?: error("Config is not loaded for feature $name")
    final override val lang: MultiLangConfiguration<L>
        get() = _lang ?: error("Lang is not loaded for feature $name")

    override val permissionNode: String by lazy { (parent?.permissionNode ?: EsuCore.instance.baseCommandNode) + "." + name.lowercase() }

    override fun onDisable() {
        unregisterCommands()
    }

    final override fun setConfigInstance(instance: C?) {
        _config = instance
    }

    override fun setLangInstance(instance: MultiLangConfiguration<L>?) {
        _lang = instance
    }

    final override fun setEnabled(value: Boolean) {
        enabled = value
        super.setEnabled(value)
    }

    final override fun setParent(parent: Feature<*, *>?) {
        this.parent = parent
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
                    var node = commandTree().rootNode()
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

    fun registerCommandJvm(block: CommandManager<User>.() -> Command.Builder<User>) {
        with(EsuCore.instance.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun registerCommand(block: CommandManager<User>.() -> MutableCommandBuilder<User>) {
        with(EsuCore.instance.commandManager) {
            val command = block.invoke(this).build()
            command(command)
            registeredCommands.add(command)
        }
    }

    fun withCommandManager(scope: CommandManager<User>.() -> Unit) {
        scope(EsuCore.instance.commandManager)
    }

    fun Command<User>.regCmd() {
        EsuCore.instance.commandManager.command(this)
        registeredCommands.add(this)
    }

    fun MutableCommandBuilder<User>.regCmd() {
        val command = build()
        EsuCore.instance.commandManager.command(command)
        registeredCommands.add(command)
    }

    fun registerCommands(obj: Any, modifier: ((AnnotationParser<User>) -> Unit)? = null) {
        with(EsuCore.instance.commandManager) {
            val annotationParser = AnnotationParser(this, User::class.java).installCoroutineSupport()
            annotationParser.registerBuilderModifier(ShortPerm::class.java) { a, b ->
                b.permission(cmdShortPerm(a.value))
            }
            modifier?.invoke(annotationParser)

            val commands = annotationParser.parse(obj)
            registeredCommands.addAll(commands)
        }
    }

    protected fun cmdShortPerm(value: String = ""): String {
        val perm = if (value.isNotEmpty()) "command.${value}" else "command"
        return perm(perm)
    }

}