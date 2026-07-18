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

sealed interface NBTType {
	@JvmInline
	value class NBTByte(val byte: Byte) : NBTType {
		override fun typeID(): Byte = 1
	}

	@JvmInline
	value class NBTShort(val short: Short) : NBTType {
		override fun typeID(): Byte = 2
	}

	@JvmInline
	value class NBTInt(val int: Int) : NBTType {
		override fun typeID(): Byte = 3
	}

	@JvmInline
	value class NBTLong(val long: Long) : NBTType {
		override fun typeID(): Byte = 4
	}

	@JvmInline
	value class NBTFloat(val float: Float) : NBTType {
		override fun typeID(): Byte = 5
	}

	@JvmInline
	value class NBTDouble(val double: Double) : NBTType {
		override fun typeID(): Byte = 6
	}

	@JvmInline
	value class NBTByteArray(val byteArray: ByteArray) : NBTType {
		override fun typeID(): Byte = 7
	}

	@JvmInline
	value class NBTString(val string: String) : NBTType {
		override fun typeID(): Byte = 8
	}

	@JvmInline
	value class NBTIntArray(val intArray: ByteArray) : NBTType {
		override fun typeID(): Byte = 11
	}

	@JvmInline
	value class NBTLongArray(val longArray: LongArray) : NBTType {
		override fun typeID(): Byte = 12
	}

	@JvmInline
	value class NBTList<T : NBTType>(val elements: List<T>) : NBTType {
		override fun typeID(): Byte = 9
	}

	@JvmInline
	value class NBTCompound(val elements: Map<String, NBTType>) : NBTType {
		constructor(vararg elements: Pair<String, NBTType>) : this(mapOf(*elements))

		override fun typeID(): Byte = 10
	}

	fun typeID(): Byte
}