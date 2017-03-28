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

import cats._
import cats.instances.int._
import cats.syntax.eq._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._

import scala.collection.immutable._
import scala.language.higherKinds

trait BoardOps[F[_]] {

  /**
    * Apply the given move (position) to the given board and
    * return the result board.
    *
    * @param b A game board.
    * @param p A position.
    * @return The resulting board.
    */
  def applyMove(b: F[Board])(p: Position): F[Board]

  /**
    * Apply the given list of moves (actual target positions) to the
    * given board and return the result board.
    *
    * @param b  A game board.
    * @param ps A list of positions.
    * @return The resulting board.
    */
  def applyMoves(b: F[Board])(ps: Seq[Position]): F[Board]

  /**
    * Gibt den Status des Spielbretts zurück.
    *
    * @param b Ein Spielbrett.
    * @return Der Status des Spielbretts.
    */
  def getState(b: F[Board]): F[BoardState]

}

object BoardOps {

  implicit val BoardOpsImpl: BoardOps[Id] = new BoardOps[Id] {
    override def applyMove(b: Id[Board])(p: Position): Id[Board] = {
      val ships = b.ships.map { s =>
        if (Ship.hit(p, s))
          s.copy(hits = s.hits.filterNot(_ === p) :+ p)
        else
          s
      }
      b.copy(ships = ships)
    }

    override def applyMoves(b: Id[Board])(ps: Seq[Position]): Id[Board] =
      ps.foldLeft(b)((nboard, npos) => applyMove(nboard)(npos))

    override def getState(b: Id[Board]): Id[BoardState] = b.ships match {
      case Nil => BoardState.Empty
      case ships =>
        if (ShipClass.all.forall(c => ships.count(_.c === c) === c.maxShips)) {
          if (ships.forall(_.isSunk)) BoardState.Finished else BoardState.Ready
        } else
          BoardState.NotReady
    }
  }

  object syntax {
    implicit final class WrapBoardOps(private val b: Board) extends AnyVal {

      /**
        * Apply the given move (position) to the given board and
        * return the result board.
        *
        * @param p A position.
        * @return The resulting board.
        */
      def applyMove(p: Position)(implicit ev: BoardOps[Id]): Id[Board] =
        ev.applyMove(b)(p)

      /**
        * Apply the given list of moves (actual target positions) to the
        * given board and return the result board.
        *
        * @param ps A list of positions.
        * @return The resulting board.
        */
      def applyMoves(ps: Seq[Position])(implicit ev: BoardOps[Id]): Id[Board] =
        ev.applyMoves(b)(ps)

      /**
        * Gibt den Status des Spielbretts zurück.
        *
        * @return Der Status des Spielbretts.
        */
      def getState(implicit ev: BoardOps[Id]): Id[BoardState] =
        ev.getState(b)
    }
  }

}
