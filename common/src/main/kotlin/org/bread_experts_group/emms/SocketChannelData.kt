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
	private val rx = ByteBuffer.allocateDirect(16384).limit(0)
	private val tx = ByteBuffer.allocateDirect(16384).limit(0)

	private var rxCipher: Cipher? = null
	private var txCipher: Cipher? = null

	private val rxCipherBuffer = ByteBuffer.allocate(16384)
	private val txCipherBuffer = ByteBuffer.allocate(16384)

	private var consumed = 0L
	override fun clearConsumed() {
		consumed = 0L
	}

	override fun consumed(): Long = consumed

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

	override fun boolean(): Boolean = buffer(Byte.SIZE_BYTES).get() == 1.toByte().also { consumed += Byte.SIZE_BYTES }
	override fun boolean(b: Boolean) {
		if (!tx.hasRemaining()) flush()
		tx.put(if (b) 1 else 0)
	}

	override fun byte(): Byte = buffer(Byte.SIZE_BYTES).get().also { consumed += Byte.SIZE_BYTES }
	override fun byte(b: Byte) {
		if (!tx.hasRemaining()) flush()
		tx.put(b)
	}

	override fun short(): Short = buffer(Short.SIZE_BYTES).getShort().also { consumed += Short.SIZE_BYTES }
	override fun short(s: Short) {
		if (!tx.hasRemaining()) flush()
		tx.putShort(s)
	}

	override fun int(): Int = buffer(Int.SIZE_BYTES).getInt().also { consumed += Int.SIZE_BYTES }
	override fun int(i: Int) {
		if (!tx.hasRemaining()) flush()
		tx.putInt(i)
	}

	override fun long(): Long = buffer(Long.SIZE_BYTES).getLong().also { consumed += Long.SIZE_BYTES }
	override fun long(l: Long) {
		if (!tx.hasRemaining()) flush()
		tx.putLong(l)
	}

	override fun float(): Float = buffer(Float.SIZE_BYTES).getFloat().also { consumed += Float.SIZE_BYTES }
	override fun float(f: Float) {
		if (!tx.hasRemaining()) flush()
		tx.putFloat(f)
	}

	override fun double(): Double = buffer(Double.SIZE_BYTES).getDouble().also { consumed += Double.SIZE_BYTES }
	override fun double(d: Double) {
		if (!tx.hasRemaining()) flush()
		tx.putDouble(d)
	}

	override fun bytes(a: ByteArray, maximum: Int?) {
		if (maximum != null && a.size > maximum) throw IOException("Byte array length exceeded maximum transmission size (${a.size} > ${maximum})")
		if (!tx.hasRemaining()) flush()
		tx.put(a)
	}

	override fun bytes(count: Int, maximum: Int?): ByteArray {
		if (maximum != null && count > maximum) throw IOException("Byte array length exceeded maximum receive size ($count > $maximum)")
		val data = ByteArray(count)
		buffer(count).get(data)
		consumed += count
		return data
	}

	override fun skip(count: Int) {
		buffer(count).position(count)
		consumed += count
	}

	override fun nbt(nbt: NBTType) {
		TODO("Not yet implemented")
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