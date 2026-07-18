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
import javax.crypto.Cipher

class SocketChannelData(private val channel: SocketChannel) : StandardDataEncryptable {
	private val rx = ByteBuffer.allocateDirect(1024).limit(0)
	private val tx = ByteBuffer.allocateDirect(1024).limit(0)

	private var rxCipher: Cipher? = null
	private var txCipher: Cipher? = null

	private val rxCipherBuffer = ByteBuffer.allocate(1024)
	private val txCipherBuffer = ByteBuffer.allocate(1024)

	override fun receiveEncryption(cipher: Cipher?) {
		rxCipher = cipher
		if (cipher != null) {
			rxCipherBuffer.clear().put(rx).flip()
			cipher.update(rxCipherBuffer, rx.clear())
			rx.flip()
		}
	}

	override fun transmitEncryption(cipher: Cipher?) {
		txCipher = cipher
	}

	override fun buffer(count: Int): ByteBuffer {
		rx.compact()
		val rxCipher = rxCipher
		if (rxCipher != null) {
			while (rx.position() < count) {
				val read = channel.read(rxCipherBuffer.clear())
				if (read == -1) throw IOException("Socket data stream ended.")
				rxCipher.update(rxCipherBuffer.flip(), rx)
			}
		} else {
			while (rx.position() < count) {
				val read = channel.read(rx)
				if (read == -1) throw IOException("Socket data stream ended.")
			}
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

	override fun bytes(a: ByteArray) {
		if (!tx.hasRemaining()) flush()
		tx.put(a)
	}

	override fun bytes(count: Int): ByteArray {
		val data = ByteArray(count)
		buffer(count).get(data)
		return data
	}

	override fun skip(count: Int) {
		buffer(count).limit(0)
	}

	override fun flush() {
		val txCipher = txCipher
		if (txCipher != null) {
			txCipher.update(tx.flip(), txCipherBuffer.clear())
			this.channel.write(txCipherBuffer.flip())
		} else this.channel.write(tx.flip())
		tx.clear()
	}
}