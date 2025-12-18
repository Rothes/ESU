package io.github.rothes.esu.bukkit.util.version.adapter.nms

interface EntityHandleGetter {

    /**
     * Get Nms handle without folia region thread check
     *
     * */
    fun getHandle(entity: org.bukkit.entity.Entity): net.minecraft.world.entity.Entity

}