package io.github.rothes.esu.core.module

import java.nio.file.Path

interface Module<C, L>: Feature<C, L> {

    val moduleFolder: Path
    val configPath: Path
    val langPath: Path

    fun doReload() {
        onReload()
    }

}