/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import org.incendo.cloud.caption.Caption
import org.incendo.cloud.caption.CaptionProvider
import org.incendo.cloud.caption.ConstantCaptionProvider
import org.incendo.cloud.caption.DelegatingCaptionProvider
import java.lang.reflect.Type
import java.util.*
import java.util.function.Predicate

object CaptionSerializer: ScalarSerializer<Caption>(Caption::class.java) {

    private val field = EsuCore.instance.commandManager.captionRegistry()::class.java.getDeclaredField("providers").also {
        it.isAccessible = true
    }

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Caption {
        val key = obj.toString()
        // Find the instance from command manager
        val captionRegistry = EsuCore.instance.commandManager.captionRegistry()
        @Suppress("UNCHECKED_CAST")
        val providers = field[captionRegistry] as LinkedList<CaptionProvider<*>>
        providers.forEach {
            ((it as? DelegatingCaptionProvider<*>)?.delegate() as? ConstantCaptionProvider)?.let { provider ->
                provider.captions().keys
                    .find { caption ->
                        caption.key() == key
                    }?.let { caption ->
                        return caption
                    }
            }
        }
        // Create one if not exists
        return Caption.of(key)
    }

    override fun serialize(caption: Caption, typeSupported: Predicate<Class<*>>): String {
        return caption.key()
    }

}
