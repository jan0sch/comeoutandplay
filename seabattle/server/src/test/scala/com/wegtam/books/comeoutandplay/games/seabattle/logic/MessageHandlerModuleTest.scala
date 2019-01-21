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

import com.wegtam.books.comeoutandplay.games.seabattle.BoardOps.syntax._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.BoardGenerators._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameStateOps.syntax._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ MustMatchers, WordSpec }

import scala.collection.immutable.Seq

class MessageHandlerModuleTest extends WordSpec with MustMatchers with PropertyChecks {

  "Registering a player" when {
    "GameState opponent is empty" must {
      "return a PlayerRegistered and the correct GameState" in {
        val gameId            = UUID.randomUUID()
        val owner             = UUID.randomUUID()
        val player            = UUID.randomUUID()
        val gs                = GameState.createEmpty(gameId)(owner)
        val msg               = Message.RegisterPlayer(gameId, player)
        val (newGs, response) = MessageHandlerModule.handle(msg).run(gs).value
        response must be(Message.PlayerRegistered(gameId, player))
        newGs.opponent must contain(player)
        newGs.running must be(true)
      }
    }

    "GameState opponent is not empty" must {
      "return a GameError and the unmodified GameState" in {
        val gameId = UUID.randomUUID()
        val owner  = UUID.randomUUID()
        val player = UUID.randomUUID()
        val gs = GameState
          .createEmpty(gameId)(owner)
          .copy(opponent = Option(UUID.randomUUID()), running = true)
        val msg               = Message.RegisterPlayer(gameId, player)
        val (newGs, response) = MessageHandlerModule.handle(msg).run(gs).value
        response must be(Message.GameError("The game is already full!"))
        newGs must be(gs)
      }
    }
  }

  "Saving the board" when {
    "the player is neither owner nor opponent" must {
      "return an error" in {
        val gameId        = UUID.randomUUID()
        val owner         = UUID.randomUUID()
        val player        = UUID.randomUUID()
        val gs            = GameState.createEmpty(gameId)(owner).copy(opponent = Option(player))
        val unknownPlayer = UUID.randomUUID()
        val board         = Board.createDefaultBoard
        val (newGs, response) =
          MessageHandlerModule.handle(Message.SaveBoard(gameId, unknownPlayer, board)).run(gs).value
        response must be(Message.GameError("Unknown player id!"))
        newGs must be(gs)
      }
    }

    "the player is the owner" must {
      "save the board" in {
        val gameId = UUID.randomUUID()
        val owner  = UUID.randomUUID()
        val player = UUID.randomUUID()
        val gs     = GameState.createEmpty(gameId)(owner).copy(opponent = Option(player))
        val board = Board.createDefaultBoard.copy(
          ships = Seq(Ship.createShip(ShipClass.Battleship, Orientation.Horizontal, Position(0, 0)))
        )
        val (newGs, response) =
          MessageHandlerModule.handle(Message.SaveBoard(gameId, owner, board)).run(gs).value
        response must be(Message.BoardSaved(gameId, owner, board))
        newGs.boards.get(owner) must contain(board)
      }
    }

    "the player is the opponent" must {
      "save the board" in {
        val gameId = UUID.randomUUID()
        val owner  = UUID.randomUUID()
        val player = UUID.randomUUID()
        val gs     = GameState.createEmpty(gameId)(owner).copy(opponent = Option(player))
        val board = Board.createDefaultBoard.copy(
          ships = Seq(Ship.createShip(ShipClass.Cruiser, Orientation.Vertical, Position(1, 1)))
        )
        val (newGs, response) =
          MessageHandlerModule.handle(Message.SaveBoard(gameId, player, board)).run(gs).value
        response must be(Message.BoardSaved(gameId, player, board))
        newGs.boards.get(player) must contain(board)
      }
    }
  }

