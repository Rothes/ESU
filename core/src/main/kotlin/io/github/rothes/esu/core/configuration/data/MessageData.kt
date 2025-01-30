package io.github.rothes.esu.core.configuration.data

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.user.User
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop.source
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

const val CHAT = "chat"
const val ACTIONBAR = "actionbar"
const val TITLE = "title"
const val SUBTITLE = "subtitle"
const val SOUND = "sound"

data class MessageData(
    val chat: String? = null,
    val actionBar: String? = null,
    val title: TitleData? = null,
    val sound: SoundData? = null,
    val pattern: String? = null,
) {

    constructor(pattern: String) : this(parse(pattern))
    constructor(copy: MessageData) : this(copy.chat, copy.actionBar, copy.title, copy.sound, copy.pattern)

    fun parsed(user: User, vararg params: TagResolver): ParsedMessageData {
        return ParsedMessageData(
            chat?.let { user.buildMinimessage(it, params = params) },
            actionBar?.let { user.buildMinimessage(it, params = params) },
            title?.parsed(user, params = params),
            sound,
        )
    }

    val string
        get() = pattern ?: buildString {
            fun tag(main: String, off: String? = null) {
                append('<').append(main)
                if (off != null) {
                    append(':').append(off)
                }
                append('>')
            }
            if (chat != null) {
                append(chat)
            }
            if (actionBar != null) {
                tag(ACTIONBAR)
                append(actionBar)
            }
            if (title != null) {
                var titleTime = true
                if (title.title != null) {
                    tag(TITLE,
                        if (titleTime) {
                            titleTime = false
                            title.times?.string
                        } else null
                    )
                    append(title.title)
                }
                if (title.subTitle != null) {
                    tag(
                        SUBTITLE,
                        if (titleTime) {
                            titleTime = false
                            title.times?.string
                        } else null
                    )
                    append(title.subTitle)
                }
            }
            if (sound != null) {
                tag(SOUND, sound.string)
            }
        }

    companion object {

        val String.message
            get() = parse(this)

        fun parse(string: String): MessageData {
            val builder = MessageDataBuilder(string)
            var messageType = MessageType.CHAT
            var handled = false // This flag is for fixing empty chat message if set other message type at start

            val buffer = StringBuilder()
            val tagBuffer = StringBuilder()

            var pointer = 0
            val length = string.length
            while (pointer < length) {
                var char = string[pointer++]
                var escaped = false
                // check for escape char
                if (char == '\\' && pointer < length) {
                    buffer.append(char) // Add current one
                    char = string[pointer++]
                    escaped = true
                }
                if (!escaped && char == '>') {
                    val tag = tagBuffer.toString()
                    val split = tag.split(':', limit = 2)
                    val main = split[0].lowercase()
                    val off = split.getOrElse(1) { "" }

                    val type = messageType
                    var matches = false
                    when (main) {
                        CHAT -> {
                            messageType = MessageType.CHAT
                            matches = true
                        }
                        ACTIONBAR -> {
                            messageType = MessageType.ACTIONBAR
                            matches = true
                        }
                        TITLE     -> {
                            messageType = MessageType.TITLE
                            parseTitleTimes(builder, off)
                            matches = true
                        }
                        SUBTITLE -> {
                            messageType = MessageType.SUBTITLE
                            parseTitleTimes(builder, off)
                            matches = true
                        }
                        SOUND              -> {
                            val split = off.split(':')
                            if (split.size < 2) {
                                EsuCore.instance.err("Failed to parse sound: At least namespace + key arguments provided")
                            } else {
                                val namespace = split[0]
                                val key = split[1]
                                val source = if (split.size > 2) Sound.Source.valueOf(split[2].uppercase()) else null
                                val volume = if (split.size > 3) split[3].toFloat() else null
                                val pitch = if (split.size > 4) split[4].toFloat() else null
                                val seed = if (split.size > 5) split[5].toLong() else null
                                builder.sound = SoundData(namespace, key, source, volume, pitch, seed)
                                matches = true
                            }
                        }
                    }
                    if (matches) {
                        val string = buffer.toString().dropLast(tagBuffer.length + 1)
                        if (handled || string.isNotEmpty()) {
                            setMessage(type, builder, string)
                        }
                        buffer.clear()
                        tagBuffer.clear()
                        handled = true
                        continue
                    }
                }
                buffer.append(char)
                tagBuffer.append(char)
                if (!escaped && char == '<') {
                    tagBuffer.clear()
                }
            }

            if (handled || buffer.isNotEmpty()) {
                setMessage(messageType, builder, buffer.toString())
            }
            return builder.messageData
        }


        private val TitleData.Times.string
            get() = "${fadeIn.toKotlinDuration()}:${stay.toKotlinDuration()}:${fadeOut.toKotlinDuration()}"

        private val SoundData.string
            get() =  if (seed != null)   "$namespace:$key:${source!!.name.lowercase()}:$volume:$pitch:$seed"
            else if (pitch != null)  "$namespace:$key:${source!!.name.lowercase()}:$volume:$pitch"
            else if (volume != null) "$namespace:$key:${source!!.name.lowercase()}:$volume"
            else if (source != null) "$namespace:$key:${source.name.lowercase()}"
            else "$namespace:$key"

        private fun parseTitleTimes(builder: MessageDataBuilder, string: String) {
            if (string.isEmpty())
                return
            val split = string.split(':')
            if (split.isNotEmpty()) {
                if (split.size != 3) {
                    EsuCore.instance.err("Failed to parse title times: Exactly 3 arguments required; Your input is '$string'")
                } else {
                    val map = split.map {
                        val ticks = it.toLongOrNull()
                        if (ticks != null)
                            (ticks * 50).milliseconds.toJavaDuration()
                        else
                            Duration.parse(it).toJavaDuration()
                    }
                    builder.titleTimes = TitleData.Times(map[0], map[1], map[2])
                }
            }
        }


        private fun setMessage(messageType: MessageType, builder: MessageDataBuilder, message: String) {
            when (messageType) {
                MessageType.CHAT -> builder.chat = message
                MessageType.ACTIONBAR -> builder.actionbar = message
                MessageType.TITLE -> builder.title = message
                MessageType.SUBTITLE -> builder.subTitle = message
            }
        }

        private data class MessageDataBuilder(
            val pattern: String,
            var chat: String? = null,
            var actionbar: String? = null,
            var title: String? = null,
            var subTitle: String? = null,
            var titleTimes: TitleData.Times? = null,
            var sound: SoundData? = null,
        ) {
            val messageData
                get() = MessageData(chat, actionbar,
                    if (title != null || subTitle != null || titleTimes != null)
                        TitleData(title, subTitle, titleTimes)
                    else null,
                    sound, pattern)
        }

        private enum class MessageType {
            CHAT,
            ACTIONBAR,
            TITLE,
            SUBTITLE,
        }
    }
}
