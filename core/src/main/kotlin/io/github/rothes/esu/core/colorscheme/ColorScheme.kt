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
    val primary: TextColor = hex("#cfa0f3"),
    val primaryVariant: TextColor = hex("#8d6acc"),
    val secondary: TextColor = hex("#6f4fab"),
    val secondaryVariant: TextColor = hex("#54398a"),
    val tertiary: TextColor = hex("#fecbe6"),
    val tertiaryVariant: TextColor = hex("#c09aae"),
    val error: TextColor = hex("#ff6666"),
    val errorVariant: TextColor = hex("#ff3333"),
): ConfigurationPart {

    @Transient val tagResolver: TagResolver = TagResolver.builder()
        .styling(primary, "primary_color", "pc")
        .styling(primaryVariant, "primary_variant_color", "pvc")
        .styling(secondary, "secondary_color", "sc")
        .styling(secondaryVariant, "secondary_variant_color", "svc")
        .styling(tertiary, "tertiary_color", "tc")
        .styling(tertiaryVariant, "tertiary_variant_color", "tvc")
        .styling(error, "error_color", "ec")
        .styling(error, "error_variant_color", "evc")
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