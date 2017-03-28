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

import java.util.UUID

import org.scalatest.{ MustMatchers, WordSpec }

import scala.collection.immutable._

class GameStateTest extends WordSpec with MustMatchers {

  "GameState" when {
    "using createEmpty" must {
      "return an empty GameState" in {
        val e = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
        val expected = GameState(
          gameId = e.gameId,
          owner = e.owner,
          opponent = None,
          moves = Seq.empty,
          boards = Map.empty,
          enemyBoards = Map.empty,
          ready = Seq.empty,
          running = false,
          winner = None
        )
        e must be(expected)
      }
    }
  }

}
