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

package com.wegtam.books.comeoutandplay.games.seabattle.logic

import java.util.UUID

import cats._
import cats.data._
import cats.implicits._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameStateOps.syntax._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.Message._
import com.wegtam.books.comeoutandplay.games.seabattle.BoardOps.syntax._

object MessageHandlerModule extends MessageHandler[Id] {

  override def handle(m: Message): LogicState[Message] =
    State[GameState, Message] { gs =>
      m match {
        case InitGame(ownerId) =>
          val gameId = UUID.randomUUID()
          (GameState.createEmpty(gameId)(ownerId), GameInitialised(gameId, ownerId))
        case RegisterPlayer(gs.gameId, playerId) =>
          (gs.copy(opponent = Option(playerId), running = true),
           PlayerRegistered(gs.gameId, playerId))
        case CreateBoard(gs.gameId, playerId) =>
          if (gs.owner === playerId || gs.opponent.contains(playerId)) {
            val b = Board.createDefaultBoard
            (gs.copy(boards = gs.boards + (playerId -> b)), BoardCreated(gs.gameId, playerId, b))
          } else
            (gs, GameError("Unknown player id!"))
        case SaveBoard(gs.gameId, playerId, board) =>
          if (gs.owner === playerId || gs.opponent.contains(playerId)) {
            (gs.copy(boards = gs.boards + (playerId -> board)),
             BoardSaved(gs.gameId, playerId, board))
          } else
            (gs, GameError("Unknown player id!"))
        case Ready(gs.gameId, playerId) =>
          if (gs.ready.contains(playerId)) {
            if (gs.ready.size > 1) {
              // Alle Spieler sind bereit, der erste darf zuerst ziehen.
              // Anderenfalls der Spieler, der nicht zuletzt gezogen hat.
              if ((gs.moves.isEmpty && gs.ready.headOption.contains(playerId)) || gs.moves.lastOption
                    .forall(_.playerId =!= playerId)) {
                val ms = for {
                  id <- Option(gs.gameId)
                  bd <- gs.getBoard(playerId)
                  op <- gs.getOtherPlayerId(playerId)
                  eb <- gs.getEnemyBoard(op)
                } yield MakeMove(id, bd, eb)
                ms.fold((gs, GameError("Could not retrieve data for MakeMove command!"): Message))(
                  m => (gs, m: Message)
                )
              } else
                (gs, WaitForOtherPlayer(gs.gameId))
            } else
              (gs, WaitForOtherPlayer(gs.gameId))
          } else {
            val ms = for {
              bd <- gs.getBoard(playerId)
              st = bd.getState
            } yield
              st match {
                case BoardState.Ready =>
                  (gs.copy(ready = gs.ready :+ playerId), WaitForOtherPlayer(gs.gameId))
                case _ => (gs, GameError("Board not ready!"))
              }
            ms.fold(
              (gs, GameError("Could not retrieve data for WaitForOtherPlayer command!"): Message)
            )(identity)
          }
        case Message.Move(gs.gameId, playerId, position) =>
          gs.getOtherPlayerId(playerId).fold((gs, GameError("No other player!"): Message)) {
            otherPlayer =>
              if (gs.winner.nonEmpty)
                gs.getEnemyBoard(otherPlayer)
                  .fold((gs, GameError("Could not retrieve data for GameOver command!"): Message))(
                    eb => (gs, GameOver(gs.gameId, gs.boards(playerId), eb, gs.winner))
                  )
              else if (gs.moves.lastOption
                         .exists(_.playerId === playerId) || (gs.moves.isEmpty && gs.ready.headOption
                         .contains(otherPlayer)))
                (gs, WaitForOtherPlayer(gs.gameId))
              else {
                val tng = for {
                  bd <- gs.getBoard(playerId)
                  eb <- gs.getEnemyBoard(otherPlayer)
                  ng = {
                    val ms = gs.moves :+ PlayerMove(playerId, position)
                    val bs = GameState.applyMoves(gs.boards)(ms)
                    val ne = eb.updateFromState(bs(otherPlayer))(position)
                    bs(otherPlayer).getState match {
                      case BoardState.Finished =>
                        (gs.copy(boards = bs,
                                 enemyBoards = gs.enemyBoards + (otherPlayer -> ne),
                                 moves = ms,
                                 running = false,
                                 winner = Option(playerId)),
                         GameOver(
                           gameId = gs.gameId,
                           board = bd,
                           enemyBoard = ne,
                           winner = Option(playerId)
                         ))
                      case _ =>
                        (gs.copy(boards = bs,
                                 enemyBoards = gs.enemyBoards + (otherPlayer -> ne),
                                 moves = ms),
                         MoveResult(
                           gameId = gs.gameId,
                           board = bd,
                           enemyBoard = ne,
                           continue = false,
                           winner = None
                         ))
                    }
                  }
                } yield ng
                tng.getOrElse(
                  (gs, GameError("Could not retrieve data for MoveResult command!"): Message)
                )
              }
          }
        case _ =>
          (gs, GameError("Wrong game id!"))
      }
    }
}
