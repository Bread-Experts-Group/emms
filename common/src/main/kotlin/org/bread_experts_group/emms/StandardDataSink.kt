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

interface StandardDataSink {
	fun boolean(b: Boolean)
	fun byte(b: Byte)
	fun unsignedByte(ub: UByte) = byte(ub.toByte())
	fun short(s: Short)
	fun unsignedShort(us: UShort) = short(us.toShort())
	fun int(i: Int)
	fun long(l: Long)
	fun float(f: Float)
	fun double(d: Double)

	fun varInt(i: Int) {
		if (i == 0) {
			byte(0)
			return
		}
		var n = i
		while (n != 0) {
			var b = n and 0b0_1111111
			n = n ushr 7
			if (n > 0) b = b or 0b1_0000000
			byte(b.toByte())
		}
	}

	fun varLong(l: Long) {
		if (l == 0L) {
			byte(0)
			return
		}
		var n = l
		while (n != 0L) {
			var b = n and 0b0_1111111
			n = n ushr 7
			if (n > 0) b = b or 0b1_0000000
			byte(b.toByte())
		}
	}

	fun bytes(a: ByteArray, maximum: Int? = null)
	fun bytes(b: ByteBuffer, maximum: Int? = null)

	fun string(s: String, maximum: Int? = null) {
		if (maximum != null && s.length > maximum) throw IOException("String length exceeded maximum transmission size (${s.length} > ${maximum})")
		varInt(s.length)
		bytes(s.toByteArray(Charsets.UTF_8))
	}

	fun identifier(s: String): Unit = string(s, 32767)

	fun uuid(uuid: Uuid) {
		uuid.toLongs { mostSignificantBits, leastSignificantBits ->
			long(mostSignificantBits)
			long(leastSignificantBits)
		}
	}

	fun nbt(nbt: NBTType, root: Boolean)

	fun componentNBT(component: Component): Unit = nbt(component.nbt(), true)
	fun componentJSON(component: Component): Unit = string(component.json(), 262144)

	fun flush()
}