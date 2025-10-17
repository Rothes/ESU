package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.lib.configurate.BasicConfigurationNode
import io.github.rothes.esu.lib.configurate.ConfigurationNode
import io.github.rothes.esu.lib.configurate.ConfigurationOptions
import io.github.rothes.esu.lib.configurate.NodePath
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import io.github.rothes.esu.lib.configurate.serialize.TypeSerializer
import io.leangen.geantyref.GenericTypeReflector
import java.lang.reflect.AnnotatedType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

object MapSerializer: TypeSerializer<Map<*, *>> {

    private const val DEFAULTED_KEY = "_default"

    override fun deserialize(type: Type, node: ConfigurationNode): MutableMap<*, *> {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for collections")
        }
        if (type.actualTypeArguments.size != 2) {
            throw SerializationException(type, "Map expected two type arguments!")
        }
        val key = type.actualTypeArguments[0]
        val value = type.actualTypeArguments[1]
        val keySerializer = node.options().serializers()[key] ?: throw SerializationException(
            type, "No type serializer available for key type $key"
        )
        val valueSerializer = node.options().serializers()[value] ?: throw SerializationException(
            type, "No type serializer available for value type $value"
        )
        @Suppress("UNCHECKED_CAST")
        return (
                when (GenericTypeReflector.erase(type)) {
                    DefaultedEnumMap::class.java       -> createDefaultedEnumMap(asEnumClass(key))
                    DefaultedLinkedHashMap::class.java -> DefaultedLinkedHashMap(null)
                    EnumMap::class.java                -> createEnumMap(asEnumClass(key))
                    else                               -> LinkedHashMap()
                } as MutableMap<Any, Any>
            ).apply {
                if (!node.isMap)
                    return@apply

                val keyNode = BasicConfigurationNode.root(node.options())

                val defNode = node.node(DEFAULTED_KEY)
                if (this is Defaulted<*>) {
                    (this as Defaulted<Any>).default = deserialize(value, valueSerializer, "default", defNode, defNode.path())
                }
                for ((k, v) in node.childrenMap()) {
                    if (this is Defaulted<*>) {
                        if (k is String && k == DEFAULTED_KEY) {
                            continue
                        }
                        v.mergeFrom(defNode)
                    }
                    val deserializedKey = deserialize(key, keySerializer, "key", keyNode.set(k), node.path()) ?: continue
                    val deserializedValue = deserialize(value, valueSerializer, "value", v, v!!.path()) ?: continue
                    put(deserializedKey, deserializedValue)
                }
            }
    }

    private fun <T: Enum<T>> createDefaultedEnumMap(clazz: Class<T>) = DefaultedEnumMap<T, Any>(clazz, null)
    private fun <T: Enum<T>> createEnumMap(clazz: Class<T>) = EnumMap<T, Any>(clazz)
    private fun asEnumClass(type: Type) = GenericTypeReflector.erase(type).asSubclass(Enum::class.java)

    private fun deserialize(
        type: Type, serializer: TypeSerializer<*>, mapPart: String, node: ConfigurationNode, path: NodePath
    ): Any? {
        try {
            return serializer.deserialize(type, node)
        } catch (ex: SerializationException) {
            ex.initPath { node.path() }
            EsuCore.instance.err("Could not deserialize $mapPart ${node.raw()} into $type at $path: ${ex.rawMessage()}")
        }
        return null
    }

    override fun serialize(type: Type, obj: Map<*, *>?, node: ConfigurationNode) {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for collections")
        }
        if (type.actualTypeArguments.size != 2) {
            throw SerializationException(type, "Map expected two type arguments!")
        }
        val key = type.actualTypeArguments[0]
        val value = type.actualTypeArguments[1]
        val keySerializer = node.options().serializers()[key] ?: throw SerializationException(
            type, "No type serializer available for key type $key"
        )
        val valueSerializer = node.options().serializers()[value] ?: throw SerializationException(
            type, "No type serializer available for value type $value"
        )

        if (obj !is Defaulted<*> && (obj == null || obj.isEmpty())) {
            node.set(emptyMap<Any, Any>())
        } else {
            if (node.empty()) {
                node.raw(emptyMap<Any, Any>())
            }

            val defNode = node.node(DEFAULTED_KEY)
            if (obj is Defaulted<*>) {
                serialize(value, valueSerializer, obj.default, "default", defNode, defNode.path())
            }
            val keyNode = BasicConfigurationNode.root(node.options())
            for ((key1, value1) in obj) {
                if (!serialize(key, keySerializer, key1, "key", keyNode, node.path())) {
                    continue
                }
                val keyObj = Objects.requireNonNull(keyNode.raw(), "Key must not be null!")
                val child = node.node(keyObj)
                serialize(value, valueSerializer, value1, "value", child, child.path())

                if (obj is Defaulted<*>) {
                    cleanSameValueChildrenNodes(child, defNode)
                }
            }
        }
    }

    private fun serialize(type: Type, serializer: TypeSerializer<*>, obj: Any?, mapPart: String, node: ConfigurationNode, path: NodePath): Boolean {
        try {
            @Suppress("UNCHECKED_CAST")
            (serializer as TypeSerializer<Any>).serialize(type, obj, node)
            return true
        } catch (ex: SerializationException) {
            ex.initPath { node.path() }
            EsuCore.instance.err("Could not serialize $mapPart $obj from $type at $path: ${ex.rawMessage()}")
        }
        return false
    }

    private fun cleanSameValueChildrenNodes(toClean: ConfigurationNode, compare: ConfigurationNode) {
        val childrenMap = compare.childrenMap()
        if (childrenMap.isEmpty()) {
            if (toClean.raw() == compare.raw()) {
                toClean.raw(null)
            }
        } else {
            for ((key, node) in compare.childrenMap()) {
                val n = toClean.node(key)
                cleanSameValueChildrenNodes(n, node)
                if (n.empty()) {
                    toClean.removeChild(key)
                }
            }
        }
    }

    override fun emptyValue(specificType: AnnotatedType, options: ConfigurationOptions?): Map<*, *>? {
        if (specificType.isAnnotationPresent(NoDeserializeNull::class.java)) {
            return null
        }
        return LinkedHashMap<Any, Any>()
    }

    interface Defaulted<V> {
        var default: V?
    }

    class DefaultedLinkedHashMap<K, V>(override var default: V?): LinkedHashMap<K, V>(), Defaulted<V> {

        override fun get(key: K) = getOrDefault(key)
        fun getOrDefault(key: K): V? = super.get(key) ?: default

    }
    class DefaultedEnumMap<K: Enum<K>, V>(keyType : Class<K>, override var default: V?): EnumMap<K, V>(keyType), Defaulted<V> {

        override fun get(key: K) = getOrDefault(key)
        fun getOrDefault(key: K): V? = super.get(key) ?: default

    }

}