  "Reporting Ready" when {
    "already reported" when {
      "other player is also ready" when {
        "game is already running" when {
          "other player made the last move" must {
            "return MakeMove" in {
              val gameId  = UUID.randomUUID()
              val owner   = UUID.randomUUID()
              val player  = UUID.randomUUID()
              val ownerB  = Board.createDefaultBoard
              val playerB = Board.createDefaultBoard
              val gs =
                GameState
                  .createEmpty(gameId)(owner)
                  .copy(
                    opponent = Option(player),
                    ready = Seq(player, owner),
                    boards = Map(owner       -> ownerB, player -> playerB),
                    enemyBoards = Map(owner  -> EnemyBoard.initialiseFromBoard(ownerB),
                                      player -> EnemyBoard.initialiseFromBoard(playerB)),
                    moves = Seq(PlayerMove(playerId = owner, position = Position(0, 0)))
                  )
              val (newGs, response) =
                MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
              response must be(Message.MakeMove(gameId, playerB, newGs.getEnemyBoard(owner).get))
            }
          }

          "player made the last move" must {
            "return WaitForOtherPlayer" in {
              val gameId  = UUID.randomUUID()
              val owner   = UUID.randomUUID()
              val player  = UUID.randomUUID()
              val ownerB  = Board.createDefaultBoard
              val playerB = Board.createDefaultBoard
              val gs =
                GameState
                  .createEmpty(gameId)(owner)
                  .copy(
                    opponent = Option(player),
                    ready = Seq(player, owner),
                    boards = Map(owner       -> ownerB, player -> playerB),
                    enemyBoards = Map(owner  -> EnemyBoard.initialiseFromBoard(ownerB),
                                      player -> EnemyBoard.initialiseFromBoard(playerB)),
                    moves = Seq(PlayerMove(playerId = player, position = Position(0, 0)))
                  )
              val (newGs, response) =
                MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
              response must be(Message.WaitForOtherPlayer(gameId))
              newGs must be(gs)
            }
          }
        }

        "player was the first ready" must {
          "return MakeMove" in {
            val gameId  = UUID.randomUUID()
            val owner   = UUID.randomUUID()
            val player  = UUID.randomUUID()
            val ownerB  = Board.createDefaultBoard
            val playerB = Board.createDefaultBoard
            val gs =
              GameState
                .createEmpty(gameId)(owner)
                .copy(
                  opponent = Option(player),
                  ready = Seq(player, owner),
                  boards = Map(owner       -> ownerB, player -> playerB),
                  enemyBoards = Map(owner  -> EnemyBoard.initialiseFromBoard(ownerB),
                                    player -> EnemyBoard.initialiseFromBoard(playerB))
                )
            val (newGs, response) =
              MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
            response must be(Message.MakeMove(gameId, playerB, newGs.getEnemyBoard(owner).get))
          }
        }

        "player was not the first ready" must {
          "return WaitForOtherPlayer" in {
            val gameId  = UUID.randomUUID()
            val owner   = UUID.randomUUID()
            val player  = UUID.randomUUID()
            val ownerB  = Board.createDefaultBoard
            val playerB = Board.createDefaultBoard
            val gs =
              GameState
                .createEmpty(gameId)(owner)
                .copy(
                  opponent = Option(player),
                  ready = Seq(owner, player),
                  boards = Map(owner       -> ownerB, player -> playerB),
                  enemyBoards = Map(owner  -> EnemyBoard.initialiseFromBoard(ownerB),
                                    player -> EnemyBoard.initialiseFromBoard(playerB)),
                  moves = Seq(PlayerMove(playerId = player, position = Position(0, 0)))
                )
            val (newGs, response) =
              MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
            response must be(Message.WaitForOtherPlayer(gameId))
            newGs must be(gs)
          }
        }
      }

      "other player is not yet ready" must {
        "return WaitForOtherPlayer" in {
          val gameId = UUID.randomUUID()
          val owner  = UUID.randomUUID()
          val player = UUID.randomUUID()
          val gs =
            GameState
              .createEmpty(gameId)(owner)
              .copy(opponent = Option(player), ready = Seq(player))
          val (newGs, response) =
            MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
          response must be(Message.WaitForOtherPlayer(gameId))
          newGs.ready must contain(player)
        }
      }
    }

    "not already reported" when {
      "board is not ready" must {
        "return GameError" in {
          val gameId = UUID.randomUUID()
          val owner  = UUID.randomUUID()
          val player = UUID.randomUUID()
          val b = Board.createDefaultBoard.copy(
            ships = Seq(Ship.createShip(ShipClass.Battleship, Orientation.Vertical, Position(0, 0)))
          )
          val gs =
            GameState
              .createEmpty(gameId)(owner)
              .copy(opponent = Option(player), boards = Map(player -> b))
          val (newGs, response) =
            MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
          newGs.ready must not contain player
          response must be(Message.GameError("Board not ready!", None))
        }
      }

      "board is ready" must {
        "add the player and return WaitForOtherPlayer" in {
          val gameId = UUID.randomUUID()
          val owner  = UUID.randomUUID()
          val player = UUID.randomUUID()
          val createShip: ShipClass => Ship =
            (c: ShipClass) => Ship(c, Seq.empty, Orientation.Horizontal, Position(0, 0))
          val b = Board.createDefaultBoard.copy(
            ships = ShipClass.all
              .foldLeft(Seq.empty[Ship])((acc, c) => acc ++ Seq.fill(c.maxShips)(createShip(c)))
          )
          val gs =
            GameState
              .createEmpty(gameId)(owner)
              .copy(opponent = Option(player), boards = Map(player -> b))
          val (newGs, response) =
            MessageHandlerModule.handle(Message.Ready(gameId, player)).run(gs).value
          newGs.ready must contain(player)
          response must be(Message.WaitForOtherPlayer(gameId))
        }
      }
    }
  }

