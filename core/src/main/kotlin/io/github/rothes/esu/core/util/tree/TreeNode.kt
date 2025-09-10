package io.github.rothes.esu.core.util.tree

class TreeNode<T> private constructor(val parent: TreeNode<T>?, val key: String?) {

    constructor(): this(null, null)

    private val map = hashMapOf<String, TreeNode<T>>()

    val path: String = buildString {
        var node: TreeNode<T>? = this@TreeNode
        while (node?.key != null) {
            insert(0, '/')
            insert(0, node.key)
            node = node.parent
        }
    }
    var value: T? = null

    fun getNode(name: String): TreeNode<T>? {
        return map[name]
    }

    fun getNode(path: List<String>): TreeNode<T>? {
        var node = this
        for (name in path) {
            node = node.getNode(name) ?: return null
        }
        return node
    }

    fun getOrCreateNode(name: String): TreeNode<T> {
        return map.getOrPut(name) { TreeNode(this, name) }
    }

    fun getOrCreateNode(name: String, value: T): TreeNode<T> {
        return getOrCreateNode(name).also { it.value = value }
    }

    fun getOrCreateNode(path: List<String>): TreeNode<T> {
        var node = this
        for (name in path) {
            node = node.getOrCreateNode(name)
        }
        return node
    }

    override fun toString(): String {
        return "TreeNode(key=$key, map=$map, value=$value)"
    }

}