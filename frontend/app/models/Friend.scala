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
  * This class describes a relation between a user and another user
  * that are friends.
  *
  * @param user     The UUID of the user.
  * @param friend   The UUID of the user that is a friend.
  * @param created  When the friendship has begun.
  */
final case class Friend(
    user: UUID,
    friend: UUID,
    created: ZonedDateTime
)

/**
  * This class holds additional information to a specific user.
  *
  * @param user      The user that could be a friend.
  * @param friend    Whether the user already is a friend.
  * @param request   Whether there exists a friends request to the user.
  * @param blocked   Whether the user was blocked by the signed in user.
  * @param blockedMe Whether the user has blocked the signed in user.
  */
final case class FriendWithInfo(
    user: User,
    friend: Option[Friend],
    request: Option[FriendRequest],
    blocked: Option[UserBlocked],
    blockedMe: Option[UserBlocked]
)
