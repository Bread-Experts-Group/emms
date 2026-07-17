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

	fun varInt(i: Int)
	fun varLong(l: Long)

	fun flush()
}