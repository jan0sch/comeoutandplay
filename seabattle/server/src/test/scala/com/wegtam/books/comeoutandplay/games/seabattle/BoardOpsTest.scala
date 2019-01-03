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

package com.wegtam.books.comeoutandplay.games.seabattle

import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import com.wegtam.books.comeoutandplay.games.seabattle.BoardOps.syntax._
import org.scalatest.{ MustMatchers, WordSpec }

import scala.collection.immutable._

class BoardOpsTest extends WordSpec with MustMatchers {

  "getState" when {
    "board does not contain any ships" must {
      "return Empty" in {
        val b = Board.createDefaultBoard
        b.getState must be(BoardState.Empty)
      }
    }

    "board does not contain all ships" must {
      "return NotReady" in {
        val b = Board.createDefaultBoard.copy(
          ships = Seq(
            Ship.createShip(ShipClass.Battleship, Orientation.Horizontal, Position(0, 0)),
            Ship.createShip(ShipClass.Cruiser, Orientation.Horizontal, Position(5, 0)),
          )
        )
        b.getState must be(BoardState.NotReady)
      }
    }

    "board does contain all ships" must {
      "return Ready" in {
        val b = Board.createDefaultBoard.copy(
          ships = ShipClass.all
            .foldLeft(Seq.empty[Ship])(
              (acc, c) =>
                acc ++ Seq
                  .fill(c.maxShips)(Ship.createShip(c, Orientation.Horizontal, Position(0, 0)))
            )
        )
        b.getState must be(BoardState.Ready)
      }
    }
  }

}
