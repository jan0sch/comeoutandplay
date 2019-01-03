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

import cats.kernel.Eq
import io.circe._
import io.circe.syntax._

import scala.collection.immutable.Seq

/**
  * A sealed trait for the allowed ship classes.
  */
sealed trait ShipClass {

  val length: Int

  val maxShips: Int

}

object ShipClass {
  final val all: Seq[ShipClass] = Seq(Battleship, Cruiser, Destroyer, Submarine)

  implicit val ShipClassEq: Eq[ShipClass] = Eq.instance[ShipClass] { (a, b) =>
    (a, b) match {
      case (Battleship, Battleship) => true
      case (Cruiser, Cruiser)       => true
      case (Destroyer, Destroyer)   => true
      case (Submarine, Submarine)   => true
      case _                        => false
    }
  }

  implicit val decode: Decoder[ShipClass] = Decoder.decodeString.map {
    case "Battleship" => Battleship
    case "Cruiser"    => Cruiser
    case "Destroyer"  => Destroyer
    case "Submarine"  => Submarine
  }

  implicit val encode: Encoder[ShipClass] = {
    case Battleship => "Battleship".asJson
    case Cruiser    => "Cruiser".asJson
    case Destroyer  => "Destroyer".asJson
    case Submarine  => "Submarine".asJson
  }

  case object Battleship extends ShipClass {
    override val length: Int   = 5
    override val maxShips: Int = 1
  }

  case object Cruiser extends ShipClass {
    override val length: Int   = 4
    override val maxShips: Int = 2
  }

  case object Destroyer extends ShipClass {
    override val length: Int   = 3
    override val maxShips: Int = 3
  }

  case object Submarine extends ShipClass {
    override val length: Int   = 2
    override val maxShips: Int = 4
  }

}
