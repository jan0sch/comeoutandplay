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

package models.daos

import java.util.UUID

import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameState
import com.wegtam.books.comeoutandplay.games.seabattle.repositories.GameStateRepository
import javax.inject.Inject
import play.api.Configuration
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }

import scala.concurrent.Future

class SeabattleDAO @Inject()(
    private val configuration: Configuration,
    override protected val dbConfigProvider: DatabaseConfigProvider
) extends GameStateRepository[Future]
    with HasDatabaseConfigProvider[CustomPostgresDriver] {
  import profile.api._

  /**
    * Helfermethode zum Selektieren eines Tabelleneintrags.
    *
    * @param gameId Die eineindeutige ID des Spiels.
    * @return Eine Slickabfrage (Query), die in anderen Abfragen verwendet werden kann.
    */
  private def selectGameState(gameId: UUID) =
    SeabattleTables.gameStateTable.filter(_.gameId === gameId)

  /**
    * Helfermethode zum Selektieren von Spielen, an denen ein Spieler
    * beteiligt ist.
    *
    * @param playerId Die ID des Spielers.
    * @return Eine Slickabfrage (Query), die in anderen Anfragen verwendet werden kann.
    */
  private def selectParticipating(playerId: UUID) =
    SeabattleTables.gameStateTable.filter(
      r => r.owner === playerId || r.opponent === Option(playerId)
    )

  override def add(gs: GameState): Future[Int] =
    dbConfig.db.run(SeabattleTables.gameStateTable += gs)

  override def get(gameId: UUID): Future[Option[GameState]] =
    dbConfig.db.run(selectGameState(gameId).result.headOption)

  override def getOpen(playerId: Option[UUID]): Future[Seq[GameState]] = {
    val q = SeabattleTables.gameStateTable.filter(_.opponent.isEmpty)

    dbConfig.db.run(
      playerId.fold(q.result)(id => q.filter(_.owner =!= id).result)
    )
  }

  override def getOwn(ownerId: UUID): Future[Seq[GameState]] =
    dbConfig.db.run(SeabattleTables.gameStateTable.filter(_.owner === ownerId).result)

  override def getParticipating(finished: Boolean)(playerId: UUID): Future[Seq[GameState]] = {
    val q = selectParticipating(playerId)
    val f =
      if (finished)
        q.filter(r => r.state.+>>("running") === "false" && r.state.+>>("winner") =!= "null")
      else
        q.filter(r => r.opponent.nonEmpty && r.state.+>>("running") === "true")
    dbConfig.db.run(f.result)
  }

  override def remove(gameId: UUID): Future[Int] =
    dbConfig.db.run(selectGameState(gameId).delete)

  override def update(gs: GameState): Future[Int] =
    dbConfig.db.run(selectGameState(gs.gameId).update(gs))
}
