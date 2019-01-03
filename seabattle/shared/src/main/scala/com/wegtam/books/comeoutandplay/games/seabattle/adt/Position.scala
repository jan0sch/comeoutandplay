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
import cats.kernel.Eq
import cats.syntax.eq._
import io.circe._

/**
  * A position on the game board.
  *
  * @param column The column number on the board (starting with zero!).
  * @param row    The row number on the board (starting with zero!).
  */
final case class Position(column: Int, row: Int) {
  override def toString: String = s"Position(column = $column, row = $row)"
}

object Position {

  implicit val decode: Decoder[Position] =
    Decoder.forProduct2("c", "r")(Position.apply)

  implicit val encode: Encoder[Position] =
    Encoder.forProduct2("c", "r")(p => (p.column, p.row))

  implicit val eqPosition: Eq[Position] = Eq.instance[Position] { (a, b) =>
    a.column === b.column && a.row === b.row
  }

}
