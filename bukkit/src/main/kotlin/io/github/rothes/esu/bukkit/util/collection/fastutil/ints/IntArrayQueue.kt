package io.github.rothes.esu.bukkit.util.collection.fastutil.ints

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue

class IntArrayQueue : IntArrayFIFOQueue() {

    fun indexOf(x: Int) : Int {
        var v = 0
        var i = start
        while (i != end) {
            if (array[i] == x) return v
            if (++i == length) i = 0
            v++
        }
        return -1
    }

    fun dropFirst(v: Int) {
        start += v
        if (start >= length) start -= length
    }

}