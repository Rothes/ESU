package io.github.rothes.esu.common.util.coroutine

import io.github.rothes.esu.core.EsuBootstrap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlin.concurrent.thread

object CoroutineLife {

    fun shutdown() {
        thread(name = "Esu-Coroutine Shutdown Dispatcher #${EsuBootstrap.instance.hashCode()}", isDaemon = true) {
            try {
                @OptIn(DelicateCoroutinesApi::class) // Just release the resources
                Dispatchers.shutdown()
            } catch (t: Throwable) {
                EsuBootstrap.instance.warn("An exception occurred while shutting down coroutine: $t")
            }
        }
    }

}