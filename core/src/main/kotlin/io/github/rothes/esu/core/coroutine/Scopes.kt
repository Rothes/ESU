package io.github.rothes.esu.core.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object AsyncScope : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default

}