package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.util.InitOnce
import java.util.*

abstract class UserManager<T, R: User> {

    protected val byUuid = hashMapOf<UUID, R>()

    abstract operator fun get(native: T): R
    operator fun get(uuid: UUID): R = byUuid[uuid] ?: create(uuid).also { byUuid[uuid] = it }

    fun getCache(uuid: UUID): R? = byUuid[uuid]
    fun getWithoutCaching(uuid: UUID): R = byUuid[uuid] ?: create(uuid)

    abstract fun create(uuid: UUID): R

    abstract fun unload(native: T): R?
    fun unload(uuid: UUID): R? = byUuid.remove(uuid)
    fun unload(user: R): R? = unload(user.uuid)


    companion object {

        var instance: UserManager<*, out User> by InitOnce()

    }

}