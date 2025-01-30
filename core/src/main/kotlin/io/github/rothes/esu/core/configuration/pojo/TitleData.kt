package io.github.rothes.esu.core.configuration.pojo

import io.github.rothes.esu.core.user.User
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.time.Duration as KtDuration

data class TitleData(
    val title: String? = null,
    val subTitle: String? = null,
    val times: Times? = null,
) {

    fun parse(user: User, vararg params: TagResolver): ParsedMessageData.ParsedTitleData {
        return ParsedMessageData.ParsedTitleData(
            title?.let { user.buildMinimessage(it, *params) },
            subTitle?.let { user.buildMinimessage(it, *params) },
            times
        )
    }

    data class Times(
        val fadeIn: Duration = Duration.ofMillis(500),
        val stay: Duration = Duration.ofMillis(3500),
        val fadeOut: Duration = Duration.ofMillis(1000),
    ) {
        constructor(fadeIn: KtDuration, stay: KtDuration, fadeOut: KtDuration) : this(fadeIn.toJavaDuration(), stay.toJavaDuration(), fadeOut.toJavaDuration())

        val adventure by lazy { Title.Times.times(fadeIn, stay, fadeOut) }

        companion object {
            fun ofTicks(fadeIn: Long, stay: Long, fadeOut: Long) = Times((fadeIn * 50).milliseconds, (stay * 50).milliseconds, (fadeOut * 50).milliseconds)
        }
    }
}
