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

@file:Suppress("RedundantNullableReturnType")

package org.bread_experts_group.emms

import java.net.*
import java.net.http.HttpClient
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlin.time.DurationUnit


const val COMPRESSION_LEVEL: UByte = 9u // In the future, it might be better to automatically determine these based on connection heuristics.
const val COMPRESSION_THRESHOLD: Int = 1024

const val DISABLE_NAGLE_ALGORITHM: Boolean = true
const val PORT: UShort = 25565u
val QUERY_PORT: UShort? = null // 25565u

const val PREVENT_PROXY_CONNECTIONS: Boolean = false
const val ENCRYPTION: Boolean = true
const val ONLINE_MODE: Boolean = false

const val SERVER_LIST_PING_PORT: UShort = 4445u
val SERVER_LIST_PING_BROADCAST_INTERNAL: Duration? = null // 1.5.toDuration(DurationUnit.SECONDS)
val SERVER_LIST_PING_INTERFACE: String? = null

val MOTD: Component = Component.Literal("A Minecraft Server")

const val WORLD_NAME: String = "world"

val HTTP_CLIENT: HttpClient = HttpClient.newHttpClient()

fun main() {
	val rsaGenerator = KeyPairGenerator.getInstance("RSA")
	rsaGenerator.initialize(1024)
	val keyPair = rsaGenerator.generateKeyPair()

	val serverCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
	serverCipher.init(Cipher.DECRYPT_MODE, keyPair.private)

	val serverInfo = ServerInformation(keyPair.public, serverCipher, HTTP_CLIENT)

	val server = ServerSocketChannel.open()
	server.bind(InetSocketAddress(PORT.toInt()))

	if (SERVER_LIST_PING_BROADCAST_INTERNAL != null) {
		if (MOTD !is Component.Literal) println("Unable to start Server List Ping broadcasting. Cannot broadcast a non-literal MOTD.")
		else {
			val interfaces = if (SERVER_LIST_PING_INTERFACE != null) listOf(
				NetworkInterface.getByName(SERVER_LIST_PING_INTERFACE).also {
					if (it == null) throw IllegalArgumentException("Server List Ping error: Network interface $SERVER_LIST_PING_INTERFACE could not be found")
					if (!it.supportsMulticast()) throw IllegalArgumentException("Server List Ping error: $SERVER_LIST_PING_INTERFACE doesn't support multicast")
				}
			) else NetworkInterface.getNetworkInterfaces().toList().filter {
				it.supportsMulticast() && it.isUp && !it.isLoopback
			}

			val interval = SERVER_LIST_PING_BROADCAST_INTERNAL.toLong(DurationUnit.MILLISECONDS)
			val data = "[MOTD]${MOTD.string}[/MOTD][AD]$PORT[/AD]".toByteArray(Charsets.UTF_8)
			interfaces.forEach { iFace ->
				fun pinger(groupAddress: String, family: ProtocolFamily): () -> Unit = {
					val udp = DatagramChannel.open(family)
						.setOption(StandardSocketOptions.IP_MULTICAST_IF, iFace)
					val buffer = ByteBuffer.wrap(data)
					val group = InetSocketAddress(groupAddress, SERVER_LIST_PING_PORT.toInt())
					while (true) {
						udp.send(buffer.rewind(), group)
						Thread.sleep(interval)
					}
				}

				if (iFace.interfaceAddresses.any { iAddr -> iAddr.address is Inet4Address }) Thread.ofVirtual()
					.name("$iFace Server List Ping (IPv4 @ $SERVER_LIST_PING_PORT)")
					.start(pinger("224.0.2.60", StandardProtocolFamily.INET))
				if (iFace.interfaceAddresses.any { iAddr -> iAddr.address is Inet6Address }) Thread.ofVirtual()
					.name("$iFace Server List Ping (IPv6 @ $SERVER_LIST_PING_PORT)")
					.start(pinger("ff75:230::60", StandardProtocolFamily.INET6))
			}
		}
	}

	if (QUERY_PORT != null) {
		val tokenRandom = SecureRandom()
		val tokens = ConcurrentHashMap<SocketAddress, Int>()
		fun query(family: ProtocolFamily): () -> Unit = {
			val buffer = ByteBuffer.allocate(1472)
			val udp = DatagramChannel.open(family)
				.bind(InetSocketAddress(QUERY_PORT.toInt()))
			while (true) {
				val client = udp.receive(buffer.clear())
				buffer.flip()
				if (buffer.getShort() != 0xFEFD.toShort()) continue
				val type = buffer.get()
				val sessionID = buffer.getInt()
				if (type == 9.toByte()) {
					val token = tokenRandom.nextInt()
					tokens[client] = token
					buffer.clear()
					buffer.put(9)
					buffer.putInt(sessionID)
					buffer.put(token.toString().toByteArray(Charsets.US_ASCII)).put(0)
					udp.send(buffer.flip(), client)
				} else {
					val token = buffer.getInt()
					if (tokens[client] != token) continue
					val full = buffer.remaining() == 4
					buffer.clear()
					buffer.put(0)
					buffer.putInt(sessionID)
					if (full) {
						buffer.put(byteArrayOf(0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, 0x80.toByte(), 0x00))
						buffer.put("hostname\u0000".toByteArray())
						buffer.put((MOTD as Component.Literal).string.toByteArray()).put(0)
						buffer.put("gametype\u0000".toByteArray())
						buffer.put("SMP\u0000".toByteArray())
						buffer.put("game_id\u0000".toByteArray())
						buffer.put("MINECRAFT\u0000".toByteArray())
						buffer.put("version\u0000".toByteArray())
						buffer.put("1.21.1\u0000".toByteArray())
						buffer.put("plugins\u0000".toByteArray())
						buffer.put("\u0000".toByteArray())
						buffer.put("map\u0000".toByteArray())
						buffer.put(WORLD_NAME.toByteArray()).put(0)
						buffer.put("numplayers\u0000".toByteArray())
						buffer.put("1\u0000".toByteArray())
						buffer.put("maxplayers\u0000".toByteArray())
						buffer.put("0\u0000".toByteArray())
						buffer.put("hostport\u0000".toByteArray())
						buffer.put("$PORT".toByteArray()).put(0)
						buffer.put("hostip\u0000".toByteArray())
						buffer.put((udp.localAddress as InetSocketAddress).hostString.toByteArray()).put(0)
						buffer.put(0)
						buffer.put(byteArrayOf(0x01, 0x70, 0x6C, 0x61, 0x79, 0x65, 0x72, 0x5F, 0x00, 0x00))
						buffer.put("Aerasto".toByteArray()).put(0)
						buffer.put(0)
					} else {
						buffer.put((MOTD as Component.Literal).string.toByteArray()).put(0)
						buffer.put("SMP\u0000".toByteArray())
						buffer.put(WORLD_NAME.toByteArray()).put(0)
						buffer.put("1\u0000".toByteArray())
						buffer.put("0\u0000".toByteArray())
						buffer.putShort(java.lang.Short.reverseBytes(PORT.toShort()))
						buffer.put((udp.localAddress as InetSocketAddress).hostString.toByteArray()).put(0)
					}
					udp.send(buffer.flip(), client)
				}
			}
		}
		Thread.ofVirtual()
			.name("Server Query Token Expirer")
			.start {
				while (true) {
					Thread.sleep(30_000)
					tokens.clear()
				}
			}
		Thread.ofVirtual()
			.name("Server Query (IPv6 @ $QUERY_PORT)")
			.start(query(StandardProtocolFamily.INET6))
	}

	while (true) {
		val client = server.accept()
		if (DISABLE_NAGLE_ALGORITHM) client.setOption(StandardSocketOptions.TCP_NODELAY, true)
		Thread.ofVirtual()
			.name("${client.remoteAddress} -> ${client.localAddress} client")
			.start(ServerClient(serverInfo, SocketChannelData(client)))
	}
}