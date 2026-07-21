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
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


const val COMPRESSION_LEVEL: UByte = 9u // In the future, it might be better to automatically determine these based on connection heuristics.
const val COMPRESSION_THRESHOLD: Int = 1024

const val NAGLE_ALGORITHM: Boolean = false
const val PORT: UShort = 25565u

const val PREVENT_PROXY_CONNECTIONS: Boolean = false
const val ENCRYPTION: Boolean = true
const val ONLINE_MODE: Boolean = true

val SERVER_LIST_PING_BROADCAST_INTERNAL: Duration? = 1.5.toDuration(DurationUnit.SECONDS)
val SERVER_LIST_PING_INTERFACE: String? = null

val MOTD: Component = Component.Literal("Hello world!")

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
					val group = InetSocketAddress(groupAddress, 4445)
					while (true) {
						udp.send(buffer.rewind(), group)
						Thread.sleep(interval)
					}
				}

				if (iFace.interfaceAddresses.any { iAddr -> iAddr.address is Inet4Address }) Thread.ofVirtual()
					.name("$iFace Server List Ping (IPv4)")
					.start(pinger("224.0.2.60", StandardProtocolFamily.INET))
				if (iFace.interfaceAddresses.any { iAddr -> iAddr.address is Inet6Address }) Thread.ofVirtual()
					.name("$iFace Server List Ping (IPv6)")
					.start(pinger("ff75:230::60", StandardProtocolFamily.INET6))
			}
		}
	}

	while (true) {
		val client = server.accept()
		if (NAGLE_ALGORITHM) client.setOption(StandardSocketOptions.TCP_NODELAY, true)
		Thread.ofVirtual()
			.name("${client.remoteAddress} -> ${client.localAddress} client")
			.start(ServerClient(serverInfo, SocketChannelData(client)))
	}
}