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

package com.wegtam.books.comeoutandplay.games.seabattle.adt

import io.circe._
import io.circe.syntax._

sealed trait Orientation extends Product with Serializable

object Orientation {

  implicit val decode: Decoder[Orientation] = Decoder.decodeString.map {
    case "H" => Horizontal
    case "V" => Vertical
  }

  implicit val encode: Encoder[Orientation] = {
    case Horizontal => "H".asJson
    case Vertical   => "V".asJson
  }

  case object Horizontal extends Orientation

  case object Vertical extends Orientation

}
