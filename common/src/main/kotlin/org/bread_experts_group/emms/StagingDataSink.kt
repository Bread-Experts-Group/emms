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
	private val buffer = ByteBuffer.allocate(16384)
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
		buffer.putInt(i)
	}

	override fun long(l: Long) {
		buffer.putLong(l)
	}

	override fun float(f: Float) {
		buffer.putFloat(f)
	}

	override fun double(d: Double) {
		buffer.putDouble(d)
	}

	override fun bytes(a: ByteArray, maximum: Int?) {
		if (maximum != null && a.size > maximum) throw IOException("Byte array length exceeded maximum write size (${a.size} > ${maximum})")
		buffer.put(a)
	}

	override fun nbt(nbt: NBTType) {
		when (nbt) {
			is NBTType.NBTCompound -> {
				nbt.elements.forEach { (key, type) ->
					buffer.put(type.typeID())
					val nameBytes = key.toByteArray(Charsets.UTF_8)
					buffer.putShort(nameBytes.size.toShort())
					buffer.put(nameBytes)
					nbt(type)
				}
				buffer.put(0x00)
			}

			is NBTType.NBTList<*> -> {
				buffer.put(nbt.elements.firstOrNull()?.typeID() ?: 0)
				buffer.putInt(nbt.elements.size)
				nbt.elements.forEach {
					nbt(it)
				}
			}

			is NBTType.NBTByte -> {
				buffer.put(nbt.byte)
			}

			is NBTType.NBTInt -> {
				buffer.putInt(nbt.int)
			}

			is NBTType.NBTFloat -> {
				buffer.putFloat(nbt.float)
			}

			is NBTType.NBTDouble -> {
				buffer.putDouble(nbt.double)
			}

			is NBTType.NBTString -> {
				val nameBytes = nbt.string.toByteArray(Charsets.UTF_8)
				buffer.putShort(nameBytes.size.toShort())
				buffer.put(nameBytes)
			}

			else -> TODO("! $nbt")
		}
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