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

import CustomPostgresDriver.api._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameState
import io.circe.Json
import io.circe.syntax._

object SeabattleTables {
  type GameStateTableRow = (UUID, UUID, Option[UUID], Json)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def databaseRowToGameState(r: GameStateTableRow): GameState = {
    val (_, _, _, json) = r
    json.as[GameState] match {
      case Left(_)  => throw new IllegalArgumentException("Unable to deserialise GameState!")
      case Right(s) => s
    }
  }

  def gameStateToDatabaseRow(s: GameState): Option[GameStateTableRow] =
    Option((s.gameId, s.owner, s.opponent, s.asJson))

  class GameStateTable(tag: Tag) extends Table[GameState](tag, "seabattles") {
    def gameId   = column[UUID]("game_id", O.PrimaryKey)
    def owner    = column[UUID]("owner")
    def opponent = column[Option[UUID]]("opponent")
    def state    = column[Json]("state")

    override def * =
      (gameId, owner, opponent, state) <> (databaseRowToGameState, gameStateToDatabaseRow)
  }

  final val gameStateTable: TableQuery[GameStateTable] = TableQuery[GameStateTable]

}
