package io.github.rothes.esu.core.configuration.pojo

import net.kyori.adventure.title.Title
import java.time.Duration

data class TitleData(
    val title: String? = null,
    val subTitle: String? = null,
    val times: Times? = null,
) {

    data class Times(
        val fadeIn: Duration = Duration.ofMillis(500),
        val stay: Duration = Duration.ofMillis(3500),
        val fadeOut: Duration = Duration.ofMillis(1000),
    ) {
        val adventure by lazy { Title.Times.times(fadeIn, stay, fadeOut) }
    }
}
