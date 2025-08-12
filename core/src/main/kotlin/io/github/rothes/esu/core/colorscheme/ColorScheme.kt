package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.NamedTextColor
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.Style
import io.github.rothes.esu.lib.net.kyori.adventure.text.format.TextColor
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.Context
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.internal.serializer.SerializableResolver
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.internal.serializer.StyleClaim
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.internal.serializer.TokenEmitter
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.Tag
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import io.github.rothes.esu.lib.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * A Color Scheme holds the colors for the color tags.
 */
data class ColorScheme(
    val primary: TextColor = hex("#c8b3fd"),
    val primaryDim: TextColor = hex("#af92f6"),
    val secondary: TextColor = hex("#cfa0f3"),
    val secondaryDim: TextColor = hex("#bc83e7"),
    val tertiary: TextColor = hex("#c0b4cf"),
    val tertiaryDim: TextColor = hex("#ada5b8"),
    val valuePositive: TextColor = hex("#36c450"),
    val valuePositiveDim: TextColor = hex("#379a49"),
    val valueNegative: TextColor = hex("#ff6e4e"),
    val valueNegativeDim: TextColor = hex("#f4532f"),
    val error: TextColor = hex("#ff6666"),
    val errorDim: TextColor = hex("#ff5050"),
): ConfigurationPart {

    @Suppress("SpellCheckingInspection")
    private val colors by lazy {
        listOf(
            primary to "pc",
            primaryDim to "pdc",
            secondary to "sc",
            secondaryDim to "sdc",
            tertiary to "tc",
            tertiaryDim to "tdc",
            valuePositive to "vpc",
            valuePositiveDim to "vpdc",
            valueNegative to "vnc",
            valueNegativeDim to "vndc",
            error to "ec",
            errorDim to "edc",
        )
    }

    @Suppress("SpellCheckingInspection")
    val tagResolver: TagResolver by lazy {
        TagResolver.builder()
            .color(primary, "pc", "primary_color")
            .color(primaryDim, "pdc", "primary_dim_color")
            .color(secondary, "sc", "secondary_color")
            .color(secondaryDim, "sdc", "secondary_dim_color")
            .color(tertiary, "tc", "tertiary_color")
            .color(tertiaryDim, "tdc", "tertiary_dim_color")
            .color(valuePositive, "vpc", "value_positive_color")
            .color(valuePositiveDim, "vpdc", "value_positive_dim_color")
            .color(valueNegative, "vnc", "value_negative_color")
            .color(valueNegativeDim, "vndc", "value_negative_dim_color")
            .color(error, "ec", "error_color")
            .color(errorDim, "edc", "error_dim_color")
            .resolver(ColorTagResolver())
            .build()
    }

    private fun TagResolver.Builder.color(color: TextColor, vararg keys: String): TagResolver.Builder {
        resolver(EsuColorTagResolver(keys.toList(), color))
        return this
    }

    companion object {
        private fun hex(hexString: String): TextColor = TextColor.fromHexString(hexString)!!
    }

    private class EsuColorTagResolver(
        val keys: List<String>,
        val color: TextColor,
    ): TagResolver {

        override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context): Tag? {
            return if (has(name)) Tag.styling(color) else null
        }

        override fun has(name: String): Boolean {
            return keys.find { it.equals(name, ignoreCase = true) } != null
        }

    }

    private inner class ColorTagResolver: TagResolver, SerializableResolver.Single {

        override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context) = null
        override fun has(name: String): Boolean = false

        override fun claimStyle(): StyleClaim<*>? {
            return StyleClaim.claim(
                "color", { obj: Style -> obj.color() }, { color: TextColor, emitter: TokenEmitter ->
                    val find = colors.find { it.first == color }
                    if (find != null)
                        emitter.tag(find.second)
                    // adventure 4.23.0
                    else if (color is NamedTextColor)
                        emitter.tag(NamedTextColor.NAMES.key(color)!!)
                    else
                        emitter.tag(color.asHexString())
                })
        }

    }

}