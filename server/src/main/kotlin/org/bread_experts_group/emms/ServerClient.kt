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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ServerClient(
	private val serverInfo: ServerInformation,
	private val data: StandardDataEncryptable
) : Runnable {
	enum class State {
		HANDSHAKING,
		STATUS,
		LOGIN,
		CONFIGURATION,
		TRANSFER
	}

	private var state = State.HANDSHAKING

	private var internalLoginState: LogInState? = null
		set(value) {
			if (value == null) throw IOException("Login information cannot be cleared")
			if (field != null) throw IOException("Login information cannot be written to more than once")
			field = value
		}

	private val loginState: LogInState
		get() = internalLoginState ?: throw IOException("Attempted to retrieve login information, but wasn't present")

	private val secureRandom = SecureRandom()

	private val stagingSink = StagingDataSink()
	private fun transmitPacket(id: Int, consumer: StandardDataSink.() -> Unit) {
		stagingSink.varInt(id)
		consumer(stagingSink)
		data.varInt(stagingSink.size())
		data.bytes(stagingSink.bytes())
		data.flush()
	}

	override fun run() {
		while (true) {
			val packetLength = data.varInt()
			val packetID = data.varInt()
			when (state) {
				State.HANDSHAKING -> {
					if (packetID != 0x00) {
						data.skip(packetLength)
						continue
					}

					val clientPVN = data.varInt()
					val clientConnectingAddress = data.string(255)
					val clientConnectingPort = data.unsignedShort()
					val nextState = State.entries[data.varInt()]

					println("$clientPVN, $clientConnectingAddress, $clientConnectingPort, $nextState")
					state = nextState
				}

				State.STATUS -> when (packetID) {
					0x00 -> transmitPacket(0x00) {
						string(32767, "{\n" +
								"    \"version\": {\n" +
								"        \"name\": \"1.21.1\",\n" +
								"        \"protocol\": 767\n" +
								"    },\n" +
								"    \"players\": {\n" +
								"        \"max\": 0,\n" +
								"        \"online\": 1,\n" +
								"        \"sample\": [\n" +
								"            {\n" +
								"                \"name\": \"Aerasto\",\n" +
								"                \"id\": \"45f8aaf6-629e-42f2-9e66-527d26ac9f86\"\n" +
								"            }\n" +
								"        ]\n" +
								"    },\n" +
								"    \"description\": {\n" +
								"        \"text\": \"Hello, world!\"\n" +
								"    },\n" +
								"    \"enforcesSecureChat\": false\n" +
								"}")
					}

					0x01 -> transmitPacket(0x01) {
						long(data.long())
					}

					else -> data.skip(packetLength)
				}

				State.LOGIN -> when (packetID) {
					0x00 -> {
						val login = LogInState(data.string(16), data.uuid())
						internalLoginState = login
						transmitPacket(0x01) {
							string(20, "")

							val pkBytes = serverInfo.publicKey.encoded
							varInt(pkBytes.size)
							bytes(pkBytes)

							secureRandom.nextBytes(login.verifyToken)
							varInt(login.verifyToken.size)
							bytes(login.verifyToken)

							boolean(true)
						}
					}

					0x01 -> {
						val encSharedSecret = data.bytes(data.varInt())
						val encVerifyToken = data.bytes(data.varInt())

						val sharedSecret = serverInfo.decryptCipher.doFinal(encSharedSecret)
						val verifyToken = serverInfo.decryptCipher.doFinal(encVerifyToken)

						if (!verifyToken.contentEquals(loginState.verifyToken)) throw IOException(
							"Client did not provide the correct verify token during encryption setup."
						)

						val sharedSecretKey = SecretKeySpec(sharedSecret, "AES")
						val sharedSecretIV = IvParameterSpec(sharedSecret)

						data.receiveEncryption(
							Cipher.getInstance("AES/CFB8/NoPadding").also {
								it.init(Cipher.DECRYPT_MODE, sharedSecretKey, sharedSecretIV)
							}
						)
						data.transmitEncryption(
							Cipher.getInstance("AES/CFB8/NoPadding").also {
								it.init(Cipher.ENCRYPT_MODE, sharedSecretKey, sharedSecretIV)
							}
						)

						transmitPacket(0x02) {
							uuid(loginState.uuid)
							string(16, loginState.username)
							varInt(0)
							boolean(true)
						}
					}

					0x03 -> {
						state = State.CONFIGURATION
					}

					else -> {
						println("... ? $packetID : $packetLength")
						data.skip(packetLength)
					}
				}

				State.CONFIGURATION -> when (packetID) {
					else -> {
						println("... ? $packetID : $packetLength")
						data.skip(packetLength)
					}
				}

				else -> TODO("$state")
			}
		}
	}
}