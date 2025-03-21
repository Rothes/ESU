package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.configuration.ConfigurationPart
import net.kyori.adventure.text.format.StyleBuilderApplicable
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

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
    val tagResolver: TagResolver by lazy {
        TagResolver.builder()
            .styling(primary, "primary_color", "pc")
            .styling(primaryDim, "primary_dim_color", "pdc")
            .styling(secondary, "secondary_color", "sc")
            .styling(secondaryDim, "secondary_dim_color", "sdc")
            .styling(tertiary, "tertiary_color", "tc")
            .styling(tertiaryDim, "tertiary_dim_color", "tdc")
            .styling(valuePositive, "value_positive_color", "vpc")
            .styling(valuePositiveDim, "value_positive_dim_color", "vpdc")
            .styling(valueNegative, "value_negative_color", "vnc")
            .styling(valueNegativeDim, "value_negative_dim_color", "vndc")
            .styling(error, "error_color", "ec")
            .styling(errorDim, "error_dim_color", "edc")
            .build()
    }

    private fun TagResolver.Builder.styling(style: StyleBuilderApplicable, vararg keys: String): TagResolver.Builder {
        keys.forEach { key ->
            resolver(TagResolver.resolver(key, Tag.styling(style)))
        }
        return this
    }

    companion object {
        private fun hex(hexString: String): TextColor = TextColor.fromHexString(hexString)!!
    }

}