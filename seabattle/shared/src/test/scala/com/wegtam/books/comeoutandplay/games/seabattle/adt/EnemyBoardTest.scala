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

import com.wegtam.books.comeoutandplay.games.seabattle.adt.EnemyBoardFieldState._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ MustMatchers, WordSpec }

import scala.collection.immutable.Seq

class EnemyBoardTest extends WordSpec with MustMatchers with PropertyChecks {

  private val defaultBoard = Board.createDefaultBoard
  private val positions = for {
    r <- Gen.choose(0, defaultBoard.rows)
    c <- Gen.choose(0, defaultBoard.columns)
  } yield Position(c, r)
  implicit private val arbitraryPosition: Arbitrary[Position] = Arbitrary(positions)
  implicit private val arbitraryPositions: Arbitrary[Seq[Position]] = Arbitrary(
    Gen.listOf(positions)
  )
  private val states                                                   = for (s <- Gen.oneOf(List(Hit, Sunk, Unknown, Water))) yield s
  implicit private val arbitraryState: Arbitrary[EnemyBoardFieldState] = Arbitrary(states)

  "Initialising from a given board" must {
    "return an enemy board full of unknown columns" in {
      val e = EnemyBoard.initialiseFromBoard(defaultBoard)
      e.rows.forall(cs => cs.forall(_ === EnemyBoardFieldState.Unknown)) must be(true)
      withClue("Wrong number of rows generated!")(e.rows.size must be(defaultBoard.rows + 1))
      withClue("Wrong number of columns generated!")(
        e.rows.foreach(r => r.size must be(defaultBoard.columns + 1))
      )
    }
  }

  "Updating a field" must {
    "return a new board with the updated field" in {
      forAll("position", "state") { (p: Position, s: EnemyBoardFieldState) =>
        val e = EnemyBoard.initialiseFromBoard(defaultBoard)
        val u = EnemyBoard.update(e)(p)(s)
        u.rows(p.row)(p.column) must be(s)
      }
    }
  }

  "isFinished" when {
    "all fields are known" must {
      "return true" in {
        val eb = EnemyBoard.initialiseFromBoard(defaultBoard)
        val ps = for {
          r <- 0 to defaultBoard.rows
          c <- 0 to defaultBoard.columns
        } yield Position(c, r)
        val b = ps.foldLeft(eb)((b, p) => b.update(p)(EnemyBoardFieldState.Water))
        withClue(s"Enemy board with no unknown fields must be finished! $b") {
          EnemyBoard.isFinished(b) must be(true)
        }
      }
    }

    "some fields are unknown" must {
      "return false" in {
        forAll("positions") { ps: Seq[Position] =>
          val eb = EnemyBoard.initialiseFromBoard(defaultBoard)
          val b  = ps.foldLeft(eb)((b, p) => b.update(p)(EnemyBoardFieldState.Water))
          whenever(b.rows.exists(cs => cs.contains(EnemyBoardFieldState.Unknown))) {
            EnemyBoard.isFinished(b) must be(false)
          }
        }
      }
    }
  }

}
