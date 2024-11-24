package io.github.rothes.esu.core.colorscheme

import io.github.rothes.esu.core.configuration.ConfigurationPart
import net.kyori.adventure.text.format.StyleBuilderApplicable
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * A Color Scheme holds the colors for the color tags.
 */
data class ColorScheme(
    val primary: TextColor = hex("#c8b3fd"),
    val primaryDim: TextColor = hex("#ac98e0"),
    val secondary: TextColor = hex("#cfa0f3"),
    val secondaryDim: TextColor = hex("#b385ff"),
    val tertiary: TextColor = hex("#fecbe6"),
    val tertiaryDim: TextColor = hex("#e2b0ff"),
    val error: TextColor = hex("#ff6666"),
    val errorDim: TextColor = hex("#ff5050"),
): ConfigurationPart {

    @Transient val tagResolver: TagResolver = TagResolver.builder()
        .styling(primary, "primary_color", "pc")
        .styling(primaryDim, "primary_dim_color", "pdc")
        .styling(secondary, "secondary_color", "sc")
        .styling(secondaryDim, "secondary_dim_color", "sdc")
        .styling(tertiary, "tertiary_color", "tc")
        .styling(tertiaryDim, "tertiary_dim_color", "tdc")
        .styling(error, "error_color", "ec")
        .styling(errorDim, "error_dim_color", "edc")
        .build()

    private fun TagResolver.Builder.styling(style: StyleBuilderApplicable, vararg keys: String): TagResolver.Builder {
        keys.forEach { key ->
            resolver(Placeholder.styling(key, style))
        }
        return this
    }

    companion object {
        private fun hex(hexString: String): TextColor = TextColor.fromHexString(hexString)!!
    }

}