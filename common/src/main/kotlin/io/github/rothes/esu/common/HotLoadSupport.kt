package io.github.rothes.esu.common

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.extension.writeAscii
import io.github.rothes.esu.lib.configurate.yaml.internal.snakeyaml.emitter.Emitter
import kotlinx.io.Buffer
import kotlinx.io.copyTo
import org.incendo.cloud.parser.flag.FlagContext
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.io.ByteArrayOutputStream

private const val TMP_DATA_VERSION = 2

open class HotLoadSupport(
    val isHot: Boolean,
) {
//    private val dataFile = EsuBootstrap.instance.baseConfigPath().resolve("hot-data.tmp")

    fun onEnable() {
        loadCriticalClasses()
//        if (isHot) {
//            try {
//                if (!dataFile.exists()) return
//                dataFile.inputStream().asSource().buffered().use { buffer ->
//                    require(buffer.readInt() == TMP_DATA_VERSION) { "Different hot data version." }
//                }
//                dataFile.deleteIfExists()
//            } catch (e: Throwable) {
//                EsuBootstrap.instance.err("Failed to read hot-data", e)
//            }
//        }
    }

    fun onDisable() {
//        if (isHot) {
//            try {
//                val buffer = Buffer()
//                buffer.writeInt(TMP_DATA_VERSION)
//                dataFile.outputStream(StandardOpenOption.CREATE).use {
//                    buffer.copyTo(it)
//                }
//                dataFile.toFile().deleteOnExit()
//            } catch (e: Throwable) {
//                EsuBootstrap.instance.err("Failed to save hot-data", e)
//            }
//        }
    }

    private fun loadCriticalClasses() {
        // Load the classes those are easily to break the hot plugin update.
        Emitter::class.java.declaredClasses // This may cause break when empty data loaded and saving with flow node
        FlagContext::class.java.toString()
        Charsets::class.java.toString()
        UpdateStatement::class.java.toString()
        loadClass("io/github/rothes/esu/common/util/extension/CommandManagersKt")
        // For JobSupport.nameString(), coroutine exception on shutdown
        loadClasses("kotlinx.coroutines.DebugKt", "kotlinx.coroutines.DebugStringsKt")

        // Velocity ServerUtils support
        // Throws NoClassDefFoundError onDisable if these are not loaded
        Buffer().apply {
            writeAscii("Load classes")
            copyTo(ByteArrayOutputStream())
        }
        // For Dispatchers.shutdown()
        loadClasses(
            "kotlinx/coroutines/ThreadLocalEventLoop", "kotlinx/coroutines/EventLoop_commonKt",
            "kotlinx/coroutines/AbstractTimeSourceKt", $$"kotlinx/coroutines/scheduling/CoroutineScheduler$Worker"
        )
    }

    private fun loadClass(clazz: String) {
        try {
            Class.forName(clazz.replace('/', '.'))
        } catch (_: ClassNotFoundException) {
            EsuBootstrap.instance.warn("HotLoadSupport - Class $clazz not found")
        }
    }

    private fun loadClasses(vararg classes: String) {
        classes.forEach { loadClass(it) }
    }

}