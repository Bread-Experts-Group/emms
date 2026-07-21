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
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.time.Instant

class ServerClient(
	private val serverInfo: ServerInformation,
	private val data: StandardData
) : Runnable {
	enum class State {
		HANDSHAKING,
		STATUS,
		LOGIN,
		CONFIGURATION,
		PLAY,
		TRANSFER
	}

	private var state = State.HANDSHAKING
	private var compressionThreshold: Int? = null

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

	private val compressionBuffer = ByteBuffer.allocate(16384)
	private val deflater = Deflater(COMPRESSION_LEVEL.toInt(), false)
	private val inflater = Inflater(false)
	private val dataCompressed = CompressedDataSource(inflater, data)

	private fun transmitPacket(id: Int, consumer: StandardDataSink.() -> Unit) {
		stagingSink.varInt(id)
		consumer(stagingSink)

		val compressionThreshold = compressionThreshold
		if (compressionThreshold != null) {
			val payload = stagingSink.bytes()
			if (payload.size > compressionThreshold) {
				stagingSink.varInt(payload.size)

				deflater.reset()
				deflater.setInput(payload)
				deflater.finish()
				deflater.deflate(compressionBuffer.clear())
				if (!deflater.finished()) TODO("grow compress")

				data.varInt(stagingSink.size() + compressionBuffer.position())
				data.bytes(stagingSink.bytes())
				data.bytes(compressionBuffer.flip())
			} else {
				data.varInt(1 + payload.size)
				data.byte(0)
				data.bytes(payload)
			}
		} else {
			data.varInt(stagingSink.size())
			data.bytes(stagingSink.bytes())
		}
		data.flush()
	}

	override fun run() {
		while (true) {
			val packetLength: Int
			val data: StandardDataSource = if (compressionThreshold != null) {
				val compressedPacketLength = data.varInt()
				data.clearConsumed()
				val dataLength = data.varInt()
				if (dataLength == 0) {
					packetLength = compressedPacketLength - data.consumed().toInt()
					data.clearConsumed()
					data
				} else {
					packetLength = dataLength
					dataCompressed.limit(compressedPacketLength - data.consumed().toInt())
					dataCompressed.clearConsumed()
					dataCompressed
				}
			} else {
				packetLength = data.varInt()
				data.clearConsumed()
				data
			}

			val packetID = data.varInt()

			fun skipPacket() {
				data.skip((packetLength - data.consumed()).toInt())
				println("$state: 0x${packetID.toHexString(HexFormat.UpperCase)} ($packetLength)")
			}

			when (state) {
				State.HANDSHAKING -> {
					if (packetID != 0x00) {
						skipPacket()
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
						string(
							"{\n" +
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
									"}", 32767
						)
					}

					0x01 -> transmitPacket(0x01) {
						long(data.long())
					}

					else -> skipPacket()
				}

				State.LOGIN -> when (packetID) {
					0x00 -> {
						val login = LogInState(data.string(16), data.uuid())
						internalLoginState = login
						transmitPacket(0x01) {
							string("", 20)

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
						data as TransportEncryptable
						val encSharedSecret = data.bytes(data.varInt())
						val encVerifyToken = data.bytes(data.varInt())

						val sharedSecret = serverInfo.decryptCipher.doFinal(encSharedSecret)
						val verifyToken = serverInfo.decryptCipher.doFinal(encVerifyToken)

						if (!verifyToken.contentEquals(loginState.verifyToken)) throw IOException(
							"Client did not provide the correct verify token during encryption setup."
						)

//						val sha = MessageDigest.getInstance("SHA-1")
//						sha.update(byteArrayOf()) // server id
//						sha.update(sharedSecret)
//						sha.update(serverInfo.publicKey.encoded)
//						val severID = BigInteger(sha.digest()).toString(16)

						// TODO: &ip=ip IF PREVENT_PROXY_CONNECTIONS
//						val response = serverInfo.httpClient.send(
//							HttpRequest.newBuilder(
//								URI("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${loginState.username}&serverId=$severID")
//							).GET().build(),
//							HttpResponse.BodyHandlers.ofString()
//						) // TODO: JSON
//						println(response.body())

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

						@Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
						if (COMPRESSION_THRESHOLD < 0) {
							transmitPacket(0x03) {
								varInt(COMPRESSION_THRESHOLD)
							}
							compressionThreshold = COMPRESSION_THRESHOLD
						}

						transmitPacket(0x02) {
							uuid(loginState.uuid)
							string(loginState.username, 16)
							varInt(0)
							boolean(true)
						}
					}

					0x03 -> {
						state = State.CONFIGURATION
					}

					else -> skipPacket()
				}

				State.CONFIGURATION -> when (packetID) {
					0x00 -> {
						loginState.locale = data.string(16)
						loginState.viewDistance = data.byte().toInt() and 0xFF
						loginState.chatMode = LogInState.ChatMode.entries[data.varInt()]
						loginState.chatColors = data.boolean()

						loginState.displayedSkinParts.clear()
						val skinPartsBits = data.unsignedByte().toUInt()
						if (skinPartsBits and 0x01u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.CAPE)
						if (skinPartsBits and 0x02u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.JACKET)
						if (skinPartsBits and 0x04u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.LEFT_SLEEVE)
						if (skinPartsBits and 0x08u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.RIGHT_SLEEVE)
						if (skinPartsBits and 0x10u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.LEFT_PANTS)
						if (skinPartsBits and 0x20u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.RIGHT_PANTS)
						if (skinPartsBits and 0x40u != 0u) loginState.displayedSkinParts.add(LogInState.SkinParts.HAT)

						loginState.mainHand = LogInState.MainHand.entries[data.varInt()]
						loginState.textFiltering = data.boolean()
						loginState.serverListingOnStatus = data.boolean()

						transmitPacket(0x0E) {
							varInt(1)
							string("minecraft")
							string("core")
							string("1.21")
						}
					}

					0x02 -> {
						val channel = data.identifier()
						val pluginData = data.bytes(packetLength - data.consumed().toInt(), 32767)
						println("$channel: ${pluginData.toHexString()}")
					}

					0x03 -> {
						state = State.PLAY
						transmitPacket(0x2B) {
							int(0)
							boolean(false)
							varInt(1)
							identifier("minecraft:overworld")
							varInt(20)
							varInt(32)
							varInt(32)
							boolean(false)
							boolean(false)
							boolean(false)
							varInt(0)
							identifier("minecraft:overworld")
							long(0)
							unsignedByte(0u) // GAME MODE
							byte(-1) // PREVIOUS GAME MODE
							boolean(false)
							boolean(false)
							boolean(false) // DEATH LOCATION
							varInt(20)
							boolean(true)
						}

						transmitPacket(0x40) {
							double(0.0)
							double(0.0)
							double(0.0)
							float(0f)
							float(0f)
							byte(0)
							varInt(0)
						}

						transmitPacket(0x3E) {
							byte(0x01)
							varInt(1)
							uuid(loginState.uuid)
							string(loginState.username, 16)
							varInt(0)
						}

						transmitPacket(0x22) {
							unsignedByte(13u)
							float(0f)
						}
					}

					0x07 -> {
						repeat(data.varInt().also { println("$it packs") }) {
							println("${data.string()} ${data.string()} ${data.string()}")
						}

						transmitPacket(0x07) {
							identifier("minecraft:dimension_type")
							val dimensionType = Path("server/src/main/resources/data/minecraft/dimension_type").walk()
								.map { it.name.removeSuffix(".json") }.toList()
							varInt(dimensionType.size)
							dimensionType.forEach {
								identifier("minecraft:$it")
								boolean(true)
								byte(0x0A)
								nbt(
									NBTType.NBTCompound(
										"has_skylight" to NBTType.NBTByte(1),
										"has_ceiling" to NBTType.NBTByte(0),
										"ultrawarm" to NBTType.NBTByte(0),
										"natural" to NBTType.NBTByte(1),
										"coordinate_scale" to NBTType.NBTDouble(1.0),
										"bed_works" to NBTType.NBTByte(1),
										"respawn_anchor_works" to NBTType.NBTByte(1),
										"min_y" to NBTType.NBTInt(-64),
										"height" to NBTType.NBTInt(384),
										"logical_height" to NBTType.NBTInt(384),
										"infiniburn" to NBTType.NBTString("#"),
										"effects" to NBTType.NBTString("minecraft:overworld"),
										"ambient_light" to NBTType.NBTFloat(0f),
										"piglin_safe" to NBTType.NBTByte(0),
										"has_raids" to NBTType.NBTByte(1),
										"monster_spawn_light_level" to NBTType.NBTByte(0),
										"monster_spawn_block_light_limit" to NBTType.NBTByte(0),
									)
								)
							}
						}

						transmitPacket(0x07) {
							identifier("minecraft:wolf_variant")
							val wolfVariant = Path("server/src/main/resources/data/minecraft/wolf_variant").walk()
								.map { it.name.removeSuffix(".json") }.toList()
							varInt(wolfVariant.size)
							wolfVariant.forEach {
								identifier("minecraft:$it")
								boolean(true)
								byte(0x0A)
								nbt(
									NBTType.NBTCompound(
										"wild_texture" to NBTType.NBTString("minecraft:entity/wolf/wolf_ashen"),
										"tame_texture" to NBTType.NBTString("minecraft:entity/wolf/wolf_ashen_tame"),
										"angry_texture" to NBTType.NBTString("minecraft:entity/wolf/wolf_ashen_angry"),
										"biomes" to NBTType.NBTList(emptyList<NBTType.NBTString>()),
									)
								)
							}
						}

						transmitPacket(0x07) {
							identifier("minecraft:painting_variant")
							val paintingVariant = Path("server/src/main/resources/data/minecraft/painting_variant").walk()
								.map { it.name.removeSuffix(".json") }.toList()
							varInt(paintingVariant.size)
							paintingVariant.forEach {
								identifier("minecraft:$it")
								boolean(true)
								byte(0x0A)
								nbt(
									NBTType.NBTCompound(
										"asset_id" to NBTType.NBTString("minecraft:alban"),
										"height" to NBTType.NBTInt(1),
										"width" to NBTType.NBTInt(1)
									)
								)
							}
						}

						transmitPacket(0x07) {
							identifier("minecraft:damage_type")
							val damageTypes = Path("server/src/main/resources/data/minecraft/damage_type").walk()
								.map { it.name.removeSuffix(".json") }.toList()
							varInt(damageTypes.size)
							damageTypes.forEach {
								identifier("minecraft:$it")
								boolean(true)
								byte(0x0A)
								nbt(
									NBTType.NBTCompound(
										"message_id" to NBTType.NBTString("inFire"),
										"scaling" to NBTType.NBTString("when_caused_by_living_non_player"),
										"exhaustion" to NBTType.NBTFloat(0.1f),
										"effects" to NBTType.NBTString("burning")
									)
								)
							}
						}

						transmitPacket(0x07) {
							identifier("minecraft:worldgen/biome")
							val biomes = Path("server/src/main/resources/data/minecraft/worldgen/biome").walk()
								.map { it.name.removeSuffix(".json") }.toList()
							varInt(biomes.size)
							biomes.forEach {
								identifier("minecraft:$it")
								boolean(true)
								byte(0x0A)
								nbt(
									NBTType.NBTCompound(
										"has_precipitation" to NBTType.NBTByte(1),
										"temperature" to NBTType.NBTFloat(0f),
										"downfall" to NBTType.NBTFloat(0f),
										"effects" to NBTType.NBTCompound(
											"fog_color" to NBTType.NBTInt(8364543),
											"water_color" to NBTType.NBTInt(8364543),
											"water_fog_color" to NBTType.NBTInt(8364543),
											"sky_color" to NBTType.NBTInt(8364543)
										)
									)
								)
							}
						}

						transmitPacket(0x03) {
						}
					}

					else -> skipPacket()
				}

				State.PLAY -> when (packetID) {
					0x07 -> {
						val sessionId = data.uuid()
						val pkExpiresAt = Instant.fromEpochMilliseconds(data.long())
						val pk = data.bytes(data.varInt(), 512)
						val keySignature = data.bytes(data.varInt(), 4096)
						println("Session $sessionId, #${pk.size} pk [$pkExpiresAt], #${keySignature.size} ks")
					}

					0x1A -> {
						val x = data.double()
						val y = data.double()
						val z = data.double()
						val ground = data.boolean()
						println("$x, $y, $z : ${if (ground) "grounded" else "freefall"}")
					}

					0x1B -> {
						val x = data.double()
						val y = data.double()
						val z = data.double()
						val yaw = data.float()
						val pitch = data.float()
						val ground = data.boolean()
						println("$x, $y, $z : $yaw* $pitch* : ${if (ground) "grounded" else "freefall"}")
					}

					0x06 -> {
						val message = data.string(256)
						val timestamp = Instant.fromEpochMilliseconds(data.long())
						val salt = data.long()
						val signature = if (data.boolean()) data.bytes(256) else null
						val messageCount = data.varInt()
						val acknowledge = data.bytes(3)
						println("MSG: $message @ $timestamp $salt $signature $messageCount $acknowledge")
					}

					else -> skipPacket()
				}

				else -> TODO("$state")
			}
		}
	}
}