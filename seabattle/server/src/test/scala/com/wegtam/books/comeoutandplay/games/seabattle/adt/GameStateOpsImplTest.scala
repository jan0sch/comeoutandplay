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

import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameStateOps._
import org.scalatest.{ MustMatchers, WordSpec }

class GameStateOpsImplTest extends WordSpec with MustMatchers {

  "GameStateOpsImpl" when {
    "getBoard" when {
      "empty GameState" must {
        "return an empty Option" in {
          val gs = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
          GameStateOpsImpl.getBoard(gs)(gs.owner) mustBe empty
        }
      }

      "non empty GameState" must {
        "return the correct board" in {
          val a = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
          val b = a.copy(boards = Map(a.owner -> Board.createDefaultBoard))
          GameStateOpsImpl.getBoard(b)(b.owner) must contain(Board.createDefaultBoard)
        }
      }
    }

    "getEnemyBoard" when {
      "empty GameState" must {
        "return an empty Option" in {
          val gs = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
          GameStateOpsImpl.getEnemyBoard(gs)(UUID.randomUUID()) mustBe empty
        }
      }

      "non empty GameState" when {
        "GameBoard exists" when {
          "EnemyBoard does not exist" must {
            "return a fresh EnemyBoard" in {
              val sa = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
              val gb = Board.createDefaultBoard
              val eb = EnemyBoard.initialiseFromBoard(gb)
              val sb = sa.copy(boards = Map(sa.owner -> Board.createDefaultBoard))
              GameStateOpsImpl.getEnemyBoard(sb)(sb.owner) must contain(eb)
            }
          }

          "EnemyBoard does exist" must {
            "return the existing EnemyBoard" in {
              val sa = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
              val gb = Board.createDefaultBoard
              val eb =
                EnemyBoard.initialiseFromBoard(gb).update(Position(0, 1))(EnemyBoardFieldState.Sunk)
              val sb = sa.copy(boards = Map(sa.owner -> Board.createDefaultBoard),
                               enemyBoards = Map(sa.owner -> eb))
              GameStateOpsImpl.getEnemyBoard(sb)(sb.owner) must contain(eb)
            }
          }
        }

        "GameBoard does not exist" must {
          "return an empty Option" in {
            val gs = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
            GameStateOpsImpl.getEnemyBoard(gs)(UUID.randomUUID()) mustBe empty
          }
        }
      }
    }

    "getOtherPlayerId" when {
      "no other player" must {
        "return an empty Option" in {
          val gs = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
          GameStateOpsImpl.getOtherPlayerId(gs)(gs.owner) mustBe empty
        }
      }

      "other player exists" must {
        "return an option to the other players id" in {
          val p = UUID.randomUUID()
          val a = GameState.createEmpty(UUID.randomUUID())(UUID.randomUUID())
          val b = a.copy(opponent = Option(p))
          GameStateOpsImpl.getOtherPlayerId(b)(b.owner) must contain(p)
          GameStateOpsImpl.getOtherPlayerId(b)(p) must contain(b.owner)
        }
      }
    }
  }

}
