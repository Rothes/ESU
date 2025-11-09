package io.github.rothes.esu.core.user

import io.github.rothes.esu.core.util.InitOnce
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class UserManager<T, R: User> {

    private val byUuid = hashMapOf<UUID, R>()
    private val lock = ReentrantReadWriteLock()

    fun getUsers(): Iterable<R> = lock.read { byUuid.values }

    protected fun set(uuid: UUID, value: R) = lock.write { byUuid.put(uuid, value) }

    abstract operator fun get(native: T): R
    operator fun get(uuid: UUID): R = lock.read { byUuid[uuid] } ?: lock.write { create(uuid).also { byUuid[uuid] = it } }

    fun getCache(uuid: UUID): R? = lock.read { byUuid[uuid] }
    fun getWithoutCaching(uuid: UUID): R = getCache(uuid) ?: create(uuid)

    abstract fun create(uuid: UUID): R

    abstract fun unload(native: T): R?
    fun unload(uuid: UUID): R? = lock.write { byUuid.remove(uuid) }
    fun unload(user: R): R? = unload(user.uuid)


    companion object {

        var instance: UserManager<*, out User> by InitOnce()

    }

}