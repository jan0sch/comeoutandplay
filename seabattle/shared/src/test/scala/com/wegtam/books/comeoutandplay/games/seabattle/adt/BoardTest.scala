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
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ MustMatchers, WordSpec }

import scala.collection.immutable.Seq

class BoardTest extends WordSpec with MustMatchers with PropertyChecks {

  private val defaultBoard = Board.createDefaultBoard
  private val classes =
    for (c <- Gen.oneOf(
           List(ShipClass.Battleship, ShipClass.Cruiser, ShipClass.Destroyer, ShipClass.Submarine)
         )) yield c
  implicit private val arbitraryShipClass: Arbitrary[ShipClass] = Arbitrary(classes)
  private val orientations =
    for (o <- Gen.oneOf(List(Orientation.Horizontal, Orientation.Vertical))) yield o
  implicit private val arbitraryOrientation: Arbitrary[Orientation] = Arbitrary(orientations)
  private val positions = for {
    r <- Gen.choose(0, defaultBoard.rows)
    c <- Gen.choose(0, defaultBoard.columns)
  } yield Position(c, r)
  implicit private val arbitraryPosition: Arbitrary[Position] = Arbitrary(positions)
  implicit private val arbitraryPositions: Arbitrary[Seq[Position]] = Arbitrary(
    Gen.listOf(positions)
  )
  private val ships = for {
    c <- classes
    o <- orientations
    p <- positions
  } yield Ship.createShip(c, o, p)
  implicit private val arbitraryShips: Arbitrary[Ship] = Arbitrary(ships)

  "Creating a default board" must {
    "return an empty 10x10 board" in {
      val b = defaultBoard
      b.ships mustBe empty
      b.columns mustEqual 9
      b.rows mustEqual 9
    }
  }

  "placeShip" must {
    "place a ship on an empty board" in {
      forAll("position", "shipClass", "orientation") {
        (pos: Position, c: ShipClass, o: Orientation) =>
          val b      = defaultBoard
          val maxRow = if (o === Orientation.Vertical) pos.row + c.length else pos.row
          val maxCol = if (o === Orientation.Horizontal) pos.column + c.length else pos.column
          whenever(maxRow <= b.rows && maxCol <= b.columns) {
            Board.placeShip(b, c, o, pos) match {
              case Left(_)  => fail(s"Placing a ship ($c) at $pos must succeed!")
              case Right(_) => // We expect success.
            }
          }
      }
    }

    "place a ship on the borders of an empty board" in {
      forAll("shipClass", "orientation") { (c: ShipClass, o: Orientation) =>
        val b = defaultBoard
        val pos = o match {
          case Orientation.Horizontal => Position(b.columns - c.length + 1, 0)
          case Orientation.Vertical   => Position(0, b.rows - c.length + 1)
        }
        Board.placeShip(b, c, o, pos) match {
          case Left(_)  => fail(s"Placing a ship $c($o) at $pos must succeed!")
          case Right(_) => // We expect success.
        }
      }
    }

    "place not colliding ships on a board" in {
      val board = for {
        b <- Board.placeShip(defaultBoard,
                             ShipClass.Battleship,
                             Orientation.Vertical,
                             Position(5, 4))
        c <- Board.placeShip(b, ShipClass.Submarine, Orientation.Horizontal, Position(3, 3))
      } yield c
      board match {
        case Left(_)  => fail("Could not place ship!")
        case Right(_) =>
      }
    }

    "error on colliding ships" in {
      val b = defaultBoard
      Board.placeShip(b, ShipClass.Destroyer, Orientation.Horizontal, Position(2, 2)) match {
        case Left(e) => fail(e.summary)
        case Right(nb) =>
          nb.ships must contain(
            Ship.createShip(ShipClass.Destroyer, Orientation.Horizontal, Position(2, 2))
          )
          Board.placeShip(nb, ShipClass.Submarine, Orientation.Vertical, Position(2, 2)) match {
            case Left(t)  => t.summary must include("collides")
            case Right(_) => fail("Must return an error!")
          }
      }
    }

    "error on touching ships" in {
      val s1 = Ship.createShip(ShipClass.Cruiser, Orientation.Horizontal, Position(0, 0))
      val s2 = Ship.createShip(ShipClass.Destroyer, Orientation.Horizontal, Position(0, 1))
      val b  = defaultBoard
      Board.placeShip(b, s1.c, s1.orientation, s1.pos) match {
        case Left(e) => fail(e.summary)
        case Right(nb) =>
          nb.ships must contain(s1)
          withClue(s"Placing $s1 besides $s2 must produce an error!") {
            Board.placeShip(nb, s2.c, s2.orientation, s2.pos) match {
              case Left(t)  => t.summary must include("collides")
              case Right(_) => fail("Touching ships must produce an error!")
            }
          }
      }
    }

    "error on illegal start coordinates" in {
      forAll("column", "row", "shipClass", "orientation") {
        (col: Int, row: Int, c: ShipClass, o: Orientation) =>
          val b = defaultBoard
          whenever(col < 0 || b.columns < col || row < 0 || b.rows < row) {
            val pos = Position(col, row)
            Board.placeShip(b, c, o, pos) match {
              case Left(t)  => t.summary must include("Illegal position")
              case Right(_) => fail(s"Placing a ship ($c) at $pos must fail!")
            }
          }
      }
    }

    "error if ship does not fit on the board" in {
      forAll("position", "shipClass", "orientation") {
        (pos: Position, c: ShipClass, o: Orientation) =>
          val b      = defaultBoard
          val maxRow = if (o === Orientation.Vertical) pos.row + c.length - 1 else pos.row
          val maxCol = if (o === Orientation.Horizontal) pos.column + c.length - 1 else pos.column
          whenever(b.rows < maxRow || b.columns < maxCol) {
            Board.placeShip(b, c, o, pos) match {
              case Left(t)  => t.summary must include("Ship does not fit onto game board!")
              case Right(_) => fail(s"Placing a ship ($c) at $pos must fail!")
            }
          }
      }
    }
  }

  "hit" when {
    "ship is hit" must {
      "return true" in {
        Board.placeShip(defaultBoard, ShipClass.Battleship, Orientation.Vertical, Position(0, 0)) match {
          case Left(_) => fail("Could not place ship!")
          case Right(b) =>
            for {
              row <- 0 to b.rows
            } yield {
              if (row < ShipClass.Battleship.length)
                withClue(s"Position(0, $row) has a ship!")(
                  Board.hit(b, Position(0, row)) must be(true)
                )
              else
                withClue(s"Position(0, $row) has no ship!")(
                  Board.hit(b, Position(0, row)) must be(false)
                )
            }
        }
      }
    }

    "ship is not hit" must {
      "return false" in {
        Board.placeShip(defaultBoard, ShipClass.Battleship, Orientation.Vertical, Position(0, 0)) match {
          case Left(_) => fail("Could not place ship!")
          case Right(b) =>
            for {
              row <- 0 to b.rows
              col <- 1 to b.columns
            } yield {
              Board.hit(b, Position(col, row)) must be(false)
              if (row < ShipClass.Battleship.length)
                withClue(s"Position(0, $row) has a ship!")(
                  Board.hit(b, Position(0, row)) must be(true)
                )
              else
                withClue(s"Position(0, $row) has no ship!")(
                  Board.hit(b, Position(0, row)) must be(false)
                )
            }
        }
      }
    }
  }

}
