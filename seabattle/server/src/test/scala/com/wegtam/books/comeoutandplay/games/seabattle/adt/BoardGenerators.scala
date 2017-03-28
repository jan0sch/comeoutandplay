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

import org.scalacheck.{ Arbitrary, Gen }

import scala.collection.immutable._

object BoardGenerators {

  val ValidGameBoardA: Board = Board.createDefaultBoard.copy(
    ships = Seq(
      Ship.createShip(ShipClass.Battleship, Orientation.Horizontal, Position(0, 0)),
      Ship.createShip(ShipClass.Cruiser, Orientation.Horizontal, Position(6, 2)),
      Ship.createShip(ShipClass.Cruiser, Orientation.Vertical, Position(6, 6)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Vertical, Position(1, 2)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Vertical, Position(8, 4)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Vertical, Position(2, 6)),
      Ship.createShip(ShipClass.Submarine, Orientation.Horizontal, Position(3, 3)),
      Ship.createShip(ShipClass.Submarine, Orientation.Horizontal, Position(8, 8)),
      Ship.createShip(ShipClass.Submarine, Orientation.Vertical, Position(4, 5)),
      Ship.createShip(ShipClass.Submarine, Orientation.Vertical, Position(0, 8))
    )
  )

  val ValidGameBoardB: Board = Board.createDefaultBoard.copy(
    ships = Seq(
      Ship.createShip(ShipClass.Battleship, Orientation.Vertical, Position(8, 5)),
      Ship.createShip(ShipClass.Cruiser, Orientation.Vertical, Position(0, 0)),
      Ship.createShip(ShipClass.Cruiser, Orientation.Vertical, Position(2, 1)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Horizontal, Position(4, 1)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Horizontal, Position(6, 3)),
      Ship.createShip(ShipClass.Destroyer, Orientation.Horizontal, Position(1, 6)),
      Ship.createShip(ShipClass.Submarine, Orientation.Horizontal, Position(0, 8)),
      Ship.createShip(ShipClass.Submarine, Orientation.Horizontal, Position(3, 9)),
      Ship.createShip(ShipClass.Submarine, Orientation.Vertical, Position(6, 6)),
      Ship.createShip(ShipClass.Submarine, Orientation.Vertical, Position(4, 4))
    )
  )

  val boardGen: Gen[Board] = for {
    b <- Gen.oneOf(List(ValidGameBoardA, ValidGameBoardB))
  } yield b

  implicit val arbitraryBoard: Arbitrary[Board] = Arbitrary(boardGen)

}
