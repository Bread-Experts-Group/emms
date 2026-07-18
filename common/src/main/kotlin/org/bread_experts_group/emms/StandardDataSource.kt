/*
 *     emms (a multithreaded server/client implementation for Minecraft)
 *     Copyright (C) 2026 Miko Elbrecht, Aerasto (https://github.com/LawsOfScience)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.bread_experts_group.emms

import java.io.IOException
import java.nio.ByteBuffer
import kotlin.uuid.Uuid

interface StandardDataSource {
	fun buffer(count: Int): ByteBuffer

	fun boolean(): Boolean
	fun byte(): Byte
	fun unsignedByte(): UByte = byte().toUByte()
	fun short(): Short
	fun unsignedShort(): UShort = short().toUShort()
	fun int(): Int
	fun long(): Long
	fun float(): Float
	fun double(): Double

	fun bytes(count: Int): ByteArray

	fun varInt(): Int {
		var n = 0
		for (bit in 0..<32 step 7) {
			val byte = buffer(1).get().toInt() and 0xFF
			n = n or ((byte and 0b0_1111111) shl bit)
			if (byte ushr 7 == 0) return n
		}
		throw IOException("VarInt exceeded maximum range.")
	}

	fun varLong(): Long {
		var n = 0L
		for (bit in 0..<64 step 7) {
			val byte = buffer(1).get().toLong() and 0xFF
			n = n or ((byte and 0b0_1111111) shl bit)
			if (byte ushr 7 == 0L) return n
		}
		throw IOException("VarLong exceeded maximum range.")
	}

	fun string(maximum: Int): String {
		val length = varInt()
		if (length > maximum) throw IOException("String length exceeded maximum receive size ($length > $maximum)")
		val data = ByteArray(length)
		buffer(length).get(data)
		return data.toString(Charsets.UTF_8)
	}

	fun uuid() = Uuid.fromLongs(long(), long())

	fun skip(count: Int)
}