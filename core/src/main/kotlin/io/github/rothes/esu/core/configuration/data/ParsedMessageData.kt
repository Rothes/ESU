package io.github.rothes.esu.core.configuration.data

import net.kyori.adventure.text.Component

data class ParsedMessageData(
    val chat: Component? = null,
    val actionBar: Component? = null,
    val title: ParsedTitleData? = null,
    val sound: SoundData? = null,
) {
    data class ParsedTitleData(
        val title: Component? = null,
        val subTitle: Component? = null,
        val times: TitleData.Times? = null,
    )
}
