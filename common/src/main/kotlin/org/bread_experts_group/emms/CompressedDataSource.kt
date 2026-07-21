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
import java.util.zip.Inflater

class CompressedDataSource(
	private val inflater: Inflater,
	private val data: StandardDataSource
) : StandardDataSource {
	private var consumed = 0L
	private var limit = 0

	private val compressedBuffer = ByteBuffer.allocate(16384)
	private val buffer = ByteBuffer.allocate(16384).limit(0)

	fun limit(bytes: Int) {
		inflater.reset()
		limit = bytes
	}

	override fun clearConsumed() {
		consumed = 0L
	}

	override fun consumed(): Long = consumed

	override fun buffer(count: Int): ByteBuffer {
		buffer.compact()
		while (buffer.position() < count) {
			data.transferTo(compressedBuffer.rewind().limit(limit))
			limit -= compressedBuffer.position()
			inflater.setInput(compressedBuffer.flip())
			inflater.inflate(buffer)
		}
		return buffer.flip()
	}

	override fun boolean(): Boolean {
		consumed += Byte.SIZE_BYTES
		return buffer(Byte.SIZE_BYTES).get() == 1.toByte()
	}

	override fun byte(): Byte {
		consumed += Byte.SIZE_BYTES
		return buffer(Byte.SIZE_BYTES).get()
	}

	override fun short(): Short {
		TODO("Not yet implemented")
	}

	override fun int(): Int {
		TODO("Not yet implemented")
	}

	override fun long(): Long {
		consumed += Long.SIZE_BYTES
		return buffer(Long.SIZE_BYTES).getLong()
	}

	override fun float(): Float {
		consumed += Float.SIZE_BYTES
		return buffer(Float.SIZE_BYTES).getFloat()
	}

	override fun double(): Double {
		consumed += Double.SIZE_BYTES
		return buffer(Double.SIZE_BYTES).getDouble()
	}

	override fun bytes(count: Int, maximum: Int?): ByteArray {
		if (maximum != null && count > maximum) throw IOException("Byte array length exceeded maximum decompression size ($count > $maximum)")
		val data = ByteArray(count)
		buffer(count).get(data)
		consumed += count
		return data
	}

	override fun transferTo(buffer: ByteBuffer) {
		TODO("Not yet implemented")
	}

	override fun skip(count: Int) {
		consumed += count
		buffer(count).position(buffer.position() + count)
	}
}