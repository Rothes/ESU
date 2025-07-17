package io.github.rothes.esu.bukkit.module.chatantispam

import io.github.rothes.esu.bukkit.module.chatantispam.check.*

object ChecksMan {

    val checks = listOf(
        WhisperTargets, Muting, FixedRequest, IllegalCharacters, LongMessage, Frequency,
        SpacesFilter, LetterCaseFilter, RandomCharactersFilter, Similarity
    )

}