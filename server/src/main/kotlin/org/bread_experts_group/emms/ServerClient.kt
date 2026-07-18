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

class ServerClient(private val data: StandardData) : Runnable {
	enum class State {
		HANDSHAKING,
		STATUS,
		LOGIN,
		TRANSFER
	}

	private var state = State.HANDSHAKING

	private val stagingSink = StagingDataSink()
	private fun writePacket(id: Int, consumer: StandardDataSink.() -> Unit) {
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
					0x00 -> writePacket(0x00) {
						string(32767, "{\n" +
								"    \"version\": {\n" +
								"        \"name\": \"1.21.1\",\n" +
								"        \"protocol\": 767\n" +
								"    },\n" +
								"    \"players\": {\n" +
								"        \"max\": 99999999999,\n" +
								"        \"online\": 1,\n" +
								"        \"sample\": [\n" +
								"            {\n" +
								"                \"name\": \"thinkofdeath\",\n" +
								"                \"id\": \"4566e69f-c907-48ee-8d71-d7ba5aa00d20\"\n" +
								"            }\n" +
								"        ]\n" +
								"    },\n" +
								"    \"description\": {\n" +
								"        \"text\": \"Hello, world!\"\n" +
								"    },\n" +
								"    \"enforcesSecureChat\": false\n" +
								"}")
					}

					0x01 -> writePacket(0x01) {
						long(data.long())
					}

					else -> data.skip(packetLength)
				}

				else -> TODO("$state")
			}
		}
	}
}