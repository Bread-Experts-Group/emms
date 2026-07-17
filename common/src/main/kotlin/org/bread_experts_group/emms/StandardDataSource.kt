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

	fun varInt(): Int
	fun varLong(): Long

	fun skip(count: Int)
}