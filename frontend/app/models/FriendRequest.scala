/*
 * Copyright (C) 2017  Jens Grassel & André Schütz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package models

import java.time.ZonedDateTime
import java.util.UUID

/**
  * This class describes a friends request from one user to another.
  *
  * @param user     The user that received the friends request.
  * @param friend   The user that has sent the friends request.
  * @param created  When the friends request was submitted.
  */
final case class FriendRequest(
    user: UUID,
    friend: UUID,
    created: ZonedDateTime
)
