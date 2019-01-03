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

package utils

import cats._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameState
import models.GameStateLight

import scala.collection.immutable._
import scala.language.higherKinds

/**
  * Type class für Operationen auf `GameState`.
  */
abstract class GameStateConverter[F[_]] {

  /**
    * Konvertiere einen Spielstand in einen reduzierten Spielstand.
    *
    * @param gs Ein Spielstand.
    * @return Ein reduzierter Spielstand im selben Container.
    */
  def toGameStateLight(gs: F[GameState]): F[GameStateLight]

}

object GameStateConverter {

  implicit val IdGameStateConverter: GameStateConverter[Id] = (gs: Id[GameState]) =>
    GameStateLight.fromGameState(gs)

  implicit val SeqGameStateConverter: GameStateConverter[Seq] = (gs: Seq[GameState]) =>
    gs.map(GameStateLight.fromGameState)

  object syntax {

    implicit final class WrapSeqGameStateConverter(val gs: Seq[GameState]) extends AnyVal {
      def light(implicit ev: GameStateConverter[Seq]): Seq[GameStateLight] =
        ev.toGameStateLight(gs)
    }

  }

}
