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

package utils.route

import java.util.UUID

import play.api.mvc.PathBindable

/**
  * Some route binders.
  */
object Binders {

  /**
    * A `java.util.UUID` bindable.
    */
  implicit val UUIDPathBindable: PathBindable[UUID] = new PathBindable[UUID] {
    def bind(key: String, value: String): Either[String, UUID] =
      try {
        Right(UUID.fromString(value))
      } catch {
        case _: Exception =>
          Left("Cannot parse parameter '" + key + "' with value '" + value + "' as UUID")
      }

    def unbind(key: String, value: UUID): String = value.toString
  }
}
