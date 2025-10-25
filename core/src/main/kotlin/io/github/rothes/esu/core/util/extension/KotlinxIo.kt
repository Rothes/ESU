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