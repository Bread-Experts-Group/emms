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

class StagingDataSink : StandardDataSink {
	private val buffer = ByteBuffer.allocate(1024)
	override fun boolean(b: Boolean) {
		TODO("Not yet implemented")
	}

	override fun byte(b: Byte) {
		TODO("Not yet implemented")
	}

	override fun short(s: Short) {
		TODO("Not yet implemented")
	}

	override fun int(i: Int) {
		TODO("Not yet implemented")
	}

	override fun long(l: Long) {
		buffer.putLong(l)
	}

	override fun float(f: Float) {
		TODO("Not yet implemented")
	}

	override fun double(d: Double) {
		TODO("Not yet implemented")
	}

	override fun varInt(i: Int) {
		if (i == 0) {
			buffer.put(0)
			return
		}
		var n = i
		while (n != 0) {
			var b = n and 0b0_1111111
			n = n ushr 7
			if (n > 0) b = b or 0b1_0000000
			buffer.put(b.toByte())
		}
	}

	override fun varLong(l: Long) {
		TODO("Not yet implemented")
	}

	override fun string(maximum: Int, s: String) {
		if (s.length > maximum) throw IOException("String length exceeded maximum transmission size (${s.length} > ${maximum})")
		varInt(s.length)
		buffer.put(s.toByteArray(Charsets.UTF_8))
	}

	override fun bytes(a: ByteArray) {
		TODO("Not yet implemented")
	}

	override fun flush() {
		TODO("Not yet implemented")
	}

	fun size(): Int = buffer.position()
	fun bytes(): ByteArray {
		val data = ByteArray(buffer.position())
		buffer.flip().get(data).clear()
		return data
	}
}