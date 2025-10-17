package io.github.rothes.esu.core.configuration.data

import io.github.rothes.esu.lib.adventure.text.Component

data class ParsedMessageData(
    val chat: List<Component>? = null,
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
