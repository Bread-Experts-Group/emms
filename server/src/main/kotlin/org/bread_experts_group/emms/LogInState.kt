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

import java.util.*
import kotlin.uuid.Uuid

class LogInState(
	val username: String,
	val uuid: Uuid,

	var verifyToken: ByteArray = ByteArray(4)
) {
	enum class ChatMode {
		ENABLED,
		COMMANDS_ONLY,
		HIDDEN
	}

	enum class SkinParts {
		CAPE,
		JACKET,
		LEFT_SLEEVE,
		RIGHT_SLEEVE,
		LEFT_PANTS,
		RIGHT_PANTS,
		HAT
	}

	enum class MainHand {
		LEFT, RIGHT
	}

	lateinit var locale: String
	var viewDistance: Int = -1
	lateinit var chatMode: ChatMode
	var chatColors: Boolean = false
	val displayedSkinParts: EnumSet<SkinParts> = EnumSet.noneOf(SkinParts::class.java)
	lateinit var mainHand: MainHand
	var textFiltering: Boolean = false
	var serverListingOnStatus: Boolean = false
}