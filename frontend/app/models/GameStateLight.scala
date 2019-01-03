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

package models

import java.util.UUID

import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameState

/**
  * Eine reduzierte Fassung eines Spielstands, die in den Views verwendet
  * werden kann.
  *
  * @param gameId   The UUID of the game.
  * @param owner    The UUID of the user that created the game.
  * @param opponent An option to the UUID of the user that is the opponent.
  * @param lastMove An option to the UUID of the player that made the last move.
  * @param moves    The number of moves that were made.
  * @param ready    The list of players that have acknowledged that they are ready for the game.
  * @param running  Indicates if the game is still running.
  * @param winner   An option to the winner of the game. Remains empty if the game was aborted.
  */
final case class GameStateLight(
    gameId: UUID,
    owner: UUID,
    opponent: Option[UUID],
    lastMove: Option[UUID],
    moves: Int,
    ready: Seq[UUID],
    running: Boolean,
    winner: Option[UUID]
)

object GameStateLight {

  /**
    * Erstellt einen reduzierten Spielstand aus einem vollständigen.
    *
    * @param gs Ein Spielstand.
    * @return Eine reduzierte Fassung des Spielstands.
    */
  def fromGameState(gs: GameState): GameStateLight =
    GameStateLight(
      gameId = gs.gameId,
      owner = gs.owner,
      opponent = gs.opponent,
      lastMove = gs.moves.reverse.headOption.map(_.playerId),
      moves = gs.moves.length,
      ready = gs.ready,
      running = gs.running,
      winner = gs.winner
    )
}
