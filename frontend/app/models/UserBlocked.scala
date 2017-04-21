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
  * This class describes the relation of an user and another user that has
  * been blocked by this user.
  *
  * @param user         The UUID of the user that has blocked the other user.
  * @param userBlocked  The UUID of the user that has been blocked.
  * @param created      The time when the block was created.
  */
final case class UserBlocked(
    user: UUID,
    userBlocked: UUID,
    created: ZonedDateTime
)
