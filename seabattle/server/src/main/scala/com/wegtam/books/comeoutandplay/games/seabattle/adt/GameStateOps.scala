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

import cats.implicits._

trait GameStateOps {

  /**
    * Return the board of the player with the given id.
    *
    * @param gs       The game state.
    * @param playerId The id of the player whos board shall be returned.
    * @return The board of the player.
    */
  def getBoard(gs: GameState)(playerId: UUID): Option[Board]

  /**
    * Return the `EnemyBoard` representation for the given player.
    *
    * @param gs       The game state.
    * @param playerId A player id.
    * @return The enemy board of the player.
    */
  def getEnemyBoard(gs: GameState)(playerId: UUID): Option[EnemyBoard]

  /**
    * Return an option to the id of the other player.
    *
    * @param gs       The game state.
    * @param playerId A player id.
    * @return An option to the id of the other player if two players are registered.
    */
  def getOtherPlayerId(gs: GameState)(playerId: UUID): Option[UUID]

}

object GameStateOps {

  implicit val GameStateOpsImpl: GameStateOps = new GameStateOps {
    override def getBoard(gs: GameState)(playerId: UUID): Option[Board] =
      gs.boards.get(playerId)

    override def getEnemyBoard(gs: GameState)(playerId: UUID): Option[EnemyBoard] =
      gs.enemyBoards
        .get(playerId)
        .fold(getBoard(gs)(playerId).map(EnemyBoard.initialiseFromBoard))(eb => eb.some)

    override def getOtherPlayerId(gs: GameState)(playerId: UUID): Option[UUID] =
      gs.opponent.map { oid =>
        if (oid === playerId)
          gs.owner
        else
          oid
      }
  }

  object syntax {

    implicit final class WrapGameStateOps(private val gs: GameState) extends AnyVal {
      def getBoard(playerId: UUID)(implicit ev: GameStateOps): Option[Board] =
        ev.getBoard(gs)(playerId)

      def getEnemyBoard(playerId: UUID)(implicit ev: GameStateOps): Option[EnemyBoard] =
        ev.getEnemyBoard(gs)(playerId)

      def getOtherPlayerId(playerId: UUID)(implicit ev: GameStateOps): Option[UUID] =
        ev.getOtherPlayerId(gs)(playerId)
    }

  }

}
