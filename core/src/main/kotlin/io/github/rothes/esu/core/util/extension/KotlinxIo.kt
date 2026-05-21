/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.util.extension

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import java.util.*

fun Sink.writeBool(value: Boolean) {
    writeByte(if (value) 1 else 0)
}

fun Source.readBool(): Boolean {
    return readByte() == 1.toByte()
}

fun Sink.writeUuid(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun Source.readUuid(): UUID {
    return UUID(readLong(), readLong())
}

fun Sink.writeVarBytes(bytes: ByteArray) {
    writeInt(bytes.size)
    write(bytes)
}

fun Source.readVarBytes(): ByteArray {
    return readByteArray(readInt())
}

fun Sink.writeAscii(string: String) {
    writeVarBytes(string.toByteArray(Charsets.US_ASCII))
}

fun Source.readAscii(): String {
    return readVarBytes().toString(Charsets.US_ASCII)
}

fun Sink.writeUtf(string: String) {
    writeVarBytes(string.toByteArray(Charsets.UTF_8))
}

fun Source.readUtf(): String {
    return readVarBytes().toString(Charsets.UTF_8)
}

fun Sink.writeByteFromInt(int: Int) {
    writeByte(int.toByte())
}

fun Source.readIntFromByte(): Int {
    return readByte().toInt()
}

fun Sink.writeShortFromInt(int: Int) {
    writeShort(int.toShort())
}

fun Source.readIntFromShort(): Int {
    return readShort().toInt()
}