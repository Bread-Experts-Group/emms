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

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.security.KeyPairGenerator
import javax.crypto.Cipher

const val COMPRESSION_LEVEL: UByte = 9u // In the future, it might be better to automatically determine these based on connection heuristics.
const val COMPRESSION_THRESHOLD: Int = 1024

fun main() {
	val rsaGenerator = KeyPairGenerator.getInstance("RSA")
	rsaGenerator.initialize(1024)
	val keyPair = rsaGenerator.generateKeyPair()

	val serverCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
	serverCipher.init(Cipher.DECRYPT_MODE, keyPair.private)

	val serverInfo = ServerInformation(keyPair.public, serverCipher)

	val server = ServerSocketChannel.open()
//	server.setOption(StandardSocketOptions.TCP_NODELAY, true)
	server.bind(InetSocketAddress(25565))
	while (true) {
		val client = server.accept()
		Thread.ofVirtual().start(ServerClient(serverInfo, SocketChannelData(client)))
	}
}