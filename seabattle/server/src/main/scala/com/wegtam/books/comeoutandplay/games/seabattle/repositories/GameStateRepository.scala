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

package com.wegtam.books.comeoutandplay.games.seabattle.repositories

import java.util.UUID

import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameState

import scala.language.higherKinds

/**
  * Definition einer Repository-Algebra für `GameState`.
  *
  * @tparam F Ein Typ höherer Ordnung (higher kinded type) der in der Implementierung meist ein Monad oder Applicative ist.
  */
trait GameStateRepository[F[_]] {

  /**
    * Füge einen Spielstand zur Datenbank hinzu.
    *
    * @param gs Der Spielstand.
    * @return Die Anzahl gespeicherter Spielstände.
    */
  def add(gs: GameState): F[Int]

  /**
    * Lade einen Spielstand aus der Datenbank.
    *
    * @param gameId Die eindeutige ID des Spiels.
    * @return Eine Option auf den Spielstand.
    */
  def get(gameId: UUID): F[Option[GameState]]

  /**
    * Lade alle Spielstände von Spielen des Eigentümers mit der angegebenen
    * ID.
    *
    * @param ownerId Die ID des Spieleigentümers.
    * @return Eine List mit Spielständen.
    */
  def getOwn(ownerId: UUID): F[Seq[GameState]]

  /**
    * Lade alle Spielstände, von offenen Spielen, d.h. Spiele,
    * denen noch ein Mitspieler fehlt.
    *
    * Insofern eine Spieler-ID angegeben wird, werden nur offene
    * Spiele zurückgegeben, deren Eigentümer nicht der Spieler ist.
    *
    * @param playerId Eine optionale Spieler-ID.
    * @return Eine Liste mit Spielständen.
    */
  def getOpen(playerId: Option[UUID]): F[Seq[GameState]]

  /**
    * Lade alle Spielstände, an denen der Spieler beteiligt ist.
    *
    * @param finished Gibt an ob die Spiele beendet sein sollen oder nicht.
    * @param playerId Die ID eines Mitspielers.
    * @return Eine Liste mit Spielständen.
    */
  def getParticipating(finished: Boolean)(playerId: UUID): F[Seq[GameState]]

  /**
    * Lösche einen Spielstand aus der Datenbank.
    *
    * @param gameId Die eindeutige ID des Spiels.
    * @return Die Anzahl der gelöschten Spielstände.
    */
  def remove(gameId: UUID): F[Int]

  /**
    * Aktualisiere einen bereits existierenden Spielstand in der Datenbank.
    *
    * @param gs Der aktualisierte Spielstand.
    * @return Die Anzahl der aktualisierten Spielstände.
    */
  def update(gs: GameState): F[Int]

}
