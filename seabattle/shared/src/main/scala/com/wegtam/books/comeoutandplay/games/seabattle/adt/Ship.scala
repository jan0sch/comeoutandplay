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

import cats.instances.int._
import cats.syntax.eq._
import io.circe._

import scala.collection.immutable.Seq

/**
  * A ship that can be placed on a board.
  *
  * @param c           The class of the ship.
  * @param hits        The hits the ship has taken.
  * @param orientation The orientation of the ship on the board.
  * @param pos         The position of the ship. A position is always considered to be "upper left" of the ship.
  */
final case class Ship(c: ShipClass, hits: Seq[Position], orientation: Orientation, pos: Position) {

  def isSunk: Boolean = hits.lengthCompare(c.length) === 0

}

object Ship {

  implicit val decode: Decoder[Ship] =
    Decoder.forProduct4("c", "hs", "o", "p")(Ship.apply)

  implicit val encode: Encoder[Ship] =
    Encoder.forProduct4("c", "hs", "o", "p")(s => (s.c, s.hits, s.orientation, s.pos))

  /**
    * Create a new ship with no hits.
    *
    * @param c The class of the ship.
    * @param o The orientation of the ship on the board.
    * @param p The position of the ship.
    * @return A new ship.
    */
  def createShip(c: ShipClass, o: Orientation, p: Position): Ship =
    Ship(
      c = c,
      hits = Seq.empty,
      orientation = o,
      pos = p
    )

  /**
    * Check if the ship has been hit by the shot at the given position.
    *
    * @param p The position of the shot.
    * @return True if the ship has been hit and false otherwise.
    */
  def hit(p: Position, s: Ship): Boolean = s.orientation match {
    case Orientation.Horizontal =>
      p.row === s.pos.row && s.pos.column <= p.column && p.column < (s.pos.column + s.c.length)
    case Orientation.Vertical =>
      p.column === s.pos.column && s.pos.row <= p.row && p.row < (s.pos.row + s.c.length)
  }

}
