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
import java.nio.channels.SocketChannel

class SocketChannelData(private val channel: SocketChannel) : StandardData {
	private val rx = ByteBuffer.allocateDirect(1024).limit(0)
	private val tx = ByteBuffer.allocateDirect(1024).limit(0)

	override fun buffer(count: Int): ByteBuffer {
		rx.compact()
		while (rx.position() < count) {
			val read = channel.read(rx)
			if (read == -1) throw IOException("Socket data stream ended.")
		}
		return rx.flip()
	}

	override fun boolean(): Boolean = buffer(1).get() == 1.toByte()
	override fun boolean(b: Boolean) {
		if (!tx.hasRemaining()) flush()
		tx.put(if (b) 1 else 0)
	}

	override fun byte(): Byte = buffer(1).get()
	override fun byte(b: Byte) {
		if (!tx.hasRemaining()) flush()
		tx.put(b)
	}

	override fun short(): Short = buffer(2).getShort()
	override fun short(s: Short) {
		if (!tx.hasRemaining()) flush()
		tx.putShort(s)
	}

	override fun int(): Int = buffer(4).getInt()
	override fun int(i: Int) {
		if (!tx.hasRemaining()) flush()
		tx.putInt(i)
	}

	override fun long(): Long = buffer(8).getLong()
	override fun long(l: Long) {
		if (!tx.hasRemaining()) flush()
		tx.putLong(l)
	}

	override fun float(): Float = buffer(4).getFloat()
	override fun float(f: Float) {
		if (!tx.hasRemaining()) flush()
		tx.putFloat(f)
	}

	override fun double(): Double = buffer(8).getDouble()
	override fun double(d: Double) {
		if (!tx.hasRemaining()) flush()
		tx.putDouble(d)
	}

	override fun varInt(): Int {
		var n = 0
		for (bit in 0..<32 step 7) {
			val byte = buffer(1).get().toInt() and 0xFF
			n = n or ((byte and 0b0_1111111) shl bit)
			if (byte ushr 7 == 0) return n
		}
		throw IOException("VarInt exceeded maximum range.")
	}

	override fun varInt(i: Int) {
		if (!tx.hasRemaining()) flush()
		if (i == 0) {
			tx.put(0)
			return
		}
		var n = i
		while (n != 0) {
			var b = n and 0b0_1111111
			n = n ushr 7
			if (n > 0) b = b or 0b1_0000000
			tx.put(b.toByte())
		}
	}

	override fun varLong(): Long {
		var n = 0L
		for (bit in 0..<64 step 7) {
			val byte = buffer(1).get().toLong() and 0xFF
			n = n or ((byte and 0b0_1111111) shl bit)
			if (byte ushr 7 == 0L) return n
		}
		throw IOException("VarLong exceeded maximum range.")
	}

	override fun varLong(l: Long) {
		if (!tx.hasRemaining()) flush()
		if (l == 0L) {
			tx.put(0)
			return
		}
		var n = l
		while (n != 0L) {
			var b = n and 0b0_1111111
			n = n ushr 7
			if (n > 0) b = b or 0b1_0000000
			tx.put(b.toByte())
		}
	}

	override fun string(maximum: Int, s: String) {
		if (s.length > maximum) throw IOException("String length exceeded maximum transmission size (${s.length} > ${maximum})")
		varInt(s.length)
		if (!tx.hasRemaining()) flush()
		tx.put(s.toByteArray(Charsets.UTF_8))
	}

	override fun string(maximum: Int): String {
		val length = varInt()
		if (length > maximum) throw IOException("String length exceeded maximum receive size ($length > $maximum)")
		val data = ByteArray(length)
		buffer(length).get(data)
		return data.toString(Charsets.UTF_8)
	}

	override fun bytes(a: ByteArray) {
		if (!tx.hasRemaining()) flush()
		tx.put(a)
	}

	override fun skip(count: Int) {
		buffer(count).limit(0)
	}

	override fun flush() {
		this.channel.write(tx.flip())
		tx.clear()
	}
}