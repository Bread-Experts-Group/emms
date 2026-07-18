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

import java.nio.ByteBuffer

class StagingDataSink : StandardDataSink {
	private val buffer = ByteBuffer.allocate(1024)
	override fun boolean(b: Boolean) {
		buffer.put(if (b) 1 else 0)
	}

	override fun byte(b: Byte) {
		buffer.put(b)
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

	override fun bytes(a: ByteArray) {
		buffer.put(a)
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