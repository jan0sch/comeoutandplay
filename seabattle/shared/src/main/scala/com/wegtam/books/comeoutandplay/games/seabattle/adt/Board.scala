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

import cats.syntax.either._
import cats.syntax.eq._
import io.circe._

import scala.collection.immutable.Seq

final case class Board(columns: Int, rows: Int, ships: Seq[Ship])

object Board {

  implicit val decode: Decoder[Board] =
    Decoder.forProduct3("c", "r", "s")(Board.apply)

  implicit val encode: Encoder[Board] =
    Encoder.forProduct3("c", "r", "s")(b => (b.columns, b.rows, b.ships))

  def calcShipCoordinates(o: Orientation, p: Position, sc: ShipClass): Seq[Position] =
    o match {
      case Orientation.Horizontal => for (c <- 0 to sc.length) yield Position(c, p.row)
      case Orientation.Vertical   => for (r <- 0 to sc.length) yield Position(p.column, r)
    }

  def calcShipBufferCoords(b: Board, o: Orientation, p: Position, sc: ShipClass): Seq[Position] = {
    val coords = o match {
      case Orientation.Horizontal =>
        for (c <- 0 to sc.length) yield List(Position(c, p.row - 1), Position(c, p.row + 1))
      case Orientation.Vertical =>
        for (r <- 0 to sc.length) yield List(Position(p.column - 1, r), Position(p.column + 1, r))
    }
    coords.flatten.filterNot(
      p => p.column < 0 || p.row < 0 || b.columns < p.column || b.rows < p.row
    )
  }

  /**
    * Create a default game board (10x10) which is empty.
    *
    * Actually we need to specify 9 because we start with position `0,0`.
    *
    * @return An empty game board.
    */
  def createDefaultBoard: Board = Board(
    columns = 9,
    rows = 9,
    ships = Seq.empty
  )

  /**
    * Calculate the positions that must be free on the game board to place
    * a ship specified by the given parameters.
    *
    * @param o  The orientation of the ship.
    * @param p  The start position of the ship (first coordinate).
    * @param sc The class of the ship.
    * @return A list of positions that will need to be free on the game board.
    */
  def calculateNeededPositions(o: Orientation, p: Position, sc: ShipClass): Seq[Position] =
    o match {
      case Orientation.Horizontal =>
        val fp = Seq(p.copy(column = p.column - 1))
        val sp = (0 until sc.length).flatMap { c =>
          Seq(
            p.copy(column = p.column + c, row = p.row - 1),
            p.copy(column = p.column + c),
            p.copy(column = p.column + c, row = p.row + 1)
          )
        }
        val lp = Seq(p.copy(column = p.column + sc.length))
        fp ++ sp ++ lp
      case Orientation.Vertical =>
        val fp = Seq(p.copy(row = p.row - 1))
        val sp = (0 until sc.length).flatMap { r =>
          Seq(
            p.copy(column = p.column - 1, row = p.row + r),
            p.copy(row = p.row + r),
            p.copy(column = p.column + 1, row = p.row + r)
          )
        }
        val lp = Seq(p.copy(row = p.row + sc.length))
        fp ++ sp ++ lp
    }

  /**
    * Place a ship of the desired class using the given parameters
    * on the provided board.
    *
    * <p>The following rules apply for placing a ship:</p>
    *
    * {{{
    * 1. Ships are not allowed "to touch".
    * 2. Ships are allowed to be placed at the border.
    * 3. Ships are not allowed to be placed diagonally.
    * }}}
    *
    * @param b  The game board.
    * @param sc The class of the ship.
    * @param o  The orientation of the ship on the board.
    * @param p  The position to place the ship.
    * @return Either a new board or an error.
    */
  def placeShip(b: Board, sc: ShipClass, o: Orientation, p: Position): Either[ErrorInfo, Board] =
    if (b.ships.count(_.c === sc) >= sc.maxShips)
      ErrorInfo("Maximum number of ships exceeded for the class!").asLeft
    else if (p.column < 0 || p.column > b.columns || p.row < 0 || p.row > b.rows)
      ErrorInfo("Illegal position!").asLeft
    else {
      val (maxCol, maxRow) = o match {
        case Orientation.Horizontal => (p.column + sc.length - 1, p.row)
        case Orientation.Vertical   => (p.column, p.row + sc.length - 1)
      }
      val neededPositions: Seq[Position] = calculateNeededPositions(o, p, sc)
      if (b.rows < maxRow || b.columns < maxCol)
        ErrorInfo("Illegal position! Ship does not fit onto game board!").asLeft
      else if (neededPositions.exists(p => Board.hit(b, p)))
        ErrorInfo(s"Ship ($sc, $o, $p) collides with existing ships!").asLeft
      else
        b.copy(ships = b.ships :+ Ship.createShip(sc, o, p)).asRight
    }

  /**
    * Return if the shot at the given position has hit a ship.
    *
    * @param b The game board.
    * @param p The position of the shot.
    * @return True if a hit has been scored and false otherwise.
    */
  def hit(b: Board, p: Position): Boolean = b.ships.exists(s => Ship.hit(p, s))

}
