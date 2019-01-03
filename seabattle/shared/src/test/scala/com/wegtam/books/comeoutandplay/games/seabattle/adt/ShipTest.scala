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

import com.wegtam.books.comeoutandplay.games.seabattle.adt.Orientation.{ Horizontal, Vertical }
import com.wegtam.books.comeoutandplay.games.seabattle.adt.ShipClass._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.{ MustMatchers, WordSpec }
import org.scalatest.prop.PropertyChecks

import scala.collection.immutable.Seq

class ShipTest extends WordSpec with MustMatchers with PropertyChecks {

  private val orientations                                          = for { o <- Gen.oneOf(Seq(Horizontal, Vertical)) } yield o
  implicit private val arbitraryOrientation: Arbitrary[Orientation] = Arbitrary(orientations)
  private val positions = for {
    col <- Gen.choose(0, 10)
    row <- Gen.choose(0, 10)
  } yield Position(col, row)
  implicit private val arbitraryPosition: Arbitrary[Position] = Arbitrary(positions)
  implicit private val arbitraryPositions: Arbitrary[Seq[Position]] = Arbitrary(
    for (p <- Gen.listOfN(100, positions)) yield p
  )
  private val shipClasses = for { c <- Gen.oneOf(Seq(Battleship, Cruiser, Destroyer, Submarine)) } yield
    c
  implicit private val arbitraryShipClass: Arbitrary[ShipClass] = Arbitrary(shipClasses)
  private val ships = for {
    c <- shipClasses
    o <- orientations
    p <- positions
  } yield
    Ship(
      c = c,
      hits = Seq.empty,
      orientation = o,
      pos = p
    )
  implicit private val arbitraryShip: Arbitrary[Ship] = Arbitrary(ships)

  "createShip" must {
    "create correct ships" in {
      forAll("shipClass", "orientation", "position") {
        (sc: ShipClass, o: Orientation, p: Position) =>
          val s = Ship.createShip(sc, o, p)
          s.c must be(sc)
          s.hits must be(Seq.empty)
          s.orientation must be(o)
          s.pos must be(p)
      }
    }
  }

  "hit" when {
    "ship is hit" must {
      "return true" in {
        forAll("ship") { s: Ship =>
          val ps: Seq[Position] =
            s.orientation match {
              case Horizontal =>
                for (cs <- 0 until s.c.length) yield s.pos.copy(column = s.pos.column + cs)
              case Vertical =>
                for (rs <- 0 until s.c.length) yield s.pos.copy(row = s.pos.row + rs)
            }
          ps.foreach { p =>
            withClue(s"Ship must be hit at $p!")(Ship.hit(p, s) must be(true))
          }
        }
      }
    }

    "ship is not hit" must {
      "return false" in {
        def isSafe(s: Ship, p: Position): Boolean = s.orientation match {
          case Horizontal =>
            s.pos.row != p.row || p.column < s.pos.column || (s.pos.column + s.c.length) <= p.column
          case Vertical =>
            s.pos.column != p.column || p.row < s.pos.row || (s.pos.row + s.c.length) <= p.row
        }

        forAll("ship", "positions") { (s: Ship, ps: Seq[Position]) =>
          ps.foreach { h =>
            if (isSafe(s, h))
              withClue(s"Ship must not be hit at $h!")(Ship.hit(h, s) must be(false))
            else
              withClue(s"Ship must be hit at $h!")(Ship.hit(h, s) must be(true))
          }
        }
      }
    }
  }
}
