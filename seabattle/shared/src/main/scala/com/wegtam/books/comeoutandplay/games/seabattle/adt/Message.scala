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

import io.circe._
import io.circe.generic.semiauto._

/**
  * A sealed trait for in game messages which are used to communicate
  * with the game server and the other player.
  *
  */
sealed trait Message extends Product with Serializable

object Message {

  implicit val decode: Decoder[Message] = deriveDecoder[Message]

  implicit val encode: Encoder[Message] = deriveEncoder[Message]

  /**
    * A wrapper for errors.
    *
    * @param summary A short summary of the error.
    * @param details An optional more detailed error description.
    */
  final case class GameError(summary: String, details: Option[String]) extends Message

  object GameError {

    /**
      * Create an error without details.
      *
      * @param summary A short summary of the error.
      * @return An error without details.
      */
    def apply(summary: String): GameError = GameError(
      summary = summary,
      details = None
    )

  }

  /**
    * Instruct the server to initialse a new game using the given player
    * as the game owner.
    *
    * @param ownerId The user id of the game owner.
    */
  final case class InitGame(ownerId: UUID) extends Message

  /**
    * The result for a successful game initialisiation.
    *
    * @param gameId  The id of the game.
    * @param ownerId The user id of the game owner.
    */
  final case class GameInitialised(gameId: UUID, ownerId: UUID) extends Message

  /**
    * Register a player for the game.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player that shall be registered.
    */
  final case class RegisterPlayer(gameId: UUID, playerId: UUID) extends Message

  /**
    * The result for a successful player registration.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player that has been registered.
    */
  final case class PlayerRegistered(gameId: UUID, playerId: UUID) extends Message

  /**
    * Request a freshly created game board from the server.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player requesting a board.
    */
  final case class CreateBoard(gameId: UUID, playerId: UUID) extends Message

  /**
    * The result of a successful board creation.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player requesting a board.
    * @param board    The created board.
    */
  final case class BoardCreated(gameId: UUID, playerId: UUID, board: Board) extends Message

  /**
    * Save the board to the server.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player that wants to save her board.
    * @param board    The board of the player.
    */
  final case class SaveBoard(gameId: UUID, playerId: UUID, board: Board) extends Message

  /**
    * The result of a successful board saving operation.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player that wants to save her board.
    * @param board    The board of the player.
    */
  final case class BoardSaved(gameId: UUID, playerId: UUID, board: Board) extends Message

  /**
    * Instruct the server that the player is ready for the game.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player who is ready.
    */
  final case class Ready(gameId: UUID, playerId: UUID) extends Message

  /**
    * Tells the player to wait for the other player.
    *
    * @param gameId The id of the game.
    */
  final case class WaitForOtherPlayer(gameId: UUID) extends Message

  /**
    * Tells the player to make a move.
    *
    * @param gameId     The id of the game.
    * @param board      The board of the player that shall move.
    * @param enemyBoard The board of the opponent.
    */
  final case class MakeMove(gameId: UUID, board: Board, enemyBoard: EnemyBoard) extends Message

  /**
    * The move of a player.
    *
    * @param gameId   The id of the game.
    * @param playerId The user id of the player that moves.
    * @param position The position that is attacked on the enemy board.
    */
  final case class Move(gameId: UUID, playerId: UUID, position: Position) extends Message

  /**
    * The result of a move.
    *
    * @param gameId     The id of the game.
    * @param board      The board of the player.
    * @param enemyBoard The board of the opponent.
    * @param continue   Indicates if the player can continue.
    * @param winner     An option to the id of the player who won the game.
    */
  final case class MoveResult(gameId: UUID,
                              board: Board,
                              enemyBoard: EnemyBoard,
                              continue: Boolean,
                              winner: Option[UUID])
      extends Message

  /**
    * A game over message.
    *
    * @param gameId     The id of the game.
    * @param board      The board of the player.
    * @param enemyBoard The board of the opponent.
    * @param winner     An option to the id of the player who won the game.
    */
  final case class GameOver(gameId: UUID,
                            board: Board,
                            enemyBoard: EnemyBoard,
                            winner: Option[UUID])
      extends Message

}
