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

import cats.syntax.eq._
import com.wegtam.books.comeoutandplay.games.seabattle._
import com.wegtam.books.comeoutandplay.games.seabattle.BoardOps.syntax._
import io.circe._
import io.circe.generic.semiauto._

import scala.collection.immutable._

/**
  * Ein Spielstand.
  *
  * @param gameId   The UUID of the game.
  * @param owner    The UUID of the user that created the game.
  * @param opponent An option to the UUID of the user that is the opponent.
  * @param moves    The history of moves.
  * @param boards   The initial boards of the players.
  * @param enemyBoards Die "Gegnerspielfelder".
  * @param ready    The list of players that have acknowledged that they are ready for the game.
  * @param running  Indicates if the game is still running.
  * @param winner   An option to the winner of the game. Remains empty if the game was aborted.
  */
final case class GameState(
    gameId: UUID,
    owner: UUID,
    opponent: Option[UUID],
    moves: Seq[PlayerMove],
    boards: Map[UUID, Board],
    enemyBoards: Map[UUID, EnemyBoard],
    ready: Seq[UUID],
    running: Boolean,
    winner: Option[UUID]
)

object GameState {

  implicit val decode: Decoder[GameState] = deriveDecoder[GameState]

  implicit val encode: Encoder[GameState] = deriveEncoder[GameState]

  /**
    * Create an empty game state.
    *
    * @param gameId The UUID of the game.
    * @param owner  The UUID of the user that created the game.
    * @return A game state with no player or whats 'o ever which is not running.
    */
  def createEmpty(gameId: UUID)(owner: UUID): GameState = GameState(
    gameId = gameId,
    owner = owner,
    opponent = None,
    moves = Seq.empty,
    boards = Map.empty,
    enemyBoards = Map.empty,
    ready = Seq.empty,
    running = false,
    winner = None
  )

  /**
    * Apply the given list of moves.
    *
    * @param bs The boards mapped to their owners.
    * @param ms The moves of the players.
    * @return The updated boards mapped to their owners.
    */
  def applyMoves(bs: Map[UUID, Board])(ms: Seq[PlayerMove]): Map[UUID, Board] =
    bs.map { p =>
      val (player, board) = p
      val moves           = ms.filterNot(_.playerId === player).map(_.position)
      player -> applyMoves(board)(moves)
    }

  /**
    * Apply the given list of moves (attacked positions) to the given board.
    *
    * @param b  A game board.
    * @param ps A list of positions which were attacked.
    * @return A board with all the moves applied.
    */
  def applyMoves(b: Board)(ps: Seq[Position]): Board =
    ps.foldLeft(b)((nb, np) => nb.applyMove(np))

}

/**
  * A move which describes the game history.
  *
  * @param playerId The player which moved.
  * @param position The position on the other player's board which was attacked.
  */
final case class PlayerMove(
    playerId: UUID,
    position: Position
)

object PlayerMove {

  implicit val decode: Decoder[PlayerMove] = deriveDecoder[PlayerMove]

  implicit val encode: Encoder[PlayerMove] = deriveEncoder[PlayerMove]

}
