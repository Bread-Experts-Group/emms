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

const val CLIENT_STATUS_RESPONSE: Int = 0x00
const val CLIENT_STATUS_PING_RESPONSE: Int = 0x01

const val CLIENT_LOGIN_DISCONNECT: Int = 0x00
const val CLIENT_LOGIN_ENCRYPTION_REQUEST: Int = 0x01
const val CLIENT_LOGIN_SUCCESS: Int = 0x02
const val CLIENT_LOGIN_COMPRESSION_THRESHOLD: Int = 0x03
const val CLIENT_LOGIN_PLUGIN_MESSAGE: Int = 0x04
const val CLIENT_LOGIN_COOKIE_REQUEST: Int = 0x05

const val CLIENT_CONFIGURATION_COOKIE_REQUEST: Int = 0x00
const val CLIENT_CONFIGURATION_PLUGIN_MESSAGE: Int = 0x01
const val CLIENT_CONFIGURATION_DISCONNECT: Int = 0x02
const val CLIENT_CONFIGURATION_FINISH: Int = 0x03
const val CLIENT_CONFIGURATION_KEEP_ALIVE: Int = 0x04
const val CLIENT_CONFIGURATION_PING_REQUEST: Int = 0x05
const val CLIENT_CONFIGURATION_RESET_CHAT: Int = 0x06
const val CLIENT_CONFIGURATION_REGISTRY_DATA: Int = 0x07
const val CLIENT_CONFIGURATION_REMOVE_RESOURCE_PACK: Int = 0x08
const val CLIENT_CONFIGURATION_ADD_RESOURCE_PACK: Int = 0x09
const val CLIENT_CONFIGURATION_COOKIE_STORE: Int = 0x0A
const val CLIENT_CONFIGURATION_TRANSFER_SERVERS: Int = 0x0B
const val CLIENT_CONFIGURATION_FEATURE_FLAGS: Int = 0x0C
const val CLIENT_CONFIGURATION_UPDATE_TAGS: Int = 0x0D
const val CLIENT_CONFIGURATION_KNOWN_PACKS: Int = 0x0E
const val CLIENT_CONFIGURATION_REPORT_DETAILS: Int = 0x0F
const val CLIENT_CONFIGURATION_SERVER_LINKS: Int = 0x10

const val CLIENT_PLAY_DISCONNECT: Int = 0x1D
const val CLIENT_PLAY_GAME_EVENT: Int = 0x22
const val CLIENT_PLAY_LOGIN: Int = 0x2B
const val CLIENT_PLAY_PING_REQUEST: Int = 0x35
const val CLIENT_PLAY_PING_RESPONSE: Int = 0x36
const val CLIENT_PLAY_USER_LIST_UPDATE: Int = 0x3E
const val CLIENT_PLAY_MOVE_PLAYER_AND_ROTATE: Int = 0x40

const val SERVER_HANDSHAKE_INTENTION: Int = 0x00

const val SERVER_STATUS_REQUEST: Int = 0x00
const val SERVER_STATUS_PING_REQUEST: Int = 0x01

const val SERVER_LOGIN_START: Int = 0x00
const val SERVER_LOGIN_ENCRYPTION_RESPONSE: Int = 0x01
const val SERVER_LOGIN_PLUGIN_MESSAGE: Int = 0x02
const val SERVER_LOGIN_ACKNOWLEDGE: Int = 0x03
const val SERVER_LOGIN_COOKIE_RESPONSE: Int = 0x04

const val SERVER_CONFIGURATION_CLIENT_INFO: Int = 0x00
const val SERVER_CONFIGURATION_COOKIE_RESPONSE: Int = 0x01
const val SERVER_CONFIGURATION_PLUGIN_MESSAGE: Int = 0x02
const val SERVER_CONFIGURATION_FINISH_ACKNOWLEDGE: Int = 0x03
const val SERVER_CONFIGURATION_KEEP_ALIVE: Int = 0x04
const val SERVER_CONFIGURATION_PING_RESPONSE: Int = 0x05
const val SERVER_CONFIGURATION_RESOURCE_PACK_RESPONSE: Int = 0x06
const val SERVER_CONFIGURATION_KNOWN_PACKS: Int = 0x07

const val SERVER_PLAY_CHAT_MESSAGE: Int = 0x06
const val SERVER_PLAY_PLAYER_SESSION: Int = 0x07
const val SERVER_PLAY_PLAYER_MOVE: Int = 0x1A
const val SERVER_PLAY_PLAYER_MOVE_AND_ROTATE: Int = 0x1B
const val SERVER_PLAY_PING_REQUEST: Int = 0x21
const val SERVER_PLAY_PING_RESPONSE: Int = 0x27
const val SERVER_PLAY_RESOURCE_PACK_RESPONSE: Int = 0x2B