  "Move" when {
    "there is a winner" must {
      "return GameOver" in {
        val gameId = UUID.randomUUID()
        val owner  = UUID.randomUUID()
        val player = UUID.randomUUID()
        val createShip: ShipClass => Ship =
          (c: ShipClass) => Ship(c, Seq.empty, Orientation.Horizontal, Position(0, 0))
        val b = Board.createDefaultBoard.copy(
          ships = ShipClass.all
            .foldLeft(Seq.empty[Ship])((acc, c) => acc ++ Seq.fill(c.maxShips)(createShip(c)))
        )
        val gs =
          GameState
            .createEmpty(gameId)(owner)
            .copy(opponent = Option(player),
                  boards = Map(owner -> Board.createDefaultBoard, player -> b),
                  winner = Option(owner))
        val (newGs, response) =
          MessageHandlerModule.handle(Message.Move(gameId, player, Position(0, 0))).run(gs).value
        newGs must be(gs)
        response must be(
          Message.GameOver(gs.gameId,
                           b,
                           EnemyBoard.initialiseFromBoard(Board.createDefaultBoard),
                           Option(owner))
        )
      }
    }

    "there is no winner" when {
      "last move was done by current player" must {
        "return WaitForOtherPlayer" in {
          val gameId = UUID.randomUUID()
          val owner  = UUID.randomUUID()
          val player = UUID.randomUUID()
          val createShip: ShipClass => Ship =
            (c: ShipClass) => Ship(c, Seq.empty, Orientation.Horizontal, Position(0, 0))
          val b = Board.createDefaultBoard.copy(
            ships = ShipClass.all
              .foldLeft(Seq.empty[Ship])((acc, c) => acc ++ Seq.fill(c.maxShips)(createShip(c)))
          )
          val gs =
            GameState
              .createEmpty(gameId)(owner)
              .copy(opponent = Option(player),
                    boards = Map(player -> b),
                    moves = Seq(PlayerMove(player, Position(1, 1))),
                    running = true)
          val (newGs, response) =
            MessageHandlerModule.handle(Message.Move(gameId, player, Position(0, 0))).run(gs).value
          newGs must be(gs)
          response must be(Message.WaitForOtherPlayer(gameId))
        }
      }

      "last move was done by other player" when {
        "move does finish the game" must {
          "apply move and return GameOver" in {
            forAll("BoardA", "BoardB") { (a: Board, b: Board) =>
              val gameId = UUID.randomUUID()
              val owner  = UUID.randomUUID()
              val player = UUID.randomUUID()

              // Modify the boards to be "finished".
              val boardA =
                a.ships.foldLeft(a) { (ba, s) =>
                  val p = s.pos
                  val shots = s.orientation match {
                    case Orientation.Horizontal =>
                      for (n <- 0 to s.c.length) yield p.copy(column = p.column + n)
                    case Orientation.Vertical =>
                      for (n <- 0 to s.c.length) yield p.copy(row = p.row + n)
                  }
                  ba.applyMoves(shots)
                }
              boardA.getState match {
                case BoardState.Finished => succeed
                case _                   => fail(s"Board A should be finished: ${boardA.ships}")
              }

              val boardB =
                b.ships.foldLeft(b) { (bb, s) =>
                  val p = s.pos
                  val shots = s.orientation match {
                    case Orientation.Horizontal =>
                      for (n <- 0 to s.c.length) yield p.copy(column = p.column + n)
                    case Orientation.Vertical =>
                      for (n <- 0 to s.c.length) yield p.copy(row = p.row + n)
                  }
                  bb.applyMoves(shots)
                }
              boardB.getState match {
                case BoardState.Finished => succeed
                case _                   => fail(s"Board B should be finished: ${boardB.ships}")
              }

              val gs = GameState
                .createEmpty(gameId)(owner)
                .copy(
                  opponent = Option(player),
                  boards = Map(player      -> boardA, owner -> boardB),
                  enemyBoards = Map(player -> EnemyBoard.initialiseFromBoard(boardA),
                                    owner  -> EnemyBoard.initialiseFromBoard(boardB)),
                  running = true
                )
              val mv = Message.Move(gameId, player, Position(0, 0))

              val eb = boardB.applyMove(mv.position)
              eb.getState match {
                case BoardState.Finished => succeed
                case _                   => fail(s"Board EB should be finished: ${eb.ships}")
              }

              val es = gs.copy(
                boards = gs.boards + (owner -> eb),
                enemyBoards = gs.enemyBoards + (owner -> EnemyBoard
                  .initialiseFromBoard(eb)
                  .updateFromState(eb)(mv.position)),
                moves = gs.moves :+ PlayerMove(mv.playerId, mv.position),
                running = false,
                winner = Option(player)
              )

              val (newGs, response) = MessageHandlerModule.handle(mv).run(gs).value
              response match {
                case Message.GameOver(id, board, enemyBoard, winner) =>
                  id must be(gameId)
                  board must be(boardA)
                  enemyBoard must be(
                    EnemyBoard.initialiseFromBoard(eb).updateFromState(eb)(mv.position)
                  )
                  winner must contain(player)
                case msg => fail(s"Invalid return type: $msg")
              }
              newGs must be(es)
            }
          }
        }

        "move does not finish the game" must {
          "apply move and return MoveResult" in {
            forAll("BoardA", "BoardB") { (a: Board, b: Board) =>
              val gameId = UUID.randomUUID()
              val owner  = UUID.randomUUID()
              val player = UUID.randomUUID()
              val gs = GameState
                .createEmpty(gameId)(owner)
                .copy(opponent = Option(player),
                      boards = Map(player -> a, owner -> b),
                      running = true)
              val mv = Message.Move(gameId, player, Position(0, 0))

              val eb = b.applyMove(mv.position)
              val es = gs.copy(
                boards = gs.boards + (owner -> eb),
                enemyBoards =
                  Map(owner -> EnemyBoard.initialiseFromBoard(eb).updateFromState(eb)(mv.position)),
                moves = gs.moves :+ PlayerMove(mv.playerId, mv.position)
              )

              val (newGs, response) = MessageHandlerModule.handle(mv).run(gs).value
              response match {
                case Message.MoveResult(id, board, enemyBoard, continue, winner) =>
                  id must be(gameId)
                  board must be(a)
                  enemyBoard must be(
                    EnemyBoard.initialiseFromBoard(eb).updateFromState(eb)(mv.position)
                  )
                  continue must be(false)
                  winner must be(empty)
                case msg => fail(s"Invalid return type: $msg")
              }
              newGs must be(es)
            }
          }
        }
      }
    }
  }
